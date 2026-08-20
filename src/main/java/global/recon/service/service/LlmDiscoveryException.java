package global.recon.service.service;

public class LlmDiscoveryException extends RuntimeException {

    public LlmDiscoveryException(String message) {
        super(message);
    }

    public LlmDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
