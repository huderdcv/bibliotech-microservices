package com.stargazing.bibliotech.catalogservice.book;

import com.stargazing.bibliotech.catalogservice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Book extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String isbn;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Builder.Default
  @Column(name = "total_copies")
  private Integer totalCopies = 0;

  @Builder.Default
  @Column(name = "available_copies")
  private Integer availableCopies = 0;

}