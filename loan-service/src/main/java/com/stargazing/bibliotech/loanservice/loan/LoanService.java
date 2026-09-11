package com.stargazing.bibliotech.loanservice.loan;

import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanService {

  LoanResponse borrowBook(BorrowBookRequest request);

  LoanResponse returnBook(Long loanId);

  Page<LoanResponse> findAllLoansByMemberId(String memberId, Pageable pageable);

}
