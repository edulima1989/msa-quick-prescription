package com.quickprescription.prescription.controller;

import com.quickprescription.prescription.service.CatalogService;
import com.quickprescription.prescription.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Lote P2: cada parámetro o cuerpo inválido responde 400 en problem+json sin llegar al servicio. */
@WebMvcTest(controllers = {RecipeController.class, CatalogController.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class ValidacionParametrosTest {
  private static final String LARGO = "a".repeat(101);

  @Autowired MockMvc mvc;
  @MockitoBean RecipeService recipeService;
  @MockitoBean CatalogService catalogService;

  @ParameterizedTest
  @ValueSource(strings = {"page=0", "page=-1", "size=0", "size=101", "sort=nombre,asc", "sort=codigo,ASC",
      "sort=codigo", "sort=codigo,asc&sort=descripcion,desc", "q=a", "q=", "categoria=x", "categoria="})
  void listadoCie10Invalido(String query) throws Exception {
    bad(get("/api/v1/cie10?" + query));
    verifyNoInteractions(catalogService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"page=0", "size=101", "sort=codigo,asc", "sort=casaComercial,up", "q=a", "casaComercial=x"})
  void listadoVademecumInvalido(String query) throws Exception {
    bad(get("/api/v1/vademecum?" + query));
    verifyNoInteractions(catalogService);
  }

  @Test
  void textosDeMasDe100Caracteres() throws Exception {
    bad(get("/api/v1/cie10").param("q", LARGO));
    bad(get("/api/v1/cie10").param("categoria", LARGO));
    bad(get("/api/v1/vademecum").param("casaComercial", LARGO));
    verifyNoInteractions(catalogService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"j00", "XYZ", "J0", "J00.12345", "J00.", "J00-1"})
  void codigoCie10ConFormatoInvalido(String codigo) throws Exception {
    bad(get("/api/v1/cie10/{codigo}", codigo)).andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    bad(get("/api/v1/cie10/{codigo}/vademecum", codigo));
    verifyNoInteractions(catalogService);
  }

  @Test
  void relacionesConPaginacionYOrdenInvalidos() throws Exception {
    bad(get("/api/v1/cie10/J00/vademecum?sort=codigo,asc"));
    bad(get("/api/v1/cie10/J00/vademecum?size=101"));
    bad(get("/api/v1/vademecum/1/cie10?sort=nombre,asc"));
    bad(get("/api/v1/vademecum/1/cie10?page=0"));
    verifyNoInteractions(catalogService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "abc", "1.5", "99999999999"})
  void idDeVademecumInvalido(String id) throws Exception {
    bad(get("/api/v1/vademecum/" + id));
    bad(get("/api/v1/vademecum/" + id + "/cie10"));
    verifyNoInteractions(catalogService);
  }

  @Test
  void valoresValidosLleganAlServicio() throws Exception {
    mvc.perform(get("/api/v1/cie10?page=1&size=100&sort=descripcion,desc&q=ab&categoria=Enfermedades"))
        .andExpect(status().isOk());
    verify(catalogService).listCie(1, 100, "descripcion,desc", "ab", "Enfermedades");
    mvc.perform(get("/api/v1/vademecum?sort=casaComercial,asc&q=pa&casaComercial=Lab")).andExpect(status().isOk());
    verify(catalogService).listVade(1, 20, "casaComercial,asc", "pa", "Lab");
    mvc.perform(get("/api/v1/cie10/J02.9")).andExpect(status().isOk());
    verify(catalogService).getCie("J02.9");
    mvc.perform(get("/api/v1/vademecum/1")).andExpect(status().isOk());
    verify(catalogService).getVade(1);
  }

  @Test
  void elementoNuloEnCodigosCie10() throws Exception {
    badBody(receta("[null]", medicamento(1))).andExpect(jsonPath("$.errores[0].campo").value("codigosCie10[0]"));
  }

  @Test
  void elementoNuloEnMedicamentos() throws Exception {
    badBody(receta("[\"J00\"]", "null")).andExpect(jsonPath("$.errores[0].campo").value("medicamentos[0]"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5})
  void vademecumIdMenorQueUno(int id) throws Exception {
    badBody(receta("[\"J00\"]", medicamento(id)))
        .andExpect(jsonPath("$.errores[0].campo").value("medicamentos[0].vademecumId"));
  }

  @Test
  void medicamentoInvalidoDentroDeLaListaSeValida() throws Exception {
    badBody(receta("[\"J00\"]", "{\"vademecumId\":1,\"dosis\":\"\",\"frecuencia\":\"c/8h\",\"duracion\":\"5 días\"}"))
        .andExpect(jsonPath("$.errores[0].campo").value("medicamentos[0].dosis"));
  }

  private static String medicamento(int id) {
    return "{\"vademecumId\":" + id + ",\"dosis\":\"500 mg\",\"frecuencia\":\"c/8h\",\"duracion\":\"5 días\"}";
  }
  private static String receta(String codigos, String medicamento) {
    return """
        {"paciente":{"nombre":"María Fernanda Torres","edad":34},"profesional":{"nombre":"Dr. Juan Pérez"},
         "fecha":"2025-03-14","codigosCie10":%s,"medicamentos":[%s]}
        """.formatted(codigos, medicamento);
  }
  private ResultActions badBody(String body) throws Exception {
    ResultActions r = bad(post("/api/v1/recetas").contentType(MediaType.APPLICATION_JSON).content(body));
    verifyNoInteractions(recipeService);
    return r;
  }
  private ResultActions bad(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Solicitud inválida"));
  }
}
