package com.stargazing.bibliotech.loanservice.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogConflictException;
import com.stargazing.bibliotech.loanservice.common.exception.ResourceNotFoundException;
import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.endsWith;

@WebMvcTest(controllers = LoanController.class)
class LoanControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private LoanService loanService;

  @MockitoBean
  private Clock clock;

  private static final String BASE_URL = "/api/v1/loans";

  // ===================================================================================
  //                        HELPERS FOR MOCK DATA
  // ===================================================================================

  private BorrowBookRequest createValidMockRequest() {
    return new BorrowBookRequest(
      "978-0134685991",
      "user-12345"
    );
  }

  private BorrowBookRequest createInvalidMockRequest() {
    // Contains invalid data (blank ISBN, malformed member ID) to trigger @Valid
    return new BorrowBookRequest(
      "",
      "invalid-user-format"
    );
  }

  private LoanResponse createMockResponse() {
    return new LoanResponse(
      1L,
      "978-0134685991",
      "user-12345",
      LocalDate.of(2026, 8, 7),
      LocalDate.of(2026, 8, 21),
      LoanStatus.ACTIVE,
      Instant.parse("2026-08-07T21:25:15Z"),
      Instant.parse("2026-08-07T21:25:15Z")
    );
  }

  // ===================================================================================
  //                        TESTS (LOGIC & VALIDATIONS)
  // ===================================================================================

  @Nested
  @DisplayName("Method: borrowBook()")
  class BorrowBookTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 201 Created, build correct Location header, and serialize response body")
      void shouldSuccessfullyBorrowBookWithHeadersAndBody() throws Exception {
        // GIVEN
        BorrowBookRequest request = createValidMockRequest();
        LoanResponse mockResponse = createMockResponse();

        given(loanService.borrowBook(any(BorrowBookRequest.class))).willReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/borrow")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", endsWith("/api/v1/loans/1")))
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.bookIsbn").value("978-0134685991"))
          .andExpect(jsonPath("$.memberId").value("user-12345"))
          .andExpect(jsonPath("$.loanDate").value("2026-08-07"))
          .andExpect(jsonPath("$.dueDate").value("2026-08-21"))
          .andExpect(jsonPath("$.status").value("ACTIVE"))
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(loanService).should().borrowBook(any(BorrowBookRequest.class));
      }
    }

    @Nested
    @DisplayName("Payload Validation Scenarios (@Valid)")
    class PayloadValidationTests {

      @Test
      @DisplayName("Should return 400 Bad Request when JSON fields violate constraints")
      void shouldReturnBadRequestWhenJsonFieldsAreInvalid() throws Exception {
        // GIVEN
        BorrowBookRequest invalidRequest = createInvalidMockRequest();

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/borrow")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());

        // Verify the service layer was protected and never called
        then(loanService).shouldHaveNoInteractions();
      }
    }

    @Nested
    @DisplayName("Exception Mapping (Global Exception Handler Integration)")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map CatalogConflictException to 409 Conflict")
      void shouldMapCatalogConflictExceptionToConflict() throws Exception {
        // GIVEN
        BorrowBookRequest request = createValidMockRequest();
        String errorMessage = "The requested book is out of stock";

        given(loanService.borrowBook(any(BorrowBookRequest.class)))
          .willThrow(new CatalogConflictException(errorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/borrow")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
      }
    }
  }

  @Nested
  @DisplayName("Method: returnBook()")
  class ReturnBookTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPathsTests {

      @Test
      @DisplayName("Should return 200 OK and serialize the updated returned loan response body")
      void shouldSuccessfullyReturnBookAndSerializeBody() throws Exception {
        // GIVEN
        Long loanId = 1L;

        // Creating a custom response inline to reflect the RETURNED status
        LoanResponse mockReturnedResponse = new LoanResponse(
          loanId,
          "978-0134685991",
          "user-12345",
          LocalDate.of(2026, 8, 7),
          LocalDate.of(2026, 8, 21),
          LoanStatus.RETURNED, // Status updated
          Instant.parse("2026-08-07T21:25:15Z"),
          Instant.parse("2026-08-11T10:00:00Z")
        );

        given(loanService.returnBook(loanId)).willReturn(mockReturnedResponse);

        // WHEN & THEN
        mockMvc.perform(put(BASE_URL + "/{id}/return", loanId)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.bookIsbn").value("978-0134685991"))
          .andExpect(jsonPath("$.memberId").value("user-12345"))
          .andExpect(jsonPath("$.status").value("RETURNED"))
          .andExpect(jsonPath("$.createdAt").exists())
          .andExpect(jsonPath("$.updatedAt").exists());

        then(loanService).should().returnBook(loanId);
      }
    }

    @Nested
    @DisplayName("Exception Mapping & Validation Scenarios")
    class ExceptionMappingTests {

      @Test
      @DisplayName("Should map ResourceNotFoundException to 404 Not Found")
      void shouldMapResourceNotFoundExceptionToNotFound() throws Exception {
        // GIVEN
        Long loanId = 99L;
        given(loanService.returnBook(loanId))
          .willThrow(new ResourceNotFoundException("This loan doesn't exist"));

        // WHEN & THEN
        mockMvc.perform(put(BASE_URL + "/{id}/return", loanId)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
      }

      @Test
      @DisplayName("Should map IllegalArgumentException to 400 Bad Request")
      void shouldMapIllegalArgumentExceptionToBadRequest() throws Exception {
        // GIVEN
        Long loanId = 1L;
        given(loanService.returnBook(loanId))
          .willThrow(new IllegalArgumentException("This loan has already been closed"));

        // WHEN & THEN
        mockMvc.perform(put(BASE_URL + "/{id}/return", loanId)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
      }

      @Test
      @DisplayName("Should return 400 Bad Request when ID path variable is not a number (Type Mismatch)")
      void shouldReturnBadRequestWhenIdIsTypeMismatch() throws Exception {
        // GIVEN an invalid string ID in the URL path instead of a Long
        String invalidId = "abc";

        // WHEN & THEN
        mockMvc.perform(put(BASE_URL + "/{id}/return", invalidId)
            .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

        // Verify the service was never called because Spring MVC blocked it at the web layer
        then(loanService).shouldHaveNoInteractions();
      }
    }
  }
}
