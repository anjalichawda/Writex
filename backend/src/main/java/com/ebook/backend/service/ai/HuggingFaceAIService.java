package com.ebook.backend.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HuggingFaceAIService implements AIService {
    private static final String HF_IMAGE_ROUTER_BASE_URL = "https://router.huggingface.co/hf-inference/models";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${ai.llm.hf.model}")
    private String model;

    @Value("${ai.llm.hf.baseUrl}")
    private String hfBaseUrl;

    @Value("${ai.llm.hf.token:}")
    private String hfToken;

    @Value("${ai.image.hf.baseUrl:https://router.huggingface.co/hf-inference/models}")
    private String hfImageBaseUrl;

    @Value("${ai.image.hf.model:stabilityai/stable-diffusion-xl-base-1.0}")
    private String imageModel;

    @Override
    public List<Map<String, String>> generateOutline(String topic, String description, String style, int numChapters) {
        String safeTopic = topic == null ? "" : topic.trim();
        String safeDescription = description == null ? "" : description.trim();
        String safeStyle = style == null ? "Informative" : style.trim();
        int safeNumChapters = Math.max(1, Math.min(20, numChapters));

        String prompt = buildOutlinePrompt(safeTopic, safeDescription, safeStyle, safeNumChapters);
        String llmJson = generateJsonFromPrompt(prompt, 600);

        try {
            JsonNode root = objectMapper.readTree(llmJson);
            JsonNode outlineNode = root.get("outline");
            if (outlineNode == null || !outlineNode.isArray()) {
                throw new IllegalStateException("LLM output did not contain an `outline` array.");
            }

            List<Map<String, String>> outline = new ArrayList<>();
            for (JsonNode ch : outlineNode) {
                String title = ch.hasNonNull("title") ? ch.get("title").asText() : "";
                String desc = ch.hasNonNull("description") ? ch.get("description").asText() : "";
                outline.add(Map.of("title", title, "description", desc));
            }
            return outline;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse LLM outline JSON.", e);
        }
    }

    @Override
    public String generateChapterContent(String chapterTitle, String chapterDescription, String style) {
        String safeChapterTitle = chapterTitle == null ? "" : chapterTitle.trim();
        String safeChapterDescription = chapterDescription == null ? "" : chapterDescription.trim();
        String safeStyle = style == null ? "Informative" : style.trim();

        String prompt = buildChapterPrompt(safeChapterTitle, safeChapterDescription, safeStyle);

        // Get raw LLM text — bypass generateJsonFromPrompt since chapter content
        // is long prose that breaks JSON extraction heuristics
        String rawText = generateRawTextFromPrompt(prompt, 1200);

        // 1. Try to parse as JSON first (happy path — LLM obeyed the schema)
        try {
            String candidate = extractFirstJsonObject(rawText);
            JsonNode root = objectMapper.readTree(candidate);
            JsonNode contentNode = root.get("content");
            if (contentNode != null && contentNode.isTextual()) {
                String extracted = contentNode.asText().trim();
                if (!extracted.isBlank()) {
                    return extracted;
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Fallback: strip any JSON wrapper fragments and return the prose directly
        String cleaned = rawText.trim();
        // Strip leading {"content": " and trailing "}
        cleaned = cleaned.replaceAll("(?s)^\\{\\s*\"content\"\\s*:\\s*\"", "");
        cleaned = cleaned.replaceAll("\"\\s*\\}\\s*$", "");
        // Unescape JSON string escapes left behind
        cleaned = cleaned.replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();

        if (!cleaned.isBlank()) {
            return cleaned;
        }

        throw new IllegalStateException("Failed to extract chapter content from LLM response.");
    }

    private String generateRawTextFromPrompt(String prompt, int maxNewTokens) {
        String effectiveToken = resolveHfToken();
        boolean hasToken = effectiveToken != null && !effectiveToken.isBlank();

        String jsonBody = """
                {
                  "model": %s,
                  "messages": [
                    { "role": "user", "content": %s }
                  ],
                  "max_tokens": %d,
                  "temperature": 0.7
                }
                """.formatted(quoteJsonString(model), quoteJsonString(prompt), maxNewTokens);

        String base = hfBaseUrl.endsWith("/")
                ? hfBaseUrl.substring(0, hfBaseUrl.length() - 1)
                : hfBaseUrl;
        String url = base + "/chat/completions";

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        if (hasToken) {
            req.header("Authorization", "Bearer " + effectiveToken);
        }

        try {
            HttpResponse<String> resp = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("LLM request failed " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            // Returns raw content string — no JSON extraction forced
            return extractChatContent(root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request was interrupted.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Hugging Face LLM.", e);
        }
    }

    @Override
    public String suggestGrammar(String text) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) {
            return "";
        }

        String prompt = """
                You are a careful grammar and spelling corrector for English text.
                
                Using ONLY the input text below:
                - Correct grammar, spelling, and punctuation.
                - Do NOT rewrite for style; preserve the original meaning.
                - Preserve proper nouns and names.
                - Preserve line breaks as much as possible.
                - If there are no mistakes, return the original text unchanged.
                
                Return EXACTLY valid JSON (no markdown, no commentary) with this schema:
                { "suggestedText": "..." }
                """;

        String llmJson = generateJsonFromPrompt(
                prompt + "\nINPUT TEXT:\n" + safeText,
                600
        );

        try {
            JsonNode root = objectMapper.readTree(llmJson);
            JsonNode suggestedNode = root.get("suggestedText");
            if (suggestedNode == null || !suggestedNode.isTextual()) {
                throw new IllegalStateException("LLM output did not contain `suggestedText`.");
            }
            return suggestedNode.asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse grammar JSON.", e);
        }
    }

    @Override
    public String suggestNextWords(String chapterTitle, String currentContent, int maxWords) {
        String safeTitle = chapterTitle == null ? "" : chapterTitle.trim();
        String safeContent = currentContent == null ? "" : currentContent.trim();
        if (safeContent.isEmpty()) {
            return "";
        }

        int safeMaxWords = Math.max(3, Math.min(25, maxWords));
        String contextTail = safeContent.substring(Math.max(0, safeContent.length() - 1400));

        String prompt = """
                You are a writing autocomplete assistant.
                
                Continue the chapter naturally from the end of CURRENT_TEXT.
                
                Rules:
                1) Return EXACTLY valid JSON with this schema: { "completion": "..." }
                2) `completion` must contain only the NEXT words, not a full rewrite.
                3) Maximum %d words.
                4) Do not repeat CURRENT_TEXT verbatim.
                5) No markdown, no commentary.
                
                Chapter title: %s
                CURRENT_TEXT:
                %s
                """.formatted(
                safeMaxWords,
                escapeForPrompt(safeTitle),
                contextTail
        );

        String llmJson = generateJsonFromPrompt(prompt, 120);
        try {
            JsonNode root = objectMapper.readTree(llmJson);
            JsonNode completionNode = root.get("completion");
            if (completionNode == null || !completionNode.isTextual()) {
                throw new IllegalStateException("LLM output did not contain `completion`.");
            }
            return limitWords(completionNode.asText(), safeMaxWords);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse autocomplete JSON.", e);
        }
    }

    @Override
    public String generateChapterImageDataUrl(String chapterTitle, String chapterContent, String style) {
        List<String> images = generateChapterImageSuggestions(chapterTitle, chapterContent, "", style, 1);
        return images.isEmpty() ? "" : images.get(0);
    }

    @Override
    public List<String> generateChapterImageSuggestions(String chapterTitle, String chapterContent, String prompt, String style, int count) {
        String safeTitle = chapterTitle == null ? "" : chapterTitle.trim();
        String safeContent = chapterContent == null ? "" : chapterContent.trim();
        String safePrompt = prompt == null ? "" : prompt.trim();
        String safeStyle = style == null ? "" : style.trim();

        int safeCount = Math.max(1, Math.min(3, count));
        String excerpt = safeContent.isBlank()
                ? "No chapter content available. Create a scene from the chapter title."
                : safeContent.substring(0, Math.min(safeContent.length(), 900));
        String theme = safePrompt.isBlank()
                ? "Use the chapter title and excerpt to infer the scene."
                : safePrompt;

        List<String> styleVariations = List.of(
                "cinematic digital illustration, dramatic lighting, highly detailed",
                "storybook watercolor illustration, soft lighting, painterly details",
                "realistic concept art, rich composition, vibrant colors"
        );

        int seedBase = Math.floorMod((safeTitle + excerpt + theme).hashCode(), 100_000);
        List<String> images = new ArrayList<>();
        for (int i = 0; i < safeCount; i++) {
            String promptText = """
                    Create a high-quality book chapter illustration.
                    No text, no watermark, no logos.
                    
                    Primary theme: %s
                    Chapter title: %s
                    Chapter excerpt: %s
                    Preferred style: %s
                    Variation style: %s
                    """.formatted(
                    theme,
                    safeTitle.isBlank() ? "Untitled chapter" : safeTitle,
                    excerpt,
                    safeStyle.isBlank() ? "visually appealing storybook style" : safeStyle,
                    styleVariations.get(i % styleVariations.size())
            );
            images.add(generateImageDataUrlFromPrompt(promptText, seedBase + (i * 97)));
        }
        return images;
    }

    private String generateImageDataUrlFromPrompt(String prompt, int seed) {
        String effectiveToken = resolveHfToken();
        if (effectiveToken == null || effectiveToken.isBlank()) {
            throw new IllegalStateException("HF_API_TOKEN is required for Hugging Face image generation.");
        }
        String base = normalizeBaseUrl(hfImageBaseUrl);
        String imageModelUrl = base + "/" + imageModel;
        String requestBody = buildImageRequestBody(prompt, seed);

        try {
            HttpResponse<byte[]> response = sendImageRequest(imageModelUrl, requestBody, effectiveToken);
            if (response.statusCode() == 410 && base.contains("api-inference.huggingface.co")) {
                String fallbackUrl = HF_IMAGE_ROUTER_BASE_URL + "/" + imageModel;
                response = sendImageRequest(fallbackUrl, requestBody, effectiveToken);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Image generation failed " + response.statusCode() + ": " + body);
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
            if (contentType.contains("application/json")) {
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Image generation returned JSON instead of image: " + body);
            }

            String encoded = Base64.getEncoder().encodeToString(response.body());
            return "data:" + contentType + ";base64," + encoded;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image generation request was interrupted.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate chapter image suggestions.", e);
        }
    }

    private HttpResponse<byte[]> sendImageRequest(String url, String requestBody, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "image/png")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return HF_IMAGE_ROUTER_BASE_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildImageRequestBody(String prompt, int seed) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "inputs", prompt,
                    "parameters", Map.of(
                            "guidance_scale", 7.5,
                            "num_inference_steps", 28,
                            "seed", seed
                    ),
                    "options", Map.of(
                            "wait_for_model", true
                    )
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize image generation request.", e);
        }
    }

    private String buildOutlinePrompt(String topic, String description, String style, int numChapters) {
        // IMPORTANT: The app should rely only on the LLM output from this prompt.
        return """
                You are a professional book writing assistant.
                
                Using ONLY the information provided below, generate a chapter outline for the book.
                
                Rules:
                1) Return EXACTLY valid JSON (no markdown, no commentary, no extra keys).
                2) The JSON must match this schema:
                {
                  "outline": [
                    { "title": "Chapter 1: ...", "description": "1-2 sentences describing the chapter" }
                  ]
                }
                3) Create exactly {numChapters} chapters.
                4) Titles must be unique and progressively build the book.
                5) Style must influence tone and wording (do not change the structure requirement).
                
                Input:
                - Topic: {topic}
                - Additional description/context: {description}
                - Writing style: {style}
                """.replace("{numChapters}", String.valueOf(numChapters))
                .replace("{topic}", escapeForPrompt(topic))
                .replace("{description}", escapeForPrompt(description))
                .replace("{style}", escapeForPrompt(style));
    }

    private String buildChapterPrompt(String chapterTitle, String chapterDescription, String style) {
        return """
                You are a professional chapter writer.
                
                Write the full chapter text using ONLY the information provided below.
                
                Rules:
                1) Return EXACTLY valid JSON (no markdown, no commentary, no extra keys).
                2) The JSON must match this schema:
                { "content": "..." }
                3) Write around 600-900 words.
                4) Writing style must match the requested style.
                5) Use the chapter title and description to determine what to include.
                
                Input:
                - Chapter title: {chapterTitle}
                - Chapter description/context: {chapterDescription}
                - Writing style: {style}
                """.replace("{chapterTitle}", escapeForPrompt(chapterTitle))
                .replace("{chapterDescription}", escapeForPrompt(chapterDescription))
                .replace("{style}", escapeForPrompt(style));
    }

    private String escapeForPrompt(String s) {
        return s.replace("\r", " ").replace("\n", " ").trim();
    }

    private String generateJsonFromPrompt(String prompt, int maxNewTokens) {
        String effectiveToken = resolveHfToken();
        boolean hasToken = effectiveToken != null && !effectiveToken.isBlank();

        // Chat Completions request body (OpenAI-compatible)
        String jsonBody = """
                {
                  "model": %s,
                  "messages": [
                    { "role": "user", "content": %s }
                  ],
                  "max_tokens": %d,
                  "temperature": 0.7
                }
                """.formatted(quoteJsonString(model), quoteJsonString(prompt), maxNewTokens);

        // New endpoint: /chat/completions
        String base = hfBaseUrl.endsWith("/")
                ? hfBaseUrl.substring(0, hfBaseUrl.length() - 1)
                : hfBaseUrl;
        String url = base + "/chat/completions";

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        if (hasToken) {
            req.header("Authorization", "Bearer " + effectiveToken);
        }

        try {
            HttpResponse<String> resp = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                if (resp.statusCode() == 404) {
                    throw new IllegalStateException(
                            "LLM request failed with 404. Check model id: " + model + ". URL: " + url);
                }
                if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                    throw new IllegalStateException(
                            "Auth error (" + resp.statusCode() + "). Set HF_API_TOKEN. Response: " + resp.body());
                }
                throw new IllegalStateException("LLM request failed " + resp.statusCode() + ": " + resp.body());
            }

            // Chat Completions response: { "choices": [{ "message": { "content": "..." } }] }
            JsonNode root = objectMapper.readTree(resp.body());
            String generatedText = extractChatContent(root);
            return extractFirstJsonObject(generatedText);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request was interrupted.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Hugging Face LLM.", e);
        }
    }

    private String extractChatContent(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.hasNonNull("content")) {
                return message.get("content").asText();
            }
        }
        throw new IllegalStateException("Unexpected HF chat response format: " + root);
    }

    private String extractFirstJsonObject(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw new IllegalStateException("LLM did not return a JSON object. Raw output: " + text);
        }
        return text.substring(firstBrace, lastBrace + 1);
    }

    private String quoteJsonString(String s) {
        // Minimal JSON string escaping for safe embedding.
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }

    private String resolveHfToken() {
        return (hfToken == null || hfToken.isBlank()) ? System.getenv("HF_API_TOKEN") : hfToken;
    }

    private String limitWords(String value, int maxWords) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return "";
        }

        String[] words = normalized.split(" ");
        if (words.length <= maxWords) {
            return normalized;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                builder.append(" ");
            }
            builder.append(words[i]);
        }
        return builder.toString().trim();
    }
}
