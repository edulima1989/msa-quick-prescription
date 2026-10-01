package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filtros de los listados (D8): los que llegan se combinan con AND.
 * q: texto contenido, sin distinguir mayúsculas. categoria y casaComercial: coincidencia exacta,
 * sin distinguir mayúsculas (docs/openapi.yaml).
 */
public final class CatalogFilters {
  private CatalogFilters() {}

  public static Specification<Cie10> cie10(String q, String categoria) {
    return (root, query, cb) -> {
      List<Predicate> and = new ArrayList<>();
      if (q != null) and.add(cb.or(contains(cb, root.get("codigo"), q), contains(cb, root.get("descripcion"), q)));
      if (categoria != null) and.add(equalsIgnoreCase(cb, root.get("categoria"), categoria));
      return cb.and(and.toArray(Predicate[]::new));
    };
  }

  public static Specification<Vademecum> vademecum(String q, String casaComercial) {
    return (root, query, cb) -> {
      List<Predicate> and = new ArrayList<>();
      if (q != null) and.add(cb.or(contains(cb, root.get("nombre"), q), contains(cb, root.get("composicion"), q)));
      if (casaComercial != null) and.add(equalsIgnoreCase(cb, root.get("casaComercial"), casaComercial));
      return cb.and(and.toArray(Predicate[]::new));
    };
  }

  private static Predicate contains(CriteriaBuilder cb, Path<String> field, String value) {
    return cb.like(cb.lower(field), "%" + value.toLowerCase(Locale.ROOT) + "%");
  }
  private static Predicate equalsIgnoreCase(CriteriaBuilder cb, Path<String> field, String value) {
    return cb.equal(cb.lower(field), value.toLowerCase(Locale.ROOT));
  }
}
