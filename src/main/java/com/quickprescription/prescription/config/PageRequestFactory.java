package com.quickprescription.prescription.config;

import org.springframework.data.domain.*;
import java.util.Set;

public final class PageRequestFactory {
  private PageRequestFactory() {}
  public static Pageable create(int page, int size, String sort, Set<String> fields) {
    if (page < 1 || size < 1 || size > 100) throw new ApiException(400, "page debe ser >= 1 y size debe estar entre 1 y 100");
    String[] parts = sort.split(",", -1);
    if (parts.length != 2 || !fields.contains(parts[0]) ||
        (!parts[1].equals("asc") && !parts[1].equals("desc"))) {
      throw new ApiException(400, "sort no es válido");
    }
    Sort.Direction direction = Sort.Direction.fromString(parts[1]);
    return PageRequest.of(page - 1, size, Sort.by(direction, parts[0]));
  }
}
