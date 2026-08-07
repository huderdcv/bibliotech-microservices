package com.stargazing.bibliotech.catalogservice.book.impl;

import com.stargazing.bibliotech.catalogservice.book.Book;
import com.stargazing.bibliotech.catalogservice.book.BookRepository;
import com.stargazing.bibliotech.catalogservice.book.BookService;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.book.mapper.BookMapper;
import com.stargazing.bibliotech.catalogservice.common.exception.DuplicateResourceException;
import com.stargazing.bibliotech.catalogservice.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  //- DEPENDENCY INJECTION
  private final BookRepository bookRepository;
  private final BookMapper bookMapper;

  //- METHODS

  //-- CREATE A BOOK
  @Override
  @Transactional(rollbackFor = Exception.class)
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

  //-- FIND ALL BOOKS
  @Override
  @Transactional(readOnly = true)
  public Page<BookResponse> findAllBooks(Pageable pageable) {
    log.info("Fetching page of books. Page number: {}, Page size: {}", pageable.getPageNumber(), pageable.getPageSize());

    Page<Book> bookEntityPage = bookRepository.findAll(pageable);

    log.info("Successfully retrieved {} books from the database", bookEntityPage.getNumberOfElements());
    return bookEntityPage.map(bookMapper::toResponse);
  }

  //-- FIND BY ISBN
  @Override
  @Transactional(readOnly = true)
  public BookResponse findOneByIsbn(String isbn) {
    log.info("Attempting to find book with ISBN: {}", isbn);

    // 1. Validations
    Book book = bookRepository.findByIsbn(isbn)
      .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN: " + isbn + " not found"));

    // 2. Map & answer
    log.info("Successfully found book with ID: {} and ISBN: {}", book.getId(), book.getIsbn());
    return bookMapper.toResponse(book);
  }

  //-- RESERVE ONE BOOK
  @Override
  @Transactional(rollbackFor = Exception.class)
  public BookResponse reserveOne(String isbn) {
    log.info("Attempting to reserve one copy of book with ISBN: {}", isbn);

    // 1. Validations
    Book book = bookRepository.findByIsbn(isbn)
      .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN: " + isbn + " not found"));

    if (book.getAvailableCopies() <= 0) {
      throw new IllegalStateException("This book doesn't have available copies");
    }

    // 2. Modify db
    book.setAvailableCopies(book.getAvailableCopies() - 1);

    // 3. Map & answer
    log.info("Successfully reserved book with ISBN: {}. Remaining available copies: {}", isbn, book.getAvailableCopies());
    return bookMapper.toResponse(book);
  }

  //-- RETURN ONE BOOK
  @Override
  @Transactional(rollbackFor = Exception.class)
  public BookResponse returnOne(String isbn) {
    log.info("Attempting to return one copy of book with ISBN: {}", isbn);

    // 1. Validations
    Book book = bookRepository.findByIsbn(isbn)
      .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN: " + isbn + " not found"));

    if (book.getAvailableCopies() >= book.getTotalCopies()) {
      throw new IllegalStateException("Cannot return book. All physical copies are already in the inventory");
    }

    // 2. Modify db
    book.setAvailableCopies(book.getAvailableCopies() + 1);

    // 3. Map & answer
    log.info("Successfully returned book with ISBN: {}. Total available copies: {}", isbn, book.getAvailableCopies());
    return bookMapper.toResponse(book);
  }
}
