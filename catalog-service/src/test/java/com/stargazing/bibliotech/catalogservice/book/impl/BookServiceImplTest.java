package com.stargazing.bibliotech.catalogservice.book.impl;

import com.stargazing.bibliotech.catalogservice.book.Book;
import com.stargazing.bibliotech.catalogservice.book.BookRepository;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.book.mapper.BookMapper;
import com.stargazing.bibliotech.catalogservice.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private BookMapper bookMapper;

  @InjectMocks
  private BookServiceImpl bookService;

  // ===================================================================================
  //                        HELPERS FOR MOCK DATA
  // ===================================================================================

  private CreateBookRequest createMockRequest(String isbn, Integer totalCopies, Integer availableCopies) {
    return new CreateBookRequest(
      isbn,
      "Clean Code",
      "Robert C. Martin",
      totalCopies,
      availableCopies
    );
  }

  private Book createMockBook(String isbn, Integer totalCopies, Integer availableCopies) {
    return Book.builder()
      .isbn(isbn)
      .title("Clean Code")
      .author("Robert C. Martin")
      .totalCopies(totalCopies)
      .availableCopies(availableCopies)
      .build();
  }

  private BookResponse createMockResponse(Long id, String isbn, Integer totalCopies, Integer availableCopies) {
    return new BookResponse(
      id,
      isbn,
      "Clean Code",
      "Robert C. Martin",
      totalCopies,
      availableCopies,
      Instant.now(),
      Instant.now()
    );
  }

  // ===================================================================================
  //                        NESTED TESTS STRUCTURE
  // ===================================================================================

  @Nested
  @DisplayName("Method: createBook()")
  class CreateBookTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should successfully create a book when request is valid")
      void shouldCreateBookSuccessfullyWhenRequestIsValid() {
        // GIVEN
        String isbn = "978-0132350884";
        CreateBookRequest request = createMockRequest(isbn, 10, 5);
        Book mappedBook = createMockBook(isbn, 10, 5);
        Book savedBook = createMockBook(isbn, 10, 5);
        savedBook.setId(1L); // Simulate database assigning ID
        BookResponse expectedResponse = createMockResponse(1L, isbn, 10, 5);

        given(bookRepository.existsByIsbn(isbn)).willReturn(false);
        given(bookMapper.toEntity(request)).willReturn(mappedBook);
        given(bookRepository.save(mappedBook)).willReturn(savedBook);
        given(bookMapper.toResponse(savedBook)).willReturn(expectedResponse);

        // WHEN
        BookResponse response = bookService.createBook(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isbn()).isEqualTo(isbn);
        assertThat(response.totalCopies()).isEqualTo(10);
        assertThat(response.availableCopies()).isEqualTo(5);

        // Verify interactions
        then(bookRepository).should(times(1)).existsByIsbn(isbn);
        then(bookMapper).should(times(1)).toEntity(request);
        then(bookRepository).should(times(1)).save(mappedBook);
        then(bookMapper).should(times(1)).toResponse(savedBook);
      }
    }

    @Nested
    @DisplayName("Error Paths (Business Rule Validation Failures)")
    class ErrorPaths {

      @Test
      @DisplayName("Should throw IllegalArgumentException when available copies exceed total copies")
      void shouldThrowIllegalArgumentExceptionWhenAvailableCopiesExceedTotalCopies() {
        // GIVEN
        String isbn = "978-0132350884";
        CreateBookRequest request = createMockRequest(isbn, 10, 15); // 15 > 10

        // WHEN & THEN
        assertThatThrownBy(() -> bookService.createBook(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Available copies cannot be greater than total copies");

        // Verify DB and Mapper were never touched
        then(bookRepository).should(never()).existsByIsbn(anyString());
        then(bookRepository).should(never()).save(any(Book.class));
        then(bookMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw DuplicateResourceException when ISBN already exists")
      void shouldThrowDuplicateResourceExceptionWhenIsbnAlreadyExists() {
        // GIVEN
        String isbn = "978-0132350884";
        CreateBookRequest request = createMockRequest(isbn, 10, 10);

        given(bookRepository.existsByIsbn(isbn)).willReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> bookService.createBook(request))
          .isInstanceOf(DuplicateResourceException.class)
          .hasMessage("A book with ISBN '" + isbn + "' already exists");

        // Verify save and mapping were never called
        then(bookRepository).should(times(1)).existsByIsbn(isbn);
        then(bookMapper).shouldHaveNoInteractions();
        then(bookRepository).should(never()).save(any(Book.class));
      }
    }

    @Nested
    @DisplayName("Boundary & Edge Cases")
    class BoundaryAndEdgeCases {

      @Test
      @DisplayName("Should successfully create book when available copies equals total copies")
      void shouldCreateBookSuccessfullyWhenAvailableCopiesEqualsTotalCopies() {
        // GIVEN
        String isbn = "978-0132350884";
        CreateBookRequest request = createMockRequest(isbn, 10, 10); // Edge condition: equals
        Book mappedBook = createMockBook(isbn, 10, 10);
        Book savedBook = createMockBook(isbn, 10, 10);
        BookResponse expectedResponse = createMockResponse(1L, isbn, 10, 10);

        given(bookRepository.existsByIsbn(isbn)).willReturn(false);
        given(bookMapper.toEntity(request)).willReturn(mappedBook);
        given(bookRepository.save(mappedBook)).willReturn(savedBook);
        given(bookMapper.toResponse(savedBook)).willReturn(expectedResponse);

        // WHEN
        BookResponse response = bookService.createBook(request);

        // THEN
        assertThat(response).isNotNull();
        then(bookRepository).should(times(1)).save(mappedBook);
      }

      @Test
      @DisplayName("Should successfully create book when copies are zero")
      void shouldCreateBookSuccessfullyWhenCopiesAreZero() {
        // GIVEN
        String isbn = "978-0132350884";
        CreateBookRequest request = createMockRequest(isbn, 0, 0); // Edge condition: zeros
        Book mappedBook = createMockBook(isbn, 0, 0);
        Book savedBook = createMockBook(isbn, 0, 0);
        BookResponse expectedResponse = createMockResponse(1L, isbn, 0, 0);

        given(bookRepository.existsByIsbn(isbn)).willReturn(false);
        given(bookMapper.toEntity(request)).willReturn(mappedBook);
        given(bookRepository.save(mappedBook)).willReturn(savedBook);
        given(bookMapper.toResponse(savedBook)).willReturn(expectedResponse);

        // WHEN
        BookResponse response = bookService.createBook(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.totalCopies()).isZero();
        assertThat(response.availableCopies()).isZero();
        then(bookRepository).should(times(1)).save(mappedBook);
      }
    }
  }
}