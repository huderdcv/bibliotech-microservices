package com.stargazing.bibliotech.catalogservice.book.impl;

import com.stargazing.bibliotech.catalogservice.book.Book;
import com.stargazing.bibliotech.catalogservice.book.BookRepository;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.book.mapper.BookMapper;
import com.stargazing.bibliotech.catalogservice.common.exception.DuplicateResourceException;
import com.stargazing.bibliotech.catalogservice.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

  @Nested
  @DisplayName("Method: findAllBooks()")
  class FindAllBooksTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should successfully return a populated page of books when data exists")
      void shouldReturnPopulatedPageWhenDataExists() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);

        Book book1 = createMockBook("978-0132350884", 10, 5);
        Book book2 = createMockBook("978-0134685991", 5, 2);
        List<Book> bookList = List.of(book1, book2);
        Page<Book> mockEntityPage = new PageImpl<>(bookList, pageable, bookList.size());

        BookResponse response1 = createMockResponse(1L, "978-0132350884", 10, 5);
        BookResponse response2 = createMockResponse(2L, "978-0134685991", 5, 2);

        given(bookRepository.findAll(pageable)).willReturn(mockEntityPage);
        given(bookMapper.toResponse(book1)).willReturn(response1);
        given(bookMapper.toResponse(book2)).willReturn(response2);

        // WHEN
        Page<BookResponse> resultPage = bookService.findAllBooks(pageable);

        // THEN
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).hasSize(2);
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getContent()).containsExactly(response1, response2);

        // Verify interactions
        then(bookRepository).should(times(1)).findAll(pageable);
        then(bookMapper).should(times(1)).toResponse(book1);
        then(bookMapper).should(times(1)).toResponse(book2);
      }
    }

    @Nested
    @DisplayName("Edge Cases & Empty States")
    class EmptyStates {

      @Test
      @DisplayName("Should return an empty page when no books exist in the database")
      void shouldReturnEmptyPageWhenDatabaseIsEmpty() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> emptyEntityPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(bookRepository.findAll(pageable)).willReturn(emptyEntityPage);

        // WHEN
        Page<BookResponse> resultPage = bookService.findAllBooks(pageable);

        // THEN
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.isEmpty()).isTrue();
        assertThat(resultPage.getContent()).isEmpty();
        assertThat(resultPage.getTotalElements()).isZero();

        // Verify interactions
        then(bookRepository).should(times(1)).findAll(pageable);
        then(bookMapper).shouldHaveNoInteractions(); // toResponse should never be called
      }
    }
  }

  @Nested
  @DisplayName("Method: findOneByIsbn()")
  class FindOneByIsbnTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should successfully return a book response when the ISBN exists")
      void shouldReturnBookResponseSuccessfullyWhenIsbnExists() {
        // GIVEN
        String isbn = "978-0132350884";
        Book foundBook = createMockBook(isbn, 10, 5);
        foundBook.setId(1L);
        BookResponse expectedResponse = createMockResponse(1L, isbn, 10, 5);

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(foundBook));
        given(bookMapper.toResponse(foundBook)).willReturn(expectedResponse);

        // WHEN
        BookResponse response = bookService.findOneByIsbn(isbn);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isbn()).isEqualTo(isbn);

        // Verify interactions
        then(bookRepository).should(times(1)).findByIsbn(isbn);
        then(bookMapper).should(times(1)).toResponse(foundBook);
      }
    }

    @Nested
    @DisplayName("Error Paths (Business Rule Validation Failures)")
    class ErrorPaths {

      @Test
      @DisplayName("Should throw ResourceNotFoundException when no book matches the provided ISBN")
      void shouldThrowResourceNotFoundExceptionWhenIsbnDoesNotExist() {
        // GIVEN
        String isbn = "978-0132350884";

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> bookService.findOneByIsbn(isbn))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Book with ISBN: " + isbn + " not found");

        // Verify repository was checked but mapper was never called (fail-fast)
        then(bookRepository).should(times(1)).findByIsbn(isbn);
        then(bookMapper).shouldHaveNoInteractions();
      }
    }
  }
}