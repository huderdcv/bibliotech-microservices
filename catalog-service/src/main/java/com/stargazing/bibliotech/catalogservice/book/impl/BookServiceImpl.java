package com.stargazing.bibliotech.catalogservice.book.impl;

import com.stargazing.bibliotech.catalogservice.book.Book;
import com.stargazing.bibliotech.catalogservice.book.BookRepository;
import com.stargazing.bibliotech.catalogservice.book.BookService;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.book.mapper.BookMapper;
import com.stargazing.bibliotech.catalogservice.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  //- DEPENDENCY INJECTION
  private final BookRepository bookRepository;
  private final BookMapper bookMapper;

  //- METHODS
  @Override
  public BookResponse createBook(CreateBookRequest request) {
    log.info("Attempting to create book with ISBN: {}", request.isbn());

    // 1. Validations
    if (request.availableCopies() > request.totalCopies()) {
      throw new IllegalArgumentException("Available copies cannot be greater than total copies");
    }

    if (bookRepository.existsByIsbn(request.isbn())) {
      // log.warn("Book creation failed. ISBN {} already exists", request.isbn());
      throw new DuplicateResourceException("A book with ISBN '" + request.isbn() + "' already exists");
    }

    // 2. Map & persist
    Book newBook = bookMapper.toEntity(request);
    Book savedBook = bookRepository.save(newBook);

    // 3. Answer
    log.info("Book successfully created with ID: {} and ISBN: {}", savedBook.getId(), savedBook.getIsbn());
    return bookMapper.toResponse(savedBook);
  }
}
