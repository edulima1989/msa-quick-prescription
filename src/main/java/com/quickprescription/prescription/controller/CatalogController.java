package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.service.CatalogService;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
public class CatalogController {
  // Valores permitidos según docs/openapi.yaml (CodigoCie10, SortCie10, SortVademecum).
  private static final String CODIGO_CIE10 = "^[A-Z][0-9]{2}(\\.[0-9A-Z]{1,4})?$";
  private static final String SORT_CIE10 = "^(codigo|descripcion),(asc|desc)$";
  private static final String SORT_VADEMECUM = "^(nombre|casaComercial),(asc|desc)$";

  private final CatalogService service;
  public CatalogController(CatalogService service) { this.service = service; }

  @GetMapping("/cie10")
  public ResponseEntity<PageResponse<Cie10Response>> listCie(
      @RequestParam(defaultValue="1") @Min(1) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue="codigo,asc") @Pattern(regexp=SORT_CIE10) String sort,
      @RequestParam(required=false) @Size(min=2, max=100) String q,
      @RequestParam(required=false) @Size(min=2, max=100) String categoria) {
    return cached(service.listCie(page, size, sort, q, categoria));
  }
  @GetMapping("/cie10/{codigo}")
  public Cie10Response getCie(@PathVariable @Size(min=3, max=8) @Pattern(regexp=CODIGO_CIE10) String codigo) {
    return service.getCie(codigo);
  }
  @GetMapping("/cie10/{codigo}/vademecum")
  public ResponseEntity<PageResponse<VademecumResponse>> relatedVademecum(
      @PathVariable @Size(min=3, max=8) @Pattern(regexp=CODIGO_CIE10) String codigo,
      @RequestParam(defaultValue="1") @Min(1) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue="nombre,asc") @Pattern(regexp=SORT_VADEMECUM) String sort) {
    return cached(service.relatedVademecum(codigo, page, size, sort));
  }
  @GetMapping("/vademecum")
  public ResponseEntity<PageResponse<VademecumResponse>> listVade(
      @RequestParam(defaultValue="1") @Min(1) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue="nombre,asc") @Pattern(regexp=SORT_VADEMECUM) String sort,
      @RequestParam(required=false) @Size(min=2, max=100) String q,
      @RequestParam(required=false) @Size(min=2, max=100) String casaComercial) {
    return cached(service.listVade(page, size, sort, q, casaComercial));
  }
  @GetMapping("/vademecum/{id}")
  public VademecumResponse getVade(@PathVariable @Min(1) Integer id) { return service.getVade(id); }
  @GetMapping("/vademecum/{id}/cie10")
  public ResponseEntity<PageResponse<Cie10Response>> relatedCie(@PathVariable @Min(1) Integer id,
      @RequestParam(defaultValue="1") @Min(1) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue="codigo,asc") @Pattern(regexp=SORT_CIE10) String sort) {
    return cached(service.relatedCie(id, page, size, sort));
  }
  private <T> ResponseEntity<T> cached(T body) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5))).body(body);
  }
}
