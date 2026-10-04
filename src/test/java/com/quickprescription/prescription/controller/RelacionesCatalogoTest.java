package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.service.CatalogService;
import com.quickprescription.prescription.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Casos felices de los endpoints de relación del catálogo: el controlador delega en el servicio
 * y entrega la página con Cache-Control: max-age=300 (5 min). La lógica del servicio ya se
 * cubre en CatalogServiceTest; aquí solo se ejercita el controlador.
 */
@WebMvcTest(controllers = {RecipeController.class, CatalogController.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class RelacionesCatalogoTest {
  @Autowired MockMvc mvc;
  @MockitoBean CatalogService catalogService;
  @MockitoBean RecipeService recipeService;

  @Test
  void vademecumRelacionadosConUnCie10() throws Exception {
    VademecumResponse vade = new VademecumResponse(1, "Amoxicilina", "Amoxicilina 500 mg",
        "Antibiótico", "Cápsulas", "500 mg", "Lab", "Alergia");
    when(catalogService.relatedVademecum("J02.9", 1, 20, "nombre,asc"))
        .thenReturn(new PageResponse<>(List.of(vade), 1, 20, 1, 1));

    mvc.perform(get("/api/v1/cie10/{codigo}/vademecum", "J02.9")
            .param("page", "1").param("size", "20").param("sort", "nombre,asc"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("max-age=300")))
        .andExpect(jsonPath("$.content[0].nombre").value("Amoxicilina"))
        .andExpect(jsonPath("$.totalElements").value(1));
    verify(catalogService).relatedVademecum("J02.9", 1, 20, "nombre,asc");
  }

  @Test
  void vademecumRelacionadosUsaValoresPorDefecto() throws Exception {
    when(catalogService.relatedVademecum("J00", 1, 20, "nombre,asc"))
        .thenReturn(new PageResponse<>(List.of(), 1, 20, 0, 0));

    mvc.perform(get("/api/v1/cie10/{codigo}/vademecum", "J00"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("max-age=300")))
        .andExpect(jsonPath("$.totalElements").value(0));
    verify(catalogService).relatedVademecum("J00", 1, 20, "nombre,asc");
  }

  @Test
  void cie10RelacionadosConUnVademecum() throws Exception {
    Cie10Response cie = new Cie10Response(1, "J02.9", "Faringitis aguda", "Respiratorio");
    when(catalogService.relatedCie(1, 1, 20, "codigo,asc"))
        .thenReturn(new PageResponse<>(List.of(cie), 1, 20, 1, 1));

    mvc.perform(get("/api/v1/vademecum/{id}/cie10", 1)
            .param("page", "1").param("size", "20").param("sort", "codigo,asc"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("max-age=300")))
        .andExpect(jsonPath("$.content[0].codigo").value("J02.9"))
        .andExpect(jsonPath("$.totalElements").value(1));
    verify(catalogService).relatedCie(1, 1, 20, "codigo,asc");
  }

  @Test
  void cie10RelacionadosUsaValoresPorDefecto() throws Exception {
    when(catalogService.relatedCie(5, 1, 20, "codigo,asc"))
        .thenReturn(new PageResponse<>(List.of(), 1, 20, 0, 0));

    mvc.perform(get("/api/v1/vademecum/{id}/cie10", 5))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("max-age=300")))
        .andExpect(jsonPath("$.totalElements").value(0));
    verify(catalogService).relatedCie(5, 1, 20, "codigo,asc");
  }
}
