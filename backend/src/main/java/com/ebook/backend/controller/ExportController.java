package com.ebook.backend.controller;

import com.ebook.backend.model.Book;
import com.ebook.backend.model.Chapter;
import com.ebook.backend.service.BookService;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final BookService bookService;

    @GetMapping("/{bookId}/{format}")
    public ResponseEntity<byte[]> exportBook(@PathVariable Long bookId, @PathVariable String format) {
        Book book = bookService.getBookById(bookId);
        
        if (format.equalsIgnoreCase("PDF")) {
            return exportToPdf(book);
        }
        
        // Fallback for other formats (Mock)
        String content = "Exported book: " + book.getTitle() + " by " + book.getAuthor() + "\n\n";
        for (Chapter c : book.getChapters()) {
            content += "## " + c.getTitle() + "\n" + (c.getContent() != null ? c.getContent() : "") + "\n\n";
        }
        byte[] bytes = content.getBytes();
        String filename = "book_" + bookId + "." + format.toLowerCase();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    private ResponseEntity<byte[]> exportToPdf(Book book) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>")
                .append("body { font-family: sans-serif; padding: 40px; }")
                .append("h1 { color: #1e293b; text-align: center; }")
                .append("h2 { color: #334155; margin-top: 30px; border-bottom: 1px solid #e2e8f0; padding-bottom: 10px; }")
                .append(".author { text-align: center; color: #64748b; font-size: 1.2em; margin-bottom: 50px; }")
                .append(".subtitle { text-align: center; color: #64748b; font-style: italic; margin-top: -10px; margin-bottom: 20px; }")
                .append(".content { line-height: 1.6; color: #334155; }")
                .append(".page-break { page-break-after: always; }")
                .append("</style></head><body>");

            html.append("<h1>").append(book.getTitle()).append("</h1>");
            if (book.getSubtitle() != null && !book.getSubtitle().isEmpty()) {
                html.append("<div class='subtitle'>").append(book.getSubtitle()).append("</div>");
            }
            html.append("<div class='author'>By ").append(book.getAuthor()).append("</div>");
            html.append("<div class='page-break'></div>");

            if (book.getChapters() != null) {
                for (Chapter chapter : book.getChapters()) {
                    html.append("<h2>").append(chapter.getTitle()).append("</h2>");
                    html.append("<div class='content'>").append(chapter.getContent() != null ? chapter.getContent() : "").append("</div>");
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
