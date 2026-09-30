package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.service.CatalogService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
  private final CatalogService service;
  public CatalogController(CatalogService service) { this.service = service; }

  @GetMapping("/cie10")
  public ResponseEntity<PageResponse<Cie10Response>> listCie(@RequestParam(defaultValue="1") int page,
      @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="codigo,asc") String sort,
      @RequestParam(required=false) String q, @RequestParam(required=false) String categoria) {
    return cached(service.listCie(page, size, sort, q, categoria));
  }
  @GetMapping("/cie10/{codigo}")
  public Cie10Response getCie(@PathVariable String codigo) { return service.getCie(codigo); }
  @GetMapping("/cie10/{codigo}/vademecum")
  public ResponseEntity<PageResponse<VademecumResponse>> relatedVademecum(@PathVariable String codigo,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size,
      @RequestParam(defaultValue="nombre,asc") String sort) {
    return cached(service.relatedVademecum(codigo, page, size, sort));
  }
  @GetMapping("/vademecum")
  public ResponseEntity<PageResponse<VademecumResponse>> listVade(@RequestParam(defaultValue="1") int page,
      @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="nombre,asc") String sort,
      @RequestParam(required=false) String q, @RequestParam(required=false) String casaComercial) {
    return cached(service.listVade(page, size, sort, q, casaComercial));
  }
  @GetMapping("/vademecum/{id}")
  public VademecumResponse getVade(@PathVariable Integer id) { return service.getVade(id); }
  @GetMapping("/vademecum/{id}/cie10")
  public ResponseEntity<PageResponse<Cie10Response>> relatedCie(@PathVariable Integer id,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size,
      @RequestParam(defaultValue="codigo,asc") String sort) {
    return cached(service.relatedCie(id, page, size, sort));
  }
  private <T> ResponseEntity<T> cached(T body) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5))).body(body);
  }
}
