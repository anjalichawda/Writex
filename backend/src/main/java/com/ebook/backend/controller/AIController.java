package com.ebook.backend.controller;

import com.ebook.backend.dto.ApiResponse;
import com.ebook.backend.service.ai.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/generate-outline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateOutline(@RequestBody Map<String, String> payload) {
        String topic = payload.getOrDefault("topic", "");
        String description = payload.getOrDefault("description", "");
        String style = payload.getOrDefault("style", "Informative");
        int numChapters = Integer.parseInt(payload.getOrDefault("numChapters", "5"));

        List<Map<String, String>> outline = aiService.generateOutline(topic, description, style, numChapters);
        Map<String, Object> response = new HashMap<>();
        response.put("outline", outline);
        return ResponseEntity.ok(ApiResponse.success(response, "Outline generated successfully"));
    }

    @PostMapping("/generate-chapter-content")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateChapterContent(@RequestBody Map<String, String> payload) {
        String chapterTitle = payload.getOrDefault("chapterTitle", "");
        String chapterDescription = payload.getOrDefault("chapterDescription", "");
        String style = payload.getOrDefault("style", "Informative");

        String content = aiService.generateChapterContent(chapterTitle, chapterDescription, style);
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        return ResponseEntity.ok(ApiResponse.success(response, "Chapter content generated successfully"));
    }

    @PostMapping("/grammar/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestGrammar(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("text", "");
        String suggestedText = aiService.suggestGrammar(text);

        Map<String, Object> response = new HashMap<>();
        response.put("suggestedText", suggestedText);
        return ResponseEntity.ok(ApiResponse.success(response, "Grammar suggestion generated successfully"));
    }
    @PostMapping("/autocomplete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autocomplete(@RequestBody Map<String, Object> payload) {
        String chapterTitle = asString(payload.get("chapterTitle"));
        String content = asString(payload.get("content"));
        int maxWords = parseInt(payload.get("maxWords"), 18);

        String completion = aiService.suggestNextWords(chapterTitle, content, maxWords);
        Map<String, Object> response = new HashMap<>();
        response.put("completion", completion);
        return ResponseEntity.ok(ApiResponse.success(response, "Autocomplete suggestion generated successfully"));
    }

    @PostMapping("/generate-chapter-images")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateChapterImages(@RequestBody Map<String, Object> payload) {
        String chapterTitle = asString(payload.get("chapterTitle"));
        String chapterContent = asString(payload.get("chapterContent"));
        String prompt = asString(payload.get("prompt"));
        String style = asString(payload.get("style"));
        int count = parseInt(payload.get("count"), 3);

        List<String> images = aiService.generateChapterImageSuggestions(chapterTitle, chapterContent, prompt, style, count);

        Map<String, Object> response = new HashMap<>();
        response.put("images", images);
        return ResponseEntity.ok(ApiResponse.success(response, "Chapter image suggestions generated successfully"));
    }

    @PostMapping("/generate-chapter-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateChapterImage(@RequestBody Map<String, String> payload) {
        String chapterTitle = payload.getOrDefault("chapterTitle", "");
        String chapterContent = payload.getOrDefault("chapterContent", "");
        String style = payload.getOrDefault("style", "");

        String imageDataUrl = aiService.generateChapterImageDataUrl(chapterTitle, chapterContent, style);

        Map<String, Object> response = new HashMap<>();
        response.put("imageDataUrl", imageDataUrl);
        return ResponseEntity.ok(ApiResponse.success(response, "Chapter image generated successfully"));
    }

    private int parseInt(Object raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String asString(Object raw) {
        return raw == null ? "" : raw.toString();
    }
}
