package com.stargazing.bibliotech.loanservice.loan.impl;

import com.stargazing.bibliotech.loanservice.client.catalog.CatalogClient;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogClientException;
import com.stargazing.bibliotech.loanservice.common.exception.ResourceNotFoundException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.ServiceUnavailableException;
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

    } catch (CatalogClientException e) {
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

  //-- RETURN A BOOK
  @Override
  public LoanResponse returnBook(Long loanId) {
    log.debug("Initiating return process for loan ID: {}", loanId);

    // 1. Validations
    Loan loan = loanRepository.findById(loanId)
      .orElseThrow(() -> new ResourceNotFoundException("This loan doesn't exist"));

    if (loan.getStatus() == LoanStatus.RETURNED) {
      throw new IllegalArgumentException("This loan has already been closed");
    }

    if (loan.getStatus() != LoanStatus.ACTIVE) {
      throw new IllegalArgumentException("This loan is in conflict. Please contact the admin");
    }

    // 2. Synchronous inter-service call outside an open DB transaction
    try {
      catalogClient.returnOne(loan.getBookIsbn());
    } catch (CatalogClientException e) {
      // 3a. Catch ALL domain rejections from the Catalog Service dynamically
      log.warn("Catalog rejected return for ISBN: {}. Reason: {}", loan.getBookIsbn(), e.getMessage());
      throw e;

    } catch (feign.FeignException e) {
      // 3b. Handle unexpected infrastructure/network failures
      throw new ServiceUnavailableException("The Catalog Service is currently unavailable. Please try again later.", e);
    }

    loan.setStatus(LoanStatus.RETURNED);
    Loan savedLoan = loanRepository.save(loan);

    return loanMapper.toResponse(savedLoan);
  }
}
