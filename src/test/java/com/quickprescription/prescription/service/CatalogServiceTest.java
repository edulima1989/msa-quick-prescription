package com.quickprescription.prescription.service;

import com.quickprescription.prescription.config.ApiException;
import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.mapper.CatalogMapper;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import com.quickprescription.prescription.repository.Cie10Repository;
import com.quickprescription.prescription.repository.VademecumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {
  @Mock Cie10Repository cie10s;
  @Mock VademecumRepository vademecums;
  @Mock CatalogMapper mapper;
  @InjectMocks CatalogService service;

  private Cie10 cie;
  private Vademecum vade;
  private Cie10Response cieResp;
  private VademecumResponse vadeResp;

  @BeforeEach
  void setUp() {
    cie = new Cie10("J02.9", "Faringitis aguda", "Respiratorio");
    vade = new Vademecum("Amoxicilina", "Amoxicilina 500 mg", "Antibiótico", "Cápsulas", "500 mg", "Lab", "Alergia");
    cieResp = new Cie10Response(1, "J02.9", "Faringitis aguda", "Respiratorio");
    vadeResp = new VademecumResponse(1, "Amoxicilina", "Amoxicilina 500 mg", "Antibiótico",
        "Cápsulas", "500 mg", "Lab", "Alergia");
    lenient().when(mapper.toResponse(any(Cie10.class))).thenReturn(cieResp);
    lenient().when(mapper.toResponse(any(Vademecum.class))).thenReturn(vadeResp);
    // toPage es default: delega en la implementación real sobre el Page recibido.
    lenient().when(mapper.toPage(any())).thenAnswer(inv -> {
      Page<?> page = inv.getArgument(0);
      return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
          page.getTotalElements(), page.getTotalPages());
    });
  }

  @Test
  void listCieDevuelvePaginaMapeada() {
    when(cie10s.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(cie)));
    PageResponse<Cie10Response> r = service.listCie(1, 20, "codigo,asc", "far", null);
    assertThat(r.content()).containsExactly(cieResp);
    assertThat(r.page()).isEqualTo(1);
  }

  @Test
  void getCieExistente() {
    when(cie10s.findByCodigo("J02.9")).thenReturn(Optional.of(cie));
    assertThat(service.getCie("J02.9")).isEqualTo(cieResp);
  }

  @Test
  void getCieInexistenteLanza404() {
    when(cie10s.findByCodigo("X99")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getCie("X99"))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(404));
  }

  @Test
  void relatedVademecumValidaDiagnosticoYDevuelve() {
    when(cie10s.findByCodigo("J02.9")).thenReturn(Optional.of(cie));
    when(vademecums.findByCie10Codigo(org.mockito.ArgumentMatchers.eq("J02.9"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(vade)));
    PageResponse<VademecumResponse> r = service.relatedVademecum("J02.9", 1, 20, "nombre,asc");
    assertThat(r.content()).containsExactly(vadeResp);
  }

  @Test
  void relatedVademecumConDiagnosticoInexistenteLanza404() {
    when(cie10s.findByCodigo("X99")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.relatedVademecum("X99", 1, 20, "nombre,asc"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void listVadeDevuelvePaginaMapeada() {
    when(vademecums.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(vade)));
    PageResponse<VademecumResponse> r = service.listVade(1, 20, "nombre,asc", "amox", null);
    assertThat(r.content()).containsExactly(vadeResp);
  }

  @Test
  void getVadeExistente() {
    when(vademecums.findById(1)).thenReturn(Optional.of(vade));
    assertThat(service.getVade(1)).isEqualTo(vadeResp);
  }

  @Test
  void getVadeInexistenteLanza404() {
    when(vademecums.findById(99)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getVade(99))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(404));
  }

  @Test
  void relatedCieValidaVademecumYDevuelve() {
    when(vademecums.findById(1)).thenReturn(Optional.of(vade));
    when(cie10s.findByVademecumId(org.mockito.ArgumentMatchers.eq(1), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(cie)));
    PageResponse<Cie10Response> r = service.relatedCie(1, 1, 20, "codigo,asc");
    assertThat(r.content()).containsExactly(cieResp);
  }

  @Test
  void relatedCieConVademecumInexistenteLanza404() {
    when(vademecums.findById(99)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.relatedCie(99, 1, 20, "codigo,asc"))
        .isInstanceOf(ApiException.class);
  }
}
