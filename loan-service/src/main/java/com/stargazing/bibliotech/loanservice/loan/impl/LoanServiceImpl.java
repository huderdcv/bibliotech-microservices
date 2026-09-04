package com.stargazing.bibliotech.loanservice.loan.impl;

import com.stargazing.bibliotech.loanservice.client.catalog.CatalogClient;
import com.stargazing.bibliotech.loanservice.common.exception.BookUnavailableException;
import com.stargazing.bibliotech.loanservice.common.exception.ServiceUnavailableException;
import com.stargazing.bibliotech.loanservice.loan.Loan;
import com.stargazing.bibliotech.loanservice.loan.LoanRepository;
import com.stargazing.bibliotech.loanservice.loan.LoanService;
import com.stargazing.bibliotech.loanservice.loan.config.LoanProperties;
import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import com.stargazing.bibliotech.loanservice.loan.mapper.LoanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {
  //- DEPENDENCY INJECTIONS
  private final LoanRepository loanRepository;

  private final CatalogClient catalogClient;
  private final LoanMapper loanMapper;
  private final Clock clock;
  private final LoanProperties loanProperties;

  //- METHODS

  //-- BORROW A BOOK
  @Override
  public LoanResponse borrowBook(BorrowBookRequest request) {
    log.debug("Initiating loan process for ISBN: {} by member: {}", request.bookIsbn(), request.memberId());

    // 1. Save initially as PENDING (Spring Data JPA save() is implicitly transactional)
    Loan pendingLoan = Loan.builder()
      .bookIsbn(request.bookIsbn())
      .memberId(request.memberId())
      .loanDate(LocalDate.now(clock))
      .dueDate(LocalDate.now(clock).plusDays(loanProperties.durationDays()))
      .status(LoanStatus.PENDING)
      .build();

    Loan savedLoan = loanRepository.save(pendingLoan);

    // 2. Synchronous inter-service call outside of an open DB transaction
    try {
      // Note: An ErrorDecoder handles infrastructure errors inside CatalogClient
      catalogClient.reserveOne(request.bookIsbn());

    } catch (BookUnavailableException e) {
      // 3a. Handle domain rejection gracefully
      log.warn("Book ISBN: {} is unavailable. Marking loan as FAILED.", request.bookIsbn());
      savedLoan.setStatus(LoanStatus.FAILED);
      loanRepository.save(savedLoan);
      throw e;

    } catch (feign.FeignException e) {
      // 3b. Handle unexpected infrastructure/network failures
      savedLoan.setStatus(LoanStatus.FAILED);
      loanRepository.save(savedLoan);
      throw new ServiceUnavailableException("The Catalog Service is currently unavailable. Please try again later.", e);

    }

    // 4. Update to ACTIVE upon success
    savedLoan.setStatus(LoanStatus.ACTIVE);
    savedLoan = loanRepository.save(savedLoan);

    log.info("Successfully activated loan record with ID: {}", savedLoan.getId());
    return loanMapper.toResponse(savedLoan);

  }
}
