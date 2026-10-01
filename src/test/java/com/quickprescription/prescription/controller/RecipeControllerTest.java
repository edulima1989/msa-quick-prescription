package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import com.quickprescription.prescription.repository.Cie10Repository;
import com.quickprescription.prescription.repository.VademecumRepository;
import com.quickprescription.prescription.service.CatalogService;
import com.quickprescription.prescription.service.RecetaPdfGenerator;
import com.quickprescription.prescription.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Lote P3: POST /recetas con el servicio y el generador reales; solo se simulan los repositorios. */
@WebMvcTest(controllers = {RecipeController.class, CatalogController.class})
@Import({RecipeService.class, RecetaPdfGenerator.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ExtendWith(OutputCaptureExtension.class)
class RecipeControllerTest {
  private static final String VALIDA = """
      {"paciente":{"nombre":"María Fernanda Torres","edad":34},
       "profesional":{"nombre":"Dr. Carlos Andrade"},"fecha":"2026-09-28",
       "codigosCie10":["J00"],
       "medicamentos":[{"vademecumId":1,"dosis":"1 tableta","frecuencia":"Cada 8 horas","duracion":"5 días"}]}
      """;

  @Autowired MockMvc mvc;
  @MockitoBean Cie10Repository cie10s;
  @MockitoBean VademecumRepository vademecums;
  @MockitoBean CatalogService catalogService;
  @MockitoSpyBean RecetaPdfGenerator generator;

  @Test
  void respondePdfConLasCabecerasDelContrato() throws Exception {
    datosDelCatalogo();
    byte[] pdf = mvc.perform(post("/api/v1/recetas").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PDF).content(VALIDA))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"receta.pdf\""))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andReturn().getResponse().getContentAsByteArray();
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void falloAlGenerarResponde500SinDatosPersonales(CapturedOutput out) throws Exception {
    datosDelCatalogo();
    doThrow(new IllegalStateException()).when(generator).generar(any(), any(), any());
    mvc.perform(post("/api/v1/recetas").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PDF).content(VALIDA))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Error interno"))
        .andExpect(content().string(not(containsString("María"))));
    assertThat(out.getAll()).contains("IllegalStateException").doesNotContain("María", "Andrade");
  }

  private void datosDelCatalogo() {
    when(cie10s.findByCodigo("J00")).thenReturn(Optional.of(new Cie10("J00", "Rinofaringitis aguda", null)));
    when(vademecums.findById(1)).thenReturn(Optional.of(new Vademecum("Paracetamol", null, null, null, null, null, null)));
  }
}
