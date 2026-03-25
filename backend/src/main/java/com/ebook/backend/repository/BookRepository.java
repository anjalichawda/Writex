package com.ebook.backend.repository;

import com.ebook.backend.model.Book;
import com.ebook.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByOwner(User owner);
}
