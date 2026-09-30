package com.quickprescription.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class ApiDtos {
  private ApiDtos() {}
  public record Cie10Response(Integer id, String codigo, String descripcion, String categoria) {}
  public record VademecumResponse(Integer id, String nombre, String composicion, String funcion,
      String presentacion, String dosificacion, String casaComercial, String contraindicaciones) {}
  public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
  public record Paciente(@NotBlank @Size(min=2,max=150) String nombre,
      @NotNull @Min(0) @Max(130) Integer edad) {}
  public record Profesional(@NotBlank @Size(min=2,max=150) String nombre) {}
  public record Medicamento(@NotNull Integer vademecumId,
      @NotBlank @Size(max=100) String dosis, @NotBlank @Size(max=100) String frecuencia,
      @NotBlank @Size(max=100) String duracion, @Size(max=500) String indicaciones) {}
  public record RecetaRequest(@Valid @NotNull Paciente paciente,
      @Valid @NotNull Profesional profesional, @NotNull LocalDate fecha,
      @NotEmpty @Size(max=10) List<@Pattern(regexp="^[A-Z][0-9]{2}(\\.[0-9A-Z]{1,4})?$") String> codigosCie10,
      @NotEmpty @Size(max=10) List<@Valid Medicamento> medicamentos) {}
}
