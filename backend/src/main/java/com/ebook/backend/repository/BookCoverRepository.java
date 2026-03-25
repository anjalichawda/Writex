package com.ebook.backend.repository;

import com.ebook.backend.model.BookCover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookCoverRepository extends JpaRepository<BookCover, Long> {
    Optional<BookCover> findByBookId(Long bookId);
}
