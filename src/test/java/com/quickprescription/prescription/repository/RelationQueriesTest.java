package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas de la relación N:N vademecum–cie10. */
@DataJpaTest
class RelationQueriesTest {
  @Autowired Cie10Repository cie10s;
  @Autowired VademecumRepository vademecums;

  private String codigo;
  private Integer vademecumId;

  @BeforeEach
  void datos() {
    Cie10 j02 = cie10s.save(new Cie10("J02.9", "Faringitis aguda", "Respiratorio"));
    Cie10 k29 = cie10s.save(new Cie10("K29", "Gastritis", "Digestivo"));
    Vademecum amoxi = new Vademecum("Amoxicilina", "Amoxicilina 500 mg", null, null, null, "Lab", null);
    // La entidad dueña de la relación es Vademecum (cie10s).
    amoxi.getCie10s().add(j02);
    amoxi.getCie10s().add(k29);
    Vademecum guardado = vademecums.save(amoxi);
    vademecums.save(new Vademecum("Ibuprofeno", "Ibuprofeno 400 mg", null, null, null, "Lab", null));

    codigo = j02.getCodigo();
    vademecumId = guardado.getId();
  }

  @Test
  void vademecumsRelacionadosConUnCie10() {
    var page = vademecums.findByCie10Codigo(codigo, PageRequest.of(0, 20, Sort.by("nombre")));
    assertThat(page.getContent()).extracting(Vademecum::getNombre).containsExactly("Amoxicilina");
  }

  @Test
  void vademecumSinRelacionDevuelveVacio() {
    var page = vademecums.findByCie10Codigo("Z99", PageRequest.of(0, 20, Sort.by("nombre")));
    assertThat(page.getContent()).isEmpty();
  }

  @Test
  void cie10RelacionadosConUnVademecum() {
    var page = cie10s.findByVademecumId(vademecumId, PageRequest.of(0, 20, Sort.by("codigo")));
    assertThat(page.getContent()).extracting(Cie10::getCodigo).containsExactly("J02.9", "K29");
  }

  @Test
  void findByCodigoDevuelveElDiagnostico() {
    assertThat(cie10s.findByCodigo("J02.9")).isPresent();
    assertThat(cie10s.findByCodigo("NOEXISTE")).isEmpty();
  }
}
