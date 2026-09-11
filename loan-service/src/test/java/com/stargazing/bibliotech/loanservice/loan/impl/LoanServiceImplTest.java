package com.stargazing.bibliotech.loanservice.loan.impl;

import com.stargazing.bibliotech.loanservice.client.catalog.CatalogClient;
import com.stargazing.bibliotech.loanservice.client.catalog.dto.BookResponse;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogBadRequestException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogClientException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogConflictException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.ServiceUnavailableException;
import com.stargazing.bibliotech.loanservice.common.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

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
      @DisplayName("Should mark loan as FAILED and bubble up CatalogBadRequestException when Catalog rejects")
      void shouldFailAndThrowBookUnavailableExceptionWhenCatalogRejects() {
        // GIVEN
        String isbn = "978-0134685991";
        String memberId = "user-12345";
        BorrowBookRequest request = createMockRequest(isbn, memberId);

        Loan pendingLoan = createMockLoan(1L, isbn, memberId, LoanStatus.PENDING);

        given(loanRepository.save(any(Loan.class))).willReturn(pendingLoan);

        CatalogBadRequestException expectedException = new CatalogBadRequestException("Out of stock");
        given(catalogClient.reserveOne(isbn)).willThrow(expectedException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.borrowBook(request))
          .isInstanceOf(CatalogBadRequestException.class)
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

  @Nested
  @DisplayName("Method: returnBook()")
  class ReturnBookTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should fetch ACTIVE loan, process return in catalog, and update status to RETURNED")
      void shouldSuccessfullyReturnBookAndCloseLoan() {
        // GIVEN
        Long loanId = 1L;
        String isbn = "978-0134685991";
        String memberId = "user-12345";

        Loan activeLoan = createMockLoan(loanId, isbn, memberId, LoanStatus.ACTIVE);
        Loan returnedLoan = createMockLoan(loanId, isbn, memberId, LoanStatus.RETURNED);

        BookResponse mockBookResponse = mock(BookResponse.class);
        LoanResponse expectedResponse = createMockResponse(loanId, isbn, memberId, LoanStatus.RETURNED);

        given(loanRepository.findById(loanId)).willReturn(Optional.of(activeLoan));
        given(catalogClient.returnOne(isbn)).willReturn(mockBookResponse);
        given(loanRepository.save(any(Loan.class))).willReturn(returnedLoan);
        given(loanMapper.toResponse(returnedLoan)).willReturn(expectedResponse);

        // WHEN
        LoanResponse response = loanService.returnBook(loanId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED);

        // Verify interactions and state mutation
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        then(loanRepository).should(times(1)).save(loanCaptor.capture());

        // Ensure the entity passed to save() was mutated to RETURNED
        assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.RETURNED);

        then(catalogClient).should(times(1)).returnOne(isbn);
        then(loanMapper).should(times(1)).toResponse(returnedLoan);
      }
    }

    @Nested
    @DisplayName("Error Paths (Business Rule Validation Failures)")
    class ErrorPaths {

      @Test
      @DisplayName("Should throw ResourceNotFoundException when loan ID does not exist")
      void shouldThrowResourceNotFoundExceptionWhenLoanDoesNotExist() {
        // GIVEN
        Long loanId = 999L;
        given(loanRepository.findById(loanId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.returnBook(loanId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("This loan doesn't exist");

        // Verify fail-fast: Catalog and Mapper are never touched, nothing is saved
        then(catalogClient).shouldHaveNoInteractions();
        then(loanRepository).should(never()).save(any(Loan.class));
        then(loanMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw IllegalArgumentException when loan is already RETURNED (Idempotency)")
      void shouldThrowIllegalArgumentExceptionWhenLoanAlreadyReturned() {
        // GIVEN
        Long loanId = 1L;
        Loan returnedLoan = createMockLoan(loanId, "978-0134685991", "user-12345", LoanStatus.RETURNED);

        given(loanRepository.findById(loanId)).willReturn(Optional.of(returnedLoan));

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.returnBook(loanId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("This loan has already been closed");

        // Verify fail-fast to prevent artificial inventory inflation
        then(catalogClient).shouldHaveNoInteractions();
        then(loanRepository).should(never()).save(any(Loan.class));
      }

      @Test
      @DisplayName("Should throw IllegalArgumentException when loan is PENDING or FAILED")
      void shouldThrowIllegalArgumentExceptionWhenLoanIsInConflictState() {
        // GIVEN
        Long loanId = 1L;
        // Simulating a loan stuck in PENDING
        Loan pendingLoan = createMockLoan(loanId, "978-0134685991", "user-12345", LoanStatus.PENDING);

        given(loanRepository.findById(loanId)).willReturn(Optional.of(pendingLoan));

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.returnBook(loanId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("This loan is in conflict. Please contact the admin");

        then(catalogClient).shouldHaveNoInteractions();
        then(loanRepository).should(never()).save(any(Loan.class));
      }
    }

    @Nested
    @DisplayName("Integration Failures (Transaction Safety)")
    class IntegrationFailures {

      @Test
      @DisplayName("Should bubble up CatalogClientException and NOT save when Catalog rejects return")
      void shouldBubbleUpDomainExceptionAndNotSaveWhenCatalogRejects() {
        // GIVEN
        Long loanId = 1L;
        String isbn = "978-0134685991";
        Loan activeLoan = createMockLoan(loanId, isbn, "user-12345", LoanStatus.ACTIVE);

        given(loanRepository.findById(loanId)).willReturn(Optional.of(activeLoan));

        // Simulating the Catalog Service returning a 409 Conflict
        CatalogConflictException expectedException = new CatalogConflictException("Inventory capacity exceeded");
        given(catalogClient.returnOne(isbn)).willThrow(expectedException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.returnBook(loanId))
          .isInstanceOf(CatalogClientException.class)
          .hasMessage("Inventory capacity exceeded");

        // Crucial Transaction Safety check: Ensure the local DB was never mutated/saved
        then(loanRepository).should(never()).save(any(Loan.class));
        then(loanMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw ServiceUnavailableException and NOT save when infrastructure fails")
      void shouldThrowServiceUnavailableExceptionAndNotSaveWhenNetworkFails() {
        // GIVEN
        Long loanId = 1L;
        String isbn = "978-0134685991";
        Loan activeLoan = createMockLoan(loanId, isbn, "user-12345", LoanStatus.ACTIVE);

        given(loanRepository.findById(loanId)).willReturn(java.util.Optional.of(activeLoan));

        // Simulating a Feign Exception (e.g. 500 Internal Server Error or Connection Timeout)
        FeignException feignException = mock(FeignException.class);
        given(catalogClient.returnOne(isbn)).willThrow(feignException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.returnBook(loanId))
          .isInstanceOf(ServiceUnavailableException.class)
          .hasMessageContaining("The Catalog Service is currently unavailable");

        // Crucial Transaction Safety check: Ensure the local DB was never mutated/saved
        then(loanRepository).should(never()).save(any(Loan.class));
        then(loanMapper).shouldHaveNoInteractions();
      }
    }
  }

  @Nested
  @DisplayName("Method: findAllLoansByMemberId()")
  class FindAllLoansByMemberIdTests {

    @Nested
    @DisplayName("Happy Paths (Success Scenarios)")
    class HappyPaths {

      @Test
      @DisplayName("Should return populated page of loans and verify exact status filtering")
      @SuppressWarnings("unchecked")
      void shouldReturnPopulatedPageAndVerifyStatusSet() {
        // GIVEN
        String memberId = "user-12345";
        Pageable pageable = PageRequest.of(0, 10);

        Loan activeLoan = createMockLoan(1L, "978-0134685991", memberId, LoanStatus.ACTIVE);
        LoanResponse expectedResponse = createMockResponse(1L, "978-0134685991", memberId, LoanStatus.ACTIVE);

        Page<Loan> mockPage = new PageImpl<>(List.of(activeLoan), pageable, 1);

        given(loanRepository.findAllByMemberIdAndStatusIn(
          eq(memberId),
          any(Set.class),
          eq(pageable)
        )).willReturn(mockPage);

        given(loanMapper.toResponse(activeLoan)).willReturn(expectedResponse);

        // WHEN
        Page<LoanResponse> result = loanService.findAllLoansByMemberId(memberId, pageable);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(result.getTotalElements()).isEqualTo(1);

        // Verify the repository was called with the exact expected Set of statuses
        ArgumentCaptor<Set<LoanStatus>> statusesCaptor = ArgumentCaptor.forClass(Set.class);
        then(loanRepository).should(times(1)).findAllByMemberIdAndStatusIn(
          eq(memberId),
          statusesCaptor.capture(),
          eq(pageable)
        );

        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(
          LoanStatus.ACTIVE,
          LoanStatus.RETURNED,
          LoanStatus.OVERDUE
        );

        // Verify mapper was called exactly once per entity
        then(loanMapper).should(times(1)).toResponse(activeLoan);
      }

      @Test
      @DisplayName("Should gracefully return empty page when member has no history")
      void shouldReturnEmptyPageWhenNoHistoryExists() {
        // GIVEN
        String memberId = "user-99999";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Loan> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(loanRepository.findAllByMemberIdAndStatusIn(
          eq(memberId),
          any(Set.class),
          eq(pageable)
        )).willReturn(emptyPage);

        // WHEN
        Page<LoanResponse> result = loanService.findAllLoansByMemberId(memberId, pageable);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        // Verify the mapper was NEVER invoked since there was no data to map
        then(loanMapper).shouldHaveNoInteractions();
      }
    }

    @Nested
    @DisplayName("Error Paths (Integration Failures)")
    class ErrorPaths {

      @Test
      @DisplayName("Should bubble up DataAccessException when database fails")
      void shouldBubbleUpExceptionWhenDatabaseFails() {
        // GIVEN
        String memberId = "user-12345";
        Pageable pageable = PageRequest.of(0, 10);

        // Simulating a database timeout or connection failure
        RuntimeException dbException = new RuntimeException("Database connection timeout");

        given(loanRepository.findAllByMemberIdAndStatusIn(
          eq(memberId),
          any(Set.class),
          eq(pageable)
        )).willThrow(dbException);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.findAllLoansByMemberId(memberId, pageable))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Database connection timeout");

        // Verify mapper was never touched
        then(loanMapper).shouldHaveNoInteractions();
      }
    }
  }
}
