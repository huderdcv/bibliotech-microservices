package com.stargazing.bibliotech.catalogservice.book;

import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

  BookResponse createBook(CreateBookRequest request);

  Page<BookResponse> findAllBooks(Pageable pageable);

  BookResponse findOneByIsbn(String isbn);

  BookResponse reserveOne(String isbn);

  BookResponse returnOne(String isbn);

}
