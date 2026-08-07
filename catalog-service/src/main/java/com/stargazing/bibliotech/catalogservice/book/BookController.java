package com.stargazing.bibliotech.catalogservice.book;

import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/catalog/books")
@RequiredArgsConstructor
@Tag(name = "Book Catalog", description = "Endpoints for managing book records in the catalog")
public class BookController {

  //- DEPENDENCY INJECTIONS
  private final BookService bookService;

  //- METHODS

  //-- CREATE A BOOK
  @Operation(
    summary = "Create a new book entry",
    description = "Saves a new book in the catalog. Verifies that the provided ISBN is unique before saving."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "201",
      description = "Book created successfully",
      headers = @Header(name = "Location", description = "URI of the newly created resource", schema = @Schema(type = "string")),
      content = @Content(schema = @Schema(implementation = BookResponse.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Invalid payload or validation error",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Conflict - Duplicate ISBN exists",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
  })
  @PostMapping
  public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
    BookResponse createdBook = bookService.createBook(request);

    // Build Location URI: /api/v1/catalog/books/{id}
    URI location = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(createdBook.id())
      .toUri();

    return ResponseEntity.created(location).body(createdBook);
  }

  //-- FIND ALL BOOKS
  @Operation(
    summary = "Retrieve a paginated list of books",
    description = "Fetches all books currently registered in the catalog, supporting pagination and sorting."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Successful retrieval of the book catalog"
    )
  })
  @GetMapping
  public ResponseEntity<PageResponse<BookResponse>> findAllBooks(
    @ParameterObject
    @PageableDefault(page = 0, size = 10, sort = "title", direction = Sort.Direction.ASC)
    Pageable pageable
  ) {

    Page<BookResponse> bookResponsePage = bookService.findAllBooks(pageable);

    return ResponseEntity.ok(new PageResponse<>(bookResponsePage));
  }

  //-- FIND ONE BOOK BY ISBN
  @Operation(
    summary = "Retrieve a single book by ISBN",
    description = "Fetches the details of a specific book using its unique International Standard Book Number (ISBN)."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Successful retrieval of the book",
      content = @Content(schema = @Schema(implementation = BookResponse.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - No book exists with the provided ISBN",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
  })
  @GetMapping("/{isbn}")
  public ResponseEntity<BookResponse> findOneByIsbn(
    @Parameter(description = "The unique ISBN of the book to retrieve", example = "978-0134685991")
    @PathVariable
    String isbn
  ) {

    BookResponse bookResponse = bookService.findOneByIsbn(isbn);

    return ResponseEntity.ok(bookResponse);
  }

  //-- RESERVE ONE BOOK
  @Operation(
    summary = "Reserve a single copy of a book",
    description = "Decrements the available copies of a specific book by 1. Fails if the book is out of stock."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Successfully reserved the book",
      content = @Content(schema = @Schema(implementation = BookResponse.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - The book is currently out of stock",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - No book exists with the provided ISBN",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
  })
  @PostMapping("/{isbn}/reserve")
  public ResponseEntity<BookResponse> reserveOne(
    @Parameter(description = "The unique ISBN of the book to reserve", example = "978-0134685991")
    @PathVariable
    String isbn
  ) {

    BookResponse bookResponse = bookService.reserveOne(isbn);

    return ResponseEntity.ok(bookResponse);
  }
}