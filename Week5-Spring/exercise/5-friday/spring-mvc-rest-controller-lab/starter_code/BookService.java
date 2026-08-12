package com.example.books.service;

import com.example.books.model.Book;
import java.util.List;
import java.util.Optional;

/**
 * STARTER CODE — Do not modify this file.
 * Implement your controller by calling these service methods.
 */
public interface BookService {
    List<Book> findAll();
    Optional<Book> findById(Long id);
    List<Book> findByAuthor(String author);
    Book save(Book book);
    Optional<Book> update(Long id, Book book);
    void delete(Long id);
}
