package com.ebook.backend.controller;

import com.ebook.backend.model.Book;
import com.ebook.backend.model.BookCover;
import com.ebook.backend.model.Chapter;
import com.ebook.backend.repository.BookCoverRepository;
import com.ebook.backend.service.BookService;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final BookService bookService;
    private final BookCoverRepository bookCoverRepository;

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    @GetMapping("/{bookId}/{format}")
    public ResponseEntity<byte[]> exportBook(@PathVariable Long bookId, @PathVariable String format) {
        Book book = bookService.getBookById(bookId);

        if (format.equalsIgnoreCase("pdf")) {
            return exportToPdf(book);
        }

        StringBuilder content = new StringBuilder();
        content.append("Exported book: ").append(book.getTitle())
                .append(" by ").append(book.getAuthor()).append("\n\n");
        for (Chapter c : book.getChapters()) {
            content.append("## ").append(c.getTitle()).append("\n")
                    .append(c.getContent() != null ? c.getContent() : "").append("\n\n");
        }
        String filename = "book_" + bookId + "." + format.toLowerCase();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content.toString().getBytes());
    }

    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        Node document = MD_PARSER.parse(markdown);
        return MD_RENDERER.render(document);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private ResponseEntity<byte[]> exportToPdf(Book book) {
        try {
            // Load cover image bytes directly — bypasses lazy loading issue
            String coverImageTag = "";
            Optional<BookCover> coverOpt = bookCoverRepository.findByBookId(book.getId());
            if (coverOpt.isPresent()) {
                BookCover cover = coverOpt.get();
                if (cover.getData() != null && cover.getData().length > 0) {
                    String contentType = cover.getContentType() != null
                            ? cover.getContentType()
                            : "image/jpeg";
                    String base64 = Base64.getEncoder().encodeToString(cover.getData());
                    coverImageTag = "<img src='data:" + contentType + ";base64," + base64
                            + "' class='cover-img' alt='Book Cover'/>";
                }
            }

            // Author falls back to "Unknown" if null/blank
            String authorName = (book.getAuthor() != null && !book.getAuthor().isBlank())
                    ? book.getAuthor()
                    : "Unknown Author";

            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'/><style>")
                    .append("body { font-family: sans-serif; padding: 40px; }")
                    .append("h1 { color: #1e293b; text-align: center; margin-top: 20px; }")
                    .append("h2 { color: #334155; margin-top: 30px; border-bottom: 1px solid #e2e8f0; padding-bottom: 10px; }")
                    .append("h3 { color: #475569; margin-top: 20px; }")
                    .append("h4 { color: #64748b; margin-top: 16px; }")
                    .append(".cover-page { text-align: center; padding-top: 60px; }")
                    .append(".cover-img { max-width: 340px; width: 100%; border-radius: 8px; margin-bottom: 32px; display: block; margin-left: auto; margin-right: auto; }")
                    .append(".author { text-align: center; color: #64748b; font-size: 1.2em; margin-bottom: 50px; }")
                    .append(".subtitle { text-align: center; color: #64748b; font-style: italic; margin-top: -10px; margin-bottom: 20px; }")
                    .append(".content { line-height: 1.8; color: #334155; }")
                    .append(".content p { margin: 0 0 12px 0; }")
                    .append(".content ul, .content ol { margin: 0 0 12px 20px; }")
                    .append(".content li { margin-bottom: 4px; }")
                    .append(".content strong { font-weight: bold; }")
                    .append(".content em { font-style: italic; }")
                    .append(".content blockquote { border-left: 3px solid #cbd5e1; margin: 12px 0; padding: 8px 16px; color: #64748b; }")
                    .append(".content code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.9em; }")
                    .append(".content pre { background: #f1f5f9; padding: 12px; border-radius: 6px; }")
                    .append(".content hr { border: none; border-top: 1px solid #e2e8f0; margin: 20px 0; }")
                    .append(".page-break { page-break-after: always; }")
                    .append("</style></head><body>");

            // Cover page
            html.append("<div class='cover-page'>");
            if (!coverImageTag.isEmpty()) {
                html.append(coverImageTag);
            }
            html.append("<h1>").append(escapeHtml(book.getTitle())).append("</h1>");
            if (book.getSubtitle() != null && !book.getSubtitle().isBlank()) {
                html.append("<div class='subtitle'>").append(escapeHtml(book.getSubtitle())).append("</div>");
            }
            html.append("<div class='author'>By ").append(escapeHtml(authorName)).append("</div>");
            html.append("</div>"); // close cover-page
            html.append("<div class='page-break'></div>");

            // Chapters
            if (book.getChapters() != null) {
                for (Chapter chapter : book.getChapters()) {
                    html.append("<h2>").append(escapeHtml(chapter.getTitle())).append("</h2>");
                    html.append("<div class='content'>")
                            .append(markdownToHtml(chapter.getContent()))
                            .append("</div>");
                    html.append("<div class='page-break'></div>");
                }
            }

            html.append("</body></html>");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html.toString(), baos);

            String filename = book.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}