package com.ebook.backend.service;

import com.ebook.backend.model.Book;
import com.ebook.backend.model.BookCover;
import com.ebook.backend.model.Chapter;
import com.ebook.backend.model.User;
import com.ebook.backend.repository.BookCoverRepository;
import com.ebook.backend.repository.BookRepository;
import com.ebook.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookCoverRepository bookCoverRepository;

    @Override
    public List<Book> getAllBooks(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return bookRepository.findByOwner(user);
    }

    @Override
    @Transactional
    public Book createBook(Book book, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        book.setOwner(user);
        if (book.getChapters() != null) {
            book.getChapters().forEach(chapter -> chapter.setBook(book));
        }
        return bookRepository.save(book);
    }

    @Override
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));
    }

    @Override
    @Transactional
    public Book updateBook(Long id, Book bookDetails) {
        Book book = getBookById(id);
        book.setTitle(bookDetails.getTitle());
        book.setSubtitle(bookDetails.getSubtitle());
        book.setDescription(bookDetails.getDescription());
        book.setAuthor(bookDetails.getAuthor());
        book.setCoverUrl(bookDetails.getCoverUrl());

        if (bookDetails.getChapters() != null) {
            List<Chapter> incomingChapters = bookDetails.getChapters();
            List<Chapter> existingChapters = book.getChapters();
            List<Chapter> finalChapters = new ArrayList<>();

            for (Chapter incoming : incomingChapters) {
                incoming.setBook(book);
                if (incoming.getId() != null) {
                    existingChapters.stream()
                            .filter(e -> e.getId().equals(incoming.getId()))
                            .findFirst()
                            .ifPresentOrElse(existing -> {
                                existing.setTitle(incoming.getTitle());
                                existing.setContent(incoming.getContent());
                                existing.setOrderIndex(incoming.getOrderIndex());
                                finalChapters.add(existing);
                            }, () -> finalChapters.add(incoming));
                } else {
                    finalChapters.add(incoming);
                }
            }
            existingChapters.clear();
            existingChapters.addAll(finalChapters);
        }

        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Book updateCover(Long id, MultipartFile file, Authentication authentication) throws IOException {
        Book book = getBookById(id);
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: Not the owner");
        }

        BookCover bookCover = bookCoverRepository.findByBookId(id).orElse(new BookCover());
        bookCover.setBook(book);
        bookCover.setData(file.getBytes());
        bookCover.setContentType(file.getContentType());
        bookCoverRepository.save(bookCover);

        book.setCoverUrl("/api/books/" + id + "/cover");
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void deleteCover(Long id, Authentication authentication) {
        Book book = getBookById(id);
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: Not the owner");
        }

        bookCoverRepository.findByBookId(id).ifPresent(cover -> {
            book.setBookCover(null);
            bookCoverRepository.delete(cover);
        });
        book.setCoverUrl(null);
        bookRepository.save(book);
    }

    @Override
    public BookCover getCover(Long id) {
        return bookCoverRepository.findByBookId(id)
                .orElseThrow(() -> new RuntimeException("Cover not found"));
    }

    @Override
    public Chapter updateChapterCover(Long bookId, Long chapterId, MultipartFile file, Authentication authentication) throws IOException {
        return null;
    }

    @Override
    public Chapter getChapterCover(Long bookId, Long chapterId) {
        return null;
    }

    @Override
    public void deleteChapterCover(Long bookId, Long chapterId, Authentication authentication) {

    }
}
