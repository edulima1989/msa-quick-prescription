package com.quickprescription.prescription.mapper;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CatalogMapper {
  Cie10Response toResponse(Cie10 entity);
  VademecumResponse toResponse(Vademecum entity);

  default <T> PageResponse<T> toPage(Page<T> page) {
    return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
