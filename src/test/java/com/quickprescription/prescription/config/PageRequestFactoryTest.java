package com.quickprescription.prescription.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestFactoryTest {
  private static final Set<String> CAMPOS = Set.of("codigo", "descripcion");

  @Test
  void creaPageableValido() {
    Pageable p = PageRequestFactory.create(2, 20, "codigo,asc", CAMPOS);
    assertThat(p.getPageNumber()).isEqualTo(1); // base 0 interno (page 2 -> índice 1)
    assertThat(p.getPageSize()).isEqualTo(20);
    assertThat(p.getSort().getOrderFor("codigo")).isNotNull();
    assertThat(p.getSort().getOrderFor("codigo").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void ordenDescendente() {
    Pageable p = PageRequestFactory.create(1, 10, "descripcion,desc", CAMPOS);
    assertThat(p.getSort().getOrderFor("descripcion").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void rechazaPageMenorA1() {
    assertThatThrownBy(() -> PageRequestFactory.create(0, 20, "codigo,asc", CAMPOS))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(400));
  }

  @Test
  void rechazaSizeFueraDeRango() {
    assertThatThrownBy(() -> PageRequestFactory.create(1, 0, "codigo,asc", CAMPOS))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> PageRequestFactory.create(1, 101, "codigo,asc", CAMPOS))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rechazaSortMalFormado() {
    assertThatThrownBy(() -> PageRequestFactory.create(1, 20, "codigo", CAMPOS))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rechazaCampoNoPermitido() {
    assertThatThrownBy(() -> PageRequestFactory.create(1, 20, "otro,asc", CAMPOS))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rechazaDireccionInvalida() {
    assertThatThrownBy(() -> PageRequestFactory.create(1, 20, "codigo,arriba", CAMPOS))
        .isInstanceOf(ApiException.class);
  }
}
