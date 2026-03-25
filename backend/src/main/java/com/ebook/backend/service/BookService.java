package com.ebook.backend.service;

import com.ebook.backend.model.Book;
import com.ebook.backend.model.BookCover;
import com.ebook.backend.model.Chapter;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BookService {
    List<Book> getAllBooks(Authentication authentication);
    Book createBook(Book book, Authentication authentication);
    Book getBookById(Long id);
    Book updateBook(Long id, Book bookDetails);
    void deleteBook(Long id);
    Book updateCover(Long id, MultipartFile file, Authentication authentication) throws IOException;
    void deleteCover(Long id, Authentication authentication);
    BookCover getCover(Long id);
    Chapter updateChapterCover(Long bookId, Long chapterId, MultipartFile file, Authentication authentication) throws IOException;
    Chapter getChapterCover(Long bookId, Long chapterId);
    void deleteChapterCover(Long bookId, Long chapterId, Authentication authentication);
}
