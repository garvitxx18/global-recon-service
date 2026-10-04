package global.recon.service.feignclient;

import global.recon.service.config.ReconProperties;
import global.recon.service.model.Source;
import global.recon.service.model.SourceAuthType;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.SourceFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConfiguredSourceClient {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredSourceClient.class);
    private static final Pattern PATH_TOKEN = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    private final RestClient restClient;
    private final ReconProperties reconProperties;
    private final ConcurrentHashMap<String, Semaphore> gates = new ConcurrentHashMap<>();

    public ConfiguredSourceClient(RestClient configuredSourceRestClient, ReconProperties reconProperties) {
        this.restClient = configuredSourceRestClient;
        this.reconProperties = reconProperties;
    }

    public byte[] fetch(Source source, Map<String, String> params, String secret) {
        Map<String, String> safeParams = params == null ? Map.of() : params;
        URI uri = buildUri(source, safeParams);
        assertAllowed(uri);
        HttpMethod method = resolveMethod(source.getHttpMethod());
        Semaphore gate = gateFor(source);
        try {
            gate.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SourceFetchException("Interrupted while waiting for source capacity: " + source.getId());
        }
        log.info("Configured source fetch id={} method={} host={} path={} paramCount={}",
                source.getId(), method, uri.getHost(), uri.getRawPath(), safeParams.size());
        try {
            return restClient.method(method)
                    .uri(uri)
                    .headers(headers -> applyAuth(headers, source, secret))
                    .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new SourceFetchException(
                                    "Configured source " + source.getId() + " returned HTTP "
                                            + response.getStatusCode().value());
                        }
                        return readCapped(response.getBody(), reconProperties.getSources().getMaxBodyBytes());
                    });
        } catch (SourceFetchException | InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceFetchException("Configured source " + source.getId() + " failed: " + ex.getMessage(), ex);
        } finally {
            gate.release();
        }
    }

    private void applyAuth(org.springframework.http.HttpHeaders headers, Source source, String secret) {
        SourceAuthType authType = source.getAuthType() == null ? SourceAuthType.NONE : source.getAuthType();
        if (authType == SourceAuthType.NONE || secret == null || secret.isBlank()) {
            return;
        }
        if (authType == SourceAuthType.BEARER) {
            headers.setBearerAuth(secret.startsWith("Bearer ") ? secret.substring(7).trim() : secret);
            return;
        }
        String headerName = source.getAuthHeader();
        if (headerName == null || headerName.isBlank()) {
            throw new InvalidRequestException("Source " + source.getId() + " HEADER auth requires authHeader");
        }
        headers.set(headerName.trim(), secret);
    }

    private URI buildUri(Source source, Map<String, String> params) {
        String base = trimSlash(source.getBaseUrl());
        String path = source.getPath() == null ? "" : source.getPath();
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        Map<String, String> remaining = new LinkedHashMap<>(params);
        Matcher matcher = PATH_TOKEN.matcher(path);
        StringBuffer filled = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = remaining.remove(name);
            if (value == null || value.isBlank()) {
                throw new InvalidRequestException("Path parameter '" + name + "' is required for source " + source.getId());
            }
            matcher.appendReplacement(filled, Matcher.quoteReplacement(encodePath(value)));
        }
        matcher.appendTail(filled);
        if (PATH_TOKEN.matcher(filled).find()) {
            throw new InvalidRequestException("Unresolved path template for source " + source.getId());
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base + filled);
        remaining.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                builder.queryParam(key, value);
            }
        });
        return builder.build(true).toUri();
    }

    private void assertAllowed(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        ReconProperties.Sources cfg = reconProperties.getSources();
        if ("https".equals(scheme)) {
            // allowed
        } else if ("http".equals(scheme) && cfg.isAllowHttp()) {
            // lab / internal only
        } else {
            throw new InvalidRequestException("Source URL scheme is not allowed: " + scheme);
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            throw new InvalidRequestException("Source URL must not include user info");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidRequestException("Source URL host is missing");
        }
        if (isBlockedHost(host, cfg.isAllowHttp())) {
            throw new InvalidRequestException("Source host is not allowed");
        }
        List<String> suffixes = cfg.getAllowedHostSuffixes();
        if (suffixes == null || suffixes.isEmpty()) {
            return;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        boolean match = suffixes.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .anyMatch(suffix -> normalized.equals(suffix) || normalized.endsWith("." + suffix));
        if (!match) {
            throw new InvalidRequestException("Source host is not on the allowlist: " + host);
        }
    }

    private boolean isBlockedHost(String host, boolean allowHttp) {
        String h = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(h) || h.endsWith(".localhost")) {
            return !allowHttp;
        }
        if (!looksLikeIp(h)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(h);
            if (address.isMulticastAddress()) {
                return true;
            }
            if (allowHttp) {
                return false;
            }
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress();
        } catch (Exception ex) {
            return true;
        }
    }

    private boolean looksLikeIp(String host) {
        return host.chars().allMatch(ch -> (ch >= '0' && ch <= '9') || ch == '.' || ch == ':')
                || host.startsWith("[");
    }

    private HttpMethod resolveMethod(String httpMethod) {
        String method = httpMethod == null ? "GET" : httpMethod.trim().toUpperCase(Locale.ROOT);
        if ("GET".equals(method) || "POST".equals(method)) {
            return HttpMethod.valueOf(method);
        }
        throw new InvalidRequestException("Unsupported HTTP method for configured source: " + method);
    }

    private Semaphore gateFor(Source source) {
        int permits = source.getMaxConcurrent() > 0
                ? source.getMaxConcurrent()
                : reconProperties.getSources().getDefaultMaxConcurrent();
        return gates.computeIfAbsent(source.getId(), id -> new Semaphore(Math.max(1, permits)));
    }

    private byte[] readCapped(InputStream body, long maxBytes) {
        if (body == null) {
            return new byte[0];
        }
        try (InputStream in = body) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes) {
                    throw new InvalidRequestException("Configured source response exceeded " + maxBytes + " bytes");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceFetchException("Failed to read configured source body: " + ex.getMessage(), ex);
        }
    }

    private String encodePath(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new InvalidRequestException("Source base URL is missing");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
