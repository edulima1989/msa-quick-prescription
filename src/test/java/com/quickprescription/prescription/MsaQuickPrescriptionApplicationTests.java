package com.quickprescription.prescription;

import com.quickprescription.prescription.mapper.CatalogMapper;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MsaQuickPrescriptionApplicationTests {

  @Autowired
  private CatalogMapper mapper;

  @Test
  void contextLoads() {
  }

  @Test
  void mapsCatalogEntitiesAndPagination() {
    var cie = mapper.toResponse(new Cie10("J00", "Rinofaringitis aguda", "Respiratorio"));
    assertEquals("J00", cie.codigo());
    assertEquals("Respiratorio", cie.categoria());

    var medicamento = mapper.toResponse(new Vademecum("Paracetamol", "500 mg", "Analgésico",
        "Tabletas", "Cada 8 horas", "Laboratorio A", "Hipersensibilidad"));
    assertEquals("Paracetamol", medicamento.nombre());
    assertEquals("Laboratorio A", medicamento.casaComercial());
    assertEquals("Hipersensibilidad", medicamento.contraindicaciones());

    var page = mapper.toPage(new PageImpl<>(List.of(cie), PageRequest.of(1, 1), 3));
    assertEquals(2, page.page());
    assertEquals(1, page.size());
    assertEquals(3, page.totalElements());
    assertEquals(3, page.totalPages());
    assertEquals(List.of(cie), page.content());
  }
}
