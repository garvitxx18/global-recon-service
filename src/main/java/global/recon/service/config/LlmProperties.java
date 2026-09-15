package global.recon.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String provider = "gemini";
    private boolean mockEnabled = false;
    private Api api = new Api();
    private Gemini gemini = new Gemini();
    private Agent agent = new Agent();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public boolean hasGeminiKey() {
        return gemini != null && gemini.getApiKey() != null && !gemini.getApiKey().isBlank();
    }

    public boolean hasAgentUrl() {
        return agent != null && agent.getBaseUrl() != null && !agent.getBaseUrl().isBlank();
    }

    public String resolvedProvider() {
        if (mockEnabled) {
            return "mock";
        }
        String value = provider == null ? "mock" : provider.trim().toLowerCase();
        if ("gemini".equals(value) && !hasGeminiKey()) {
            return "mock";
        }
        if ("agent".equals(value) && !hasAgentUrl()) {
            return "mock";
        }
        return value;
    }

    public static class Api {
        private String url = "http://localhost:9080";
        private String path = "/v1/complete";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Gemini {
        private String baseUrl = "https://generativelanguage.googleapis.com";
        private String apiKey = "";
        private String model = "gemini-3.6-flash";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Agent {
        private String baseUrl = "";
        private String appName = "global_recon";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }
    }
}
