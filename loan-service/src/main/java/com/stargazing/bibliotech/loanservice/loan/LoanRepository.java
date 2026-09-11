package com.stargazing.bibliotech.loanservice.loan;

import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  Page<Loan> findAllByMemberIdAndStatusIn(String memberId, Set<LoanStatus> statuses, Pageable pageable);
}
