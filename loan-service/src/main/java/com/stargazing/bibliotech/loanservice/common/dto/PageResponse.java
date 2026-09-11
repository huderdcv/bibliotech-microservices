package com.stargazing.bibliotech.loanservice.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generic paginated response wrapper containing the data list and pagination metadata")
public record PageResponse<T>(
  @Schema(description = "List of items for the current page")
  List<T> content,

  @Schema(description = "Current page number (0-indexed)", example = "0")
  int page,

  @Schema(description = "Number of records requested per page", example = "10")
  int size,

  @Schema(description = "Total number of elements across all pages in the database", example = "120")
  long totalElements,

  @Schema(description = "Total number of pages available", example = "12")
  int totalPages,

  @Schema(description = "Indicates if the current page is the first page", example = "true")
  boolean first,

  @Schema(description = "Indicates if the current page is the last page", example = "false")
  boolean last
) {
  public PageResponse(Page<T> pageModel) {
    this(
      pageModel.getContent(),
      pageModel.getNumber(),
      pageModel.getSize(),
      pageModel.getTotalElements(),
      pageModel.getTotalPages(),
      pageModel.isFirst(),
      pageModel.isLast()
    );
  }
}
