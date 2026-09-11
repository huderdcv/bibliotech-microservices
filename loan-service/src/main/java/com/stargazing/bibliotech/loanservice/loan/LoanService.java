package com.stargazing.bibliotech.loanservice.loan;

import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;

public interface LoanService {

  LoanResponse borrowBook(BorrowBookRequest request);

  LoanResponse returnBook(Long loanId);

}
