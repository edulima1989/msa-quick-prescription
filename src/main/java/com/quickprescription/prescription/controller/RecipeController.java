package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.dto.ApiDtos.RecetaRequest;
import com.quickprescription.prescription.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recetas")
public class RecipeController {
  private final RecipeService service;
  public RecipeController(RecipeService service) { this.service = service; }
  @PostMapping(produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generate(@Valid @RequestBody RecetaRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("receta.pdf").build().toString())
        .body(service.generate(request));
  }
}
