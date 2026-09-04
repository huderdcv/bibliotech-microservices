package com.stargazing.bibliotech.loanservice.loan.impl;

import com.stargazing.bibliotech.loanservice.client.catalog.CatalogClient;
import com.stargazing.bibliotech.loanservice.client.catalog.dto.BookResponse;
import com.stargazing.bibliotech.loanservice.common.exception.BookUnavailableException;
import com.stargazing.bibliotech.loanservice.common.exception.ServiceUnavailableException;
import com.stargazing.bibliotech.loanservice.loan.Loan;
import com.stargazing.bibliotech.loanservice.loan.LoanRepository;
import com.stargazing.bibliotech.loanservice.loan.config.LoanProperties;
import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import com.stargazing.bibliotech.loanservice.loan.mapper.LoanMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

  @Mock
  private LoanRepository loanRepository;

  @Mock
  private CatalogClient catalogClient;

  @Mock
  private LoanMapper loanMapper;

  @Mock
  private Clock clock;

  @Mock
  private LoanProperties loanProperties;

  @InjectMocks
  private LoanServiceImpl loanService;

  private final Instant FIXED_INSTANT = Instant.parse("2026-08-07T10:00:00Z");
  private final ZoneId FIXED_ZONE = ZoneId.of("UTC");

  // ===================================================================================
  //                        HELPERS FOR MOCK DATA
  // ===================================================================================

  private BorrowBookRequest createMockRequest(String isbn, String memberId) {
    return new BorrowBookRequest(isbn, memberId);
  }

  private Loan createMockLoan(Long id, String isbn, String memberId, LoanStatus status) {
    return Loan.builder()
      .id(id)
      .bookIsbn(isbn)
      .memberId(memberId)
      .loanDate(LocalDate.ofInstant(FIXED_INSTANT, FIXED_ZONE))
      .dueDate(LocalDate.ofInstant(FIXED_INSTANT, FIXED_ZONE).plusDays(14))
      .status(status)
      .build();
  }

  private LoanResponse createMockResponse(Long id, String isbn, String memberId, LoanStatus status) {
    return new LoanResponse(
      id,
      isbn,
      memberId,
      LocalDate.ofInstant(FIXED_INSTANT, FIXED_ZONE),
      LocalDate.ofInstant(FIXED_INSTANT, FIXED_ZONE).plusDays(14),
      status,
      Instant.now(),
      Instant.now()
    );
  }

  // ===================================================================================
  //                        NESTED TESTS STRUCTURE
  // ===================================================================================

  @Nested
  @DisplayName("Method: borrowBook()")
  class BorrowBookTests {

    @BeforeEach
    void setUpClockAndProperties() {
      // Mock the Clock so LocalDate.now(clock) returns a predictable date in tests
      given(clock.instant()).willReturn(FIXED_INSTANT);
      given(clock.getZone()).willReturn(FIXED_ZONE);

      // Mock the duration days properties
      given(loanProperties.durationDays()).willReturn(14);
    }

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should save PENDING, reserve in catalog, update to ACTIVE, and return response")
      void shouldSuccessfullyBorrowBookAndActivateLoan() {
        // GIVEN
        String isbn = "978-0134685991";
        String memberId = "user-12345";
        BorrowBookRequest request = createMockRequest(isbn, memberId);

        Loan pendingLoan = createMockLoan(1L, isbn, memberId, LoanStatus.PENDING);
        Loan activeLoan = createMockLoan(1L, isbn, memberId, LoanStatus.ACTIVE);

        BookResponse mockBookResponse = mock(BookResponse.class);
        LoanResponse expectedResponse = createMockResponse(1L, isbn, memberId, LoanStatus.ACTIVE);

        // First save() returns PENDING, second save() returns ACTIVE
        given(loanRepository.save(any(Loan.class))).willReturn(pendingLoan).willReturn(activeLoan);
        given(catalogClient.reserveOne(isbn)).willReturn(mockBookResponse);
        given(loanMapper.toResponse(activeLoan)).willReturn(expectedResponse);

        // WHEN
        LoanResponse response = loanService.borrowBook(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);

        // Crucial Validation: Ensure repository saved exactly twice
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        then(loanRepository).should(times(2)).save(loanCaptor.capture());

        List<Loan> savedLoans = loanCaptor.getAllValues();
        assertThat(savedLoans.get(0).getStatus()).isEqualTo(LoanStatus.PENDING); // 1st state
        assertThat(savedLoans.get(1).getStatus()).isEqualTo(LoanStatus.ACTIVE);  // 2nd state

        then(catalogClient).should(times(1)).reserveOne(isbn);
        then(loanMapper).should(times(1)).toResponse(any(Loan.class));
      }
    }

    @Nested
    @DisplayName("Error Paths (Integration & Business Failures)")
    class ErrorPaths {

      @Test
      @DisplayName("Should mark loan as FAILED and bubble up BookUnavailableException when Catalog rejects")
      void shouldFailAndThrowBookUnavailableExceptionWhenCatalogRejects() {
        // GIVEN
        String isbn = "978-0134685991";
        String memberId = "user-12345";
        BorrowBookRequest request = createMockRequest(isbn, memberId);

        Loan pendingLoan = createMockLoan(1L, isbn, memberId, LoanStatus.PENDING);

        given(loanRepository.save(any(Loan.class))).willReturn(pendingLoan);

        BookUnavailableException expectedException = new BookUnavailableException("Out of stock");
        given(catalogClient.reserveOne(isbn)).willThrow(expectedException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.borrowBook(request))
          .isInstanceOf(BookUnavailableException.class)
          .hasMessage("Out of stock");

        // Verify state machine transitions
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        then(loanRepository).should(times(2)).save(loanCaptor.capture());

        List<Loan> savedLoans = loanCaptor.getAllValues();
        assertThat(savedLoans.get(0).getStatus()).isEqualTo(LoanStatus.PENDING); // Initial save
        assertThat(savedLoans.get(1).getStatus()).isEqualTo(LoanStatus.FAILED);  // Fallback save

        // Ensure mapper was never called
        then(loanMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should mark loan as FAILED and throw ServiceUnavailableException when infrastructure fails")
      void shouldFailAndThrowServiceUnavailableExceptionWhenInfrastructureFails() {
        // GIVEN
        String isbn = "978-0134685991";
        String memberId = "user-12345";
        BorrowBookRequest request = createMockRequest(isbn, memberId);

        Loan pendingLoan = createMockLoan(1L, isbn, memberId, LoanStatus.PENDING);

        given(loanRepository.save(any(Loan.class))).willReturn(pendingLoan);

        // Mock a FeignException to simulate a network timeout or 500 error
        FeignException feignException = mock(FeignException.class);
        given(catalogClient.reserveOne(isbn)).willThrow(feignException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.borrowBook(request))
          .isInstanceOf(ServiceUnavailableException.class)
          .hasMessageContaining("The Catalog Service is currently unavailable");

        // Verify state machine transitions
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        then(loanRepository).should(times(2)).save(loanCaptor.capture());

        List<Loan> savedLoans = loanCaptor.getAllValues();
        assertThat(savedLoans.get(0).getStatus()).isEqualTo(LoanStatus.PENDING); // Initial save
        assertThat(savedLoans.get(1).getStatus()).isEqualTo(LoanStatus.FAILED);  // Fallback save

        // Ensure mapper was never called
        then(loanMapper).shouldHaveNoInteractions();
      }
    }
  }
}
