package com.stargazing.bibliotech.catalogservice.book;

import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;

public interface BookService {

  BookResponse createBook(CreateBookRequest request);
}
