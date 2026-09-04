package com.stargazing.bibliotech.loanservice.loan.mapper;

import com.stargazing.bibliotech.loanservice.loan.Loan;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanMapper {

  // 1. Entity to LoanResponse
  LoanResponse toResponse(Loan entity);
}
