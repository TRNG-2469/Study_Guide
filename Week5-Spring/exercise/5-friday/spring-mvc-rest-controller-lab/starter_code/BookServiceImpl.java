package com.example.books.service;

import com.example.books.model.Book;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * STARTER CODE — Do not modify this file.
 *
 * In-memory stub implementation — no database required for this lab.
 * Replace this with a JpaRepository-backed implementation for Project 2.
 */
@Service
public class BookServiceImpl implements BookService {

    private final Map<Long, Book> store = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public BookServiceImpl() {
        // Seed data — available on startup
        save(new Book(null, "Clean Code", "Robert C. Martin", "978-0132350884"));
        save(new Book(null, "The Pragmatic Programmer", "David Thomas", "978-0201616224"));
        save(new Book(null, "Effective Java", "Joshua Bloch", "978-0134685991"));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Book> findByAuthor(String author) {
        if (author == null || author.isBlank()) return findAll();
        return store.values().stream()
                .filter(b -> b.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Book save(Book book) {
        long id = idCounter.getAndIncrement();
        book.setId(id);
        store.put(id, book);
        return book;
    }

    @Override
    public Optional<Book> update(Long id, Book book) {
        if (!store.containsKey(id)) return Optional.empty();
        book.setId(id);
        store.put(id, book);
        return Optional.of(book);
    }

    @Override
    public void delete(Long id) {
        store.remove(id);
    }
}
