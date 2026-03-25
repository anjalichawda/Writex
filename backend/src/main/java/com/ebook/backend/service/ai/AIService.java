package com.ebook.backend.service.ai;

import java.util.List;
import java.util.Map;

public interface AIService {
    List<Map<String, String>> generateOutline(String topic, String description, String style, int numChapters);
    String generateChapterContent(String chapterTitle, String chapterDescription, String style);
    String suggestGrammar(String text);
    String suggestNextWords(String chapterTitle, String currentContent, int maxWords);
    String generateChapterImageDataUrl(String chapterTitle, String chapterContent, String style);
    List<String> generateChapterImageSuggestions(String chapterTitle, String chapterContent, String prompt, String style, int count);
}

