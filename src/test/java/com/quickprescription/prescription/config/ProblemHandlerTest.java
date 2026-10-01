package com.quickprescription.prescription.config;

import com.quickprescription.prescription.controller.CatalogController;
import com.quickprescription.prescription.controller.RecipeController;
import com.quickprescription.prescription.service.CatalogService;
import com.quickprescription.prescription.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RecipeController.class, CatalogController.class})
// Sin volcado de MockMvc: imprime el cuerpo de la solicitud y ensuciaría la revisión de logs.
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ExtendWith(OutputCaptureExtension.class)
class ProblemHandlerTest {
  private static final String RECETAS = "/api/v1/recetas";
  private static final String VALIDA = """
      {"paciente":{"nombre":"María Fernanda Torres","edad":34},
       "profesional":{"nombre":"Dr. Juan Pérez"},"fecha":"2025-03-14",
       "codigosCie10":["J00"],
       "medicamentos":[{"vademecumId":1,"dosis":"500 mg","frecuencia":"cada 8 h","duracion":"5 días"}]}
      """;

  @Autowired MockMvc mvc;
  @MockitoBean RecipeService recipeService;
  @MockitoBean CatalogService catalogService;

  @Test
  void jsonMalFormado() throws Exception {
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).content("{\"paciente\": "), 400);
  }

  @Test
  void tipoInvalidoEnElCuerpoNoFiltraValoresNiAlLogNiALaRespuesta(CapturedOutput out) throws Exception {
    String body = VALIDA.replace("\"edad\":34", "\"edad\":\"abc\"");
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).content(body), 400)
        .andExpect(content().string(not(containsString("abc"))))
        .andExpect(content().string(not(containsString("María"))));
    assertThat(out.getAll()).doesNotContain("abc", "María", "Fernanda", "2025-03-14");
  }

  @Test
  void fechaConFormatoInvalido(CapturedOutput out) throws Exception {
    String body = VALIDA.replace("2025-03-14", "31-02-2026");
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).content(body), 400)
        .andExpect(content().string(not(containsString("31-02-2026"))));
    assertThat(out.getAll()).doesNotContain("31-02-2026", "María");
  }

  @Test
  void cuerpoAusente() throws Exception {
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON), 400)
        .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud es obligatorio."));
  }

  @Test
  void validacionDeBeanConErrores(CapturedOutput out) throws Exception {
    String body = VALIDA.replace("\"edad\":34", "\"edad\":150");
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).content(body), 400)
        .andExpect(jsonPath("$.errores[0].campo").value("paciente.edad"))
        .andExpect(content().string(not(containsString("150"))));
    assertThat(out.getAll()).doesNotContain("María", "150");
  }

  @Test
  void contentTypeNoSoportado() throws Exception {
    problem(post(RECETAS).contentType(MediaType.TEXT_PLAIN).content("hola"), 415);
  }

  @Test
  void acceptIncompatible() throws Exception {
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .content(VALIDA), 406);
  }

  @Test
  void parametroConTipoInvalido() throws Exception {
    problem(get("/api/v1/cie10").param("page", "abc"), 400)
        .andExpect(jsonPath("$.detail").value("El parámetro 'page' tiene un tipo inválido."));
  }

  @Test
  void noEncontrado() throws Exception {
    when(catalogService.getCie("J99")).thenThrow(new ApiException(404, "CIE-10 no encontrado"));
    problem(get("/api/v1/cie10/J99"), 404).andExpect(jsonPath("$.title").value("Recurso no encontrado"));
  }

  @Test
  void entidadNoProcesableAunqueElClientePidaPdf() throws Exception {
    when(recipeService.generate(any())).thenThrow(new ApiException(422, "Uno o más vademecumId no existen"));
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PDF)
        .content(VALIDA), 422).andExpect(jsonPath("$.title").value("Entidad no procesable"));
  }

  @Test
  void errorInesperadoNoExponeDetalles(CapturedOutput out) throws Exception {
    when(recipeService.generate(any())).thenThrow(new IllegalStateException("detalle interno SQL María"));
    problem(post(RECETAS).contentType(MediaType.APPLICATION_JSON).content(VALIDA), 500)
        .andExpect(jsonPath("$.title").value("Error interno"))
        .andExpect(jsonPath("$.detail").value(ProblemHandler.INTERNAL_DETAIL))
        .andExpect(content().string(not(containsString("SQL"))));
    assertThat(out.getAll()).contains("IllegalStateException").doesNotContain("SQL", "María");
  }

  private ResultActions problem(MockHttpServletRequestBuilder request, int status) throws Exception {
    return mvc.perform(request)
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.title").isNotEmpty());
  }
}
