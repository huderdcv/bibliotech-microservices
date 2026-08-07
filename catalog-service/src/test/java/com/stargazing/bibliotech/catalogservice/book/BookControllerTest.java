package com.stargazing.bibliotech.catalogservice.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import com.stargazing.bibliotech.catalogservice.common.exception.DuplicateResourceException;
import com.stargazing.bibliotech.catalogservice.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookController.class)
class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookService bookService;

  private static final String BASE_URL = "/api/v1/catalog/books";

  // ===================================================================================
  //                        HELPERS FOR MOCK DATA
  // ===================================================================================

  private CreateBookRequest createValidMockRequest() {
    return new CreateBookRequest(
      "978-0134685991",
      "Effective Java",
      "Joshua Bloch",
      10,
      10
    );
  }

  private CreateBookRequest createInvalidMockRequest() {
    // Contains invalid data (blank title, negative copies) to trigger @Valid
    return new CreateBookRequest(
      "invalid-isbn",
      "", // Blank title
      "Joshua Bloch",
      -5, // Negative total copies
      10
    );
  }

  private BookResponse createMockResponse() {
    return new BookResponse(
      1L,
      "978-0134685991",
      "Effective Java",
      "Joshua Bloch",
      10,
      10,
      Instant.now(),
      Instant.now()
    );
  }

  private Page<BookResponse> createMockBookPage() {
    return new PageImpl<>(List.of(createMockResponse()), PageRequest.of(0, 10), 1);
  }

  private Page<BookResponse> createEmptyBookPage() {
    return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
  }

  // ===================================================================================
  //                        TESTS (LOGIC & VALIDATIONS)
  // ===================================================================================

  @Nested
  @DisplayName("Method: createBook()")
  class CreateBookTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 201 Created, build correct Location header, and serialize exact response body")
      void shouldSuccessfullyCreateBookWithHeadersAndBody() throws Exception {
        // GIVEN
        CreateBookRequest request = createValidMockRequest();
        BookResponse mockResponse = createMockResponse();

        given(bookService.createBook(any(CreateBookRequest.class))).willReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/catalog/books/1")))
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.isbn").value("978-0134685991"))
          .andExpect(jsonPath("$.title").value("Effective Java"))
          .andExpect(jsonPath("$.author").value("Joshua Bloch"))
          .andExpect(jsonPath("$.totalCopies").value(10))
          .andExpect(jsonPath("$.availableCopies").value(10))
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(bookService).should().createBook(any(CreateBookRequest.class));
      }
    }

    @Nested
    @DisplayName("Payload Validation Scenarios (@Valid)")
    class PayloadValidationTests {

      @Test
      @DisplayName("Should return 400 Bad Request when JSON fields violate constraints")
      void shouldReturnBadRequestWhenJsonFieldsAreInvalid() throws Exception {
        // GIVEN
        CreateBookRequest invalidRequest = createInvalidMockRequest();

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());

        // Verify the service layer was protected and never called
        then(bookService).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should return 400 Bad Request when request body is entirely missing")
      void shouldReturnBadRequestWhenRequestBodyIsMissing() throws Exception {
        // GIVEN a request with no content

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

        then(bookService).shouldHaveNoInteractions();
      }
    }

    @Nested
    @DisplayName("Exception Mapping (Global Exception Handler Integration)")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map DuplicateResourceException to 409 Conflict")
      void shouldMapDuplicateResourceExceptionToConflict() throws Exception {
        // GIVEN
        CreateBookRequest request = createValidMockRequest();

        given(bookService.createBook(any(CreateBookRequest.class)))
          .willThrow(new DuplicateResourceException("A book with ISBN '978-0134685991' already exists"));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
      }

      @Test
      @DisplayName("Should map IllegalArgumentException to 400 Bad Request")
      void shouldMapIllegalArgumentExceptionToBadRequest() throws Exception {
        // GIVEN
        CreateBookRequest request = createValidMockRequest();

        given(bookService.createBook(any(CreateBookRequest.class)))
          .willThrow(new IllegalArgumentException("Available copies cannot be greater than total copies"));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("Method: findAllBooks()")
  class FindAllBooksTests {

    @Test
    @DisplayName("Should retrieve a paginated list of books using default parameters (200 OK)")
    void shouldRetrieveBooksWithDefaultPagination() throws Exception {
      // GIVEN
      given(bookService.findAllBooks(any(Pageable.class))).willReturn(createMockBookPage());

      // WHEN & THEN
      mockMvc.perform(get(BASE_URL)
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").exists()) // Adjust JSON path based on your exact PageResponse fields
        .andExpect(jsonPath("$.content").isArray());

      // VERIFY CAPTOR
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      then(bookService).should(times(1)).findAllBooks(pageableCaptor.capture());

      Pageable capturedPageable = pageableCaptor.getValue();
      assertThat(capturedPageable.getPageNumber()).isZero();
      assertThat(capturedPageable.getPageSize()).isEqualTo(10);
      assertThat(capturedPageable.getSort().getOrderFor("title")).isNotNull();
      assertThat(capturedPageable.getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Should retrieve a paginated list of books using custom client parameters (200 OK)")
    void shouldRetrieveBooksWithCustomPagination() throws Exception {
      // GIVEN
      given(bookService.findAllBooks(any(Pageable.class))).willReturn(createMockBookPage());

      // WHEN & THEN
      mockMvc.perform(get(BASE_URL)
          .param("page", "2")
          .param("size", "5")
          .param("sort", "author,desc")
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

      // VERIFY CAPTOR
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      then(bookService).should(times(1)).findAllBooks(pageableCaptor.capture());

      Pageable capturedPageable = pageableCaptor.getValue();
      assertThat(capturedPageable.getPageNumber()).isEqualTo(2);
      assertThat(capturedPageable.getPageSize()).isEqualTo(5);
      assertThat(capturedPageable.getSort().getOrderFor("author")).isNotNull();
      assertThat(capturedPageable.getSort().getOrderFor("author").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Should return 200 OK with empty array when the catalog is empty")
    void shouldReturnEmptyListWhenCatalogIsEmpty() throws Exception {
      // GIVEN
      given(bookService.findAllBooks(any(Pageable.class))).willReturn(createEmptyBookPage());

      // WHEN & THEN
      mockMvc.perform(get(BASE_URL)
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty()) // Or '$.content' depending on your PageResponse
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));

      then(bookService).should(times(1)).findAllBooks(any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("Method: findOneByIsbn()")
  class FindOneByIsbnTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 200 OK and serialize exact response body when book is found")
      void shouldSuccessfullyRetrieveBookByIsbn() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        BookResponse mockResponse = createMockResponse();

        given(bookService.findOneByIsbn(isbn)).willReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(get(BASE_URL + "/{isbn}", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.isbn").value(isbn))
          .andExpect(jsonPath("$.title").value("Effective Java"))
          .andExpect(jsonPath("$.author").value("Joshua Bloch"))
          .andExpect(jsonPath("$.totalCopies").value(10))
          .andExpect(jsonPath("$.availableCopies").value(10))
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(bookService).should().findOneByIsbn(isbn);
      }
    }

    @Nested
    @DisplayName("Exception Mapping (Global Exception Handler Integration)")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map ResourceNotFoundException to 404 Not Found")
      void shouldMapResourceNotFoundExceptionToNotFound() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        String expectedErrorMessage = "Book with ISBN: " + isbn + " not found";

        given(bookService.findOneByIsbn(isbn))
          .willThrow(new ResourceNotFoundException(expectedErrorMessage));

        // WHEN & THEN
        mockMvc.perform(get(BASE_URL + "/{isbn}", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value(expectedErrorMessage));

        then(bookService).should().findOneByIsbn(isbn);
      }
    }
  }

  @Nested
  @DisplayName("Method: reserveOne()")
  class ReserveOneTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 200 OK and serialize updated book response when reservation succeeds")
      void shouldSuccessfullyReserveBookAndReturnUpdatedDetails() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        BookResponse mockResponse = createMockResponse(); // Assuming this represents the state AFTER reservation

        given(bookService.reserveOne(isbn)).willReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/reserve", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.isbn").value(isbn))
          .andExpect(jsonPath("$.title").value("Effective Java"))
          .andExpect(jsonPath("$.author").value("Joshua Bloch"))
          .andExpect(jsonPath("$.totalCopies").value(10))
          .andExpect(jsonPath("$.availableCopies").value(10)) // In a real scenario, this would be 9
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(bookService).should().reserveOne(isbn);
      }
    }

    @Nested
    @DisplayName("Exception Mapping (Global Exception Handler Integration)")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map ResourceNotFoundException to 404 Not Found")
      void shouldMapResourceNotFoundExceptionToNotFound() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        String expectedErrorMessage = "Book with ISBN: " + isbn + " not found";

        given(bookService.reserveOne(isbn))
          .willThrow(new ResourceNotFoundException(expectedErrorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/reserve", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value(expectedErrorMessage));

        then(bookService).should().reserveOne(isbn);
      }

      @Test
      @DisplayName("Should map IllegalStateException (Out of Stock) to 400 Bad Request")
      void shouldMapOutOfStockExceptionToBadRequest() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        String expectedErrorMessage = "This book doesn't have available copies";

        given(bookService.reserveOne(isbn))
          .willThrow(new IllegalStateException(expectedErrorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/reserve", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value(expectedErrorMessage));

        then(bookService).should().reserveOne(isbn);
      }
    }
  }

  @Nested
  @DisplayName("Method: returnOne()")
  class ReturnOneTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 200 OK and serialize updated book response when return succeeds")
      void shouldSuccessfullyReturnBookAndReturnUpdatedDetails() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        BookResponse mockResponse = createMockResponse();

        given(bookService.returnOne(isbn)).willReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/return", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.isbn").value(isbn))
          .andExpect(jsonPath("$.title").value("Effective Java"))
          .andExpect(jsonPath("$.author").value("Joshua Bloch"))
          .andExpect(jsonPath("$.totalCopies").value(10))
          .andExpect(jsonPath("$.availableCopies").value(10))
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(bookService).should().returnOne(isbn);
      }
    }

    @Nested
    @DisplayName("Exception Mapping (Global Exception Handler Integration)")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map ResourceNotFoundException to 404 Not Found")
      void shouldMapResourceNotFoundExceptionToNotFound() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        String expectedErrorMessage = "Book with ISBN: " + isbn + " not found";

        given(bookService.returnOne(isbn))
          .willThrow(new ResourceNotFoundException(expectedErrorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/return", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value(expectedErrorMessage));

        then(bookService).should().returnOne(isbn);
      }

      @Test
      @DisplayName("Should map IllegalStateException (Over-Capacity) to 400 Bad Request")
      void shouldMapOverCapacityExceptionToBadRequest() throws Exception {
        // GIVEN
        String isbn = "978-0134685991";
        String expectedErrorMessage = "Cannot return book. All physical copies are already in the inventory";

        given(bookService.returnOne(isbn))
          .willThrow(new IllegalStateException(expectedErrorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/{isbn}/return", isbn)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value(expectedErrorMessage));

        then(bookService).should().returnOne(isbn);
      }
    }
  }
}