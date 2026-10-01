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

/** D8: los filtros se combinan con AND; categoria y casaComercial son exactos sin distinguir mayúsculas. */
@DataJpaTest
class CatalogFiltersTest {
  @Autowired Cie10Repository cie10s;
  @Autowired VademecumRepository vademecums;

  @BeforeEach
  void datos() {
    cie10s.save(new Cie10("J00", "Rinofaringitis aguda", "Enfermedades del sistema respiratorio"));
    cie10s.save(new Cie10("J02.9", "Faringitis aguda", "Enfermedades del sistema respiratorio"));
    cie10s.save(new Cie10("K29", "Gastritis aguda", "Enfermedades del sistema digestivo"));
    vademecums.save(new Vademecum("Paracetamol", "Paracetamol 500 mg", null, null, null, "Lab Uno", null));
    vademecums.save(new Vademecum("Paracetamol Forte", "Paracetamol 1 g", null, null, null, "Lab Dos", null));
    vademecums.save(new Vademecum("Ibuprofeno", "Ibuprofeno 400 mg", null, null, null, "Lab Uno", null));
  }

  @Test
  void cie10CombinaQYCategoria() {
    assertThat(cie10(null, null)).containsExactly("J00", "J02.9", "K29");
    assertThat(cie10("aguda", null)).containsExactly("J00", "J02.9", "K29");
    assertThat(cie10("AGUDA", "enfermedades del sistema RESPIRATORIO")).containsExactly("J00", "J02.9");
    assertThat(cie10("gastr", "Enfermedades del sistema respiratorio")).isEmpty();
    assertThat(cie10("j02", null)).containsExactly("J02.9");
  }

  @Test
  void categoriaEsCoincidenciaExacta() {
    assertThat(cie10(null, "respiratorio")).isEmpty();
    assertThat(cie10(null, "Enfermedades del sistema digestivo")).containsExactly("K29");
  }

  @Test
  void vademecumCombinaQYCasaComercial() {
    assertThat(vademecum("paracetamol", null)).containsExactly("Paracetamol", "Paracetamol Forte");
    assertThat(vademecum("paracetamol", "LAB UNO")).containsExactly("Paracetamol");
    assertThat(vademecum("400 mg", "lab uno")).containsExactly("Ibuprofeno");
    assertThat(vademecum(null, "Lab")).isEmpty();
  }

  private java.util.List<String> cie10(String q, String categoria) {
    return cie10s.findAll(CatalogFilters.cie10(q, categoria), PageRequest.of(0, 20, Sort.by("codigo")))
        .map(Cie10::getCodigo).getContent();
  }
  private java.util.List<String> vademecum(String q, String casa) {
    return vademecums.findAll(CatalogFilters.vademecum(q, casa), PageRequest.of(0, 20, Sort.by("nombre")))
        .map(Vademecum::getNombre).getContent();
  }
}
