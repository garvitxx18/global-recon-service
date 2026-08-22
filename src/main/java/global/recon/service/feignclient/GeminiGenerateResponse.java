package global.recon.service.feignclient;

import java.util.ArrayList;
import java.util.List;

public class GeminiGenerateResponse {

    private List<Candidate> candidates = new ArrayList<>();

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public String firstText() {
        if (candidates == null || candidates.isEmpty() || candidates.getFirst().getContent() == null) {
            return null;
        }
        List<GeminiGenerateRequest.Part> parts = candidates.getFirst().getContent().getParts();
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        return parts.getFirst().getText();
    }

    public static class Candidate {
        private GeminiGenerateRequest.Content content;

        public GeminiGenerateRequest.Content getContent() {
            return content;
        }

        public void setContent(GeminiGenerateRequest.Content content) {
            this.content = content;
        }
    }
}
