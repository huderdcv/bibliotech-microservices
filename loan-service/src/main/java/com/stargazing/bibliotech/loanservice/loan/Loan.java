package com.stargazing.bibliotech.loanservice.loan;

import com.stargazing.bibliotech.loanservice.common.entity.BaseEntity;
import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Loan extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "book_isbn", nullable = false)
  private String bookIsbn;

  @Column(name = "member_id", nullable = false)
  private String memberId;

  @Column(name = "loan_date", nullable = false)
  private LocalDate loanDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private LoanStatus status;
}
