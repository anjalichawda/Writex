package com.ebook.backend.controller;

import com.ebook.backend.dto.ApiResponse;
import com.ebook.backend.model.Book;
import com.ebook.backend.model.BookCover;
import com.ebook.backend.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Book>>> getBooks(Authentication authentication) {
        List<Book> books = bookService.getAllBooks(authentication);
        return ResponseEntity.ok(ApiResponse.success(books, "Books fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@RequestBody Book book, Authentication authentication) {
        Book created = bookService.createBook(book, authentication);
        return ResponseEntity.ok(ApiResponse.success(created, "Book created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> getBookById(@PathVariable Long id) {
        Book b = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success(b, "Book fetched successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book savedBook = bookService.updateBook(id, bookDetails);
        return ResponseEntity.ok(ApiResponse.success(savedBook, "Book updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Book deleted successfully"));
    }

    @PutMapping("/{id}/cover")
    public ResponseEntity<ApiResponse<Book>> updateCover(@PathVariable Long id, @RequestParam("coverImage") MultipartFile file, Authentication authentication) {
        try {
            Book updated = bookService.updateCover(id, file, authentication);
            return ResponseEntity.ok(ApiResponse.success(updated, "Cover updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getCover(@PathVariable Long id) {
        try {
            BookCover bookCover = bookService.getCover(id);
            return ResponseEntity.ok()
                    .header("Content-Type", bookCover.getContentType())
                    .header("Cache-Control", "max-age=3600, public")
                    .body(bookCover.getData());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/{id}/cover")
    public ResponseEntity<ApiResponse<Void>> deleteCover(@PathVariable Long id, Authentication authentication) {
        bookService.deleteCover(id, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Cover deleted successfully"));
    }
}
