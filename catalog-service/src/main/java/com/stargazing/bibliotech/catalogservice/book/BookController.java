package com.stargazing.bibliotech.catalogservice.book;

import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
  @PostMapping
  @Operation(
    summary = "Create a new book entry",
    description = "Saves a new book in the catalog. Verifies that the provided ISBN is unique before saving."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "201",
      description = "Book created successfully (AC2)",
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
      description = "Conflict - Duplicate ISBN exists (AC3)",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
  })
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
}