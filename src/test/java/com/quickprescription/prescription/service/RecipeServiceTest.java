package com.quickprescription.prescription.service;

import com.quickprescription.prescription.config.ApiException;
import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import com.quickprescription.prescription.repository.Cie10Repository;
import com.quickprescription.prescription.repository.VademecumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
  @Mock Cie10Repository cie10s;
  @Mock VademecumRepository vademecums;
  @Mock RecetaPdfGenerator pdf;

  private RecipeService service() {
    return new RecipeService(cie10s, vademecums, pdf);
  }

  private static Medicamento med(int id) {
    return new Medicamento(id, "500 mg", "Cada 8 horas", "7 días", null);
  }

  private static RecetaRequest request(List<String> codigos, List<Medicamento> meds) {
    return new RecetaRequest(new Paciente("Ana", 30), new Profesional("Dr. X"),
        LocalDate.of(2026, 10, 3), codigos, meds);
  }

  @Test
  void generaPdfCuandoTodoExiste() {
    when(cie10s.findByCodigo("J02.9")).thenReturn(Optional.of(new Cie10("J02.9", "Faringitis", null)));
    when(vademecums.findById(1)).thenReturn(Optional.of(
        new Vademecum("Amoxicilina", null, null, null, null, null, null)));
    when(pdf.generar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
        org.mockito.ArgumentMatchers.anyList())).thenReturn(new byte[] {1, 2, 3});

    byte[] out = service().generate(request(List.of("J02.9"), List.of(med(1))));
    assertThat(out).containsExactly(1, 2, 3);
  }

  @Test
  void rechazaCodigosCie10Repetidos() {
    RecetaRequest r = request(List.of("J02.9", "J02.9"), List.of(med(1)));
    assertThatThrownBy(() -> service().generate(r))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(400));
  }

  @Test
  void rechazaCie10InexistenteCon422() {
    when(cie10s.findByCodigo("X99")).thenReturn(Optional.empty());
    RecetaRequest r = request(List.of("X99"), List.of(med(1)));
    assertThatThrownBy(() -> service().generate(r))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(422));
  }

  @Test
  void rechazaVademecumInexistenteCon422() {
    when(cie10s.findByCodigo("J02.9")).thenReturn(Optional.of(new Cie10("J02.9", "Faringitis", null)));
    lenient().when(vademecums.findById(99)).thenReturn(Optional.empty());
    RecetaRequest r = request(List.of("J02.9"), List.of(med(99)));
    assertThatThrownBy(() -> service().generate(r))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(422));
  }
}
