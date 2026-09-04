package com.stargazing.bibliotech.loanservice.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stargazing.bibliotech.loanservice.common.exception.BookUnavailableException;
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

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  private java.time.Clock clock;

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
      @DisplayName("Should map BookUnavailableException to 409 Conflict")
      void shouldMapBookUnavailableExceptionToConflict() throws Exception {
        // GIVEN
        BorrowBookRequest request = createValidMockRequest();
        String errorMessage = "The requested book is out of stock";

        given(loanService.borrowBook(any(BorrowBookRequest.class)))
          .willThrow(new BookUnavailableException(errorMessage));

        // WHEN & THEN
        mockMvc.perform(post(BASE_URL + "/borrow")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
      }
    }
  }
}
