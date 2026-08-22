package global.recon.service.feignclient;

import java.util.ArrayList;
import java.util.List;

public class GeminiGenerateRequest {

    private List<Content> contents = new ArrayList<>();
    private GenerationConfig generationConfig = new GenerationConfig();

    public static GeminiGenerateRequest fromPrompt(String prompt) {
        GeminiGenerateRequest request = new GeminiGenerateRequest();
        Part part = new Part();
        part.setText(prompt);
        Content content = new Content();
        content.setRole("user");
        content.getParts().add(part);
        request.getContents().add(content);
        return request;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public static class Content {
        private String role;
        private List<Part> parts = new ArrayList<>();

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class GenerationConfig {
        private double temperature = 0.1;
        private String responseMimeType = "application/json";

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getResponseMimeType() {
            return responseMimeType;
        }

        public void setResponseMimeType(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }
    }
}
