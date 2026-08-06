package com.stargazing.bibliotech.catalogservice.book.mapper;

import com.stargazing.bibliotech.catalogservice.book.Book;
import com.stargazing.bibliotech.catalogservice.book.dto.BookResponse;
import com.stargazing.bibliotech.catalogservice.book.dto.CreateBookRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookMapper {

  // 1. DTO to Entity
  @Mapping(target = "isbn", qualifiedByName = "trimString")
  @Mapping(target = "title", qualifiedByName = "trimString")
  @Mapping(target = "author", qualifiedByName = "trimString")
  Book toEntity(CreateBookRequest request);

  // 2. Entity to DTO
  BookResponse toResponse(Book entity);

  // 3. Custom Logic
  @Named("trimString")
  default String trimString(String value) {
    return value != null ? value.trim() : null;
  }
}
