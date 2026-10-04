package com.quickprescription.prescription.service;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RecetaPdfGeneratorTest {
  private final RecetaPdfGenerator generator = new RecetaPdfGenerator();

  @Test
  void incluyeTodosLosDatosConTildesYEnes() throws Exception {
    RecetaRequest r = new RecetaRequest(new Paciente("María Fernanda Torres", 34),
        new Profesional("Dra. Ñusta Peña (pediatría)"), LocalDate.of(2026, 9, 28),
        List.of("J00", "J02.9"),
        List.of(new Medicamento(1, "1 tableta de 500 mg", "Cada 8 horas", "5 días", "Tomar después de las comidas."),
            new Medicamento(2, "10 ml", "Cada 12 horas", "3 días", null)));
    byte[] pdf = generator.generar(r,
        List.of(cie("J00", "Rinofaringitis aguda [resfriado común]"), cie("J02.9", "Faringitis aguda, no especificada")),
        List.of(vad("Paracetamol"), vad("Ibuprofeno jarabe")));

    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    String texto = texto(pdf);
    assertThat(texto).contains(
        // Encabezado y plantilla
        "PRESCRIPTION", "Dra. Ñusta Peña (pediatría)", "Rx", "Firma", "RecetaRápida",
        // Datos del paciente y fecha
        "María Fernanda Torres", "34 años", "28/09/2026",
        // Diagnósticos
        "J00", "Rinofaringitis aguda [resfriado común]", "J02.9", "Faringitis aguda, no especificada",
        // Medicamentos
        "1. Paracetamol", "1 tableta de 500 mg", "Cada 8 horas", "5 días", "Tomar después de las comidas.",
        "2. Ibuprofeno jarabe", "10 ml", "Cada 12 horas", "3 días");
    assertThat(texto.split("Indicaciones:", -1)).hasSize(2);
  }

  @Test
  void contenidoLargoPaginaSinCortarse() throws Exception {
    String largo = "Indicación extensa con tildes y eñes: ñandú, acción, pingüino. ".repeat(8).substring(0, 495) + " FIN.";
    List<String> codigos = IntStream.rangeClosed(0, 9).mapToObj(i -> "J0" + i).toList();
    List<Cie10> diagnosticos = codigos.stream()
        .map(c -> cie(c, "Descripción del diagnóstico " + c + " " + "x".repeat(200))).toList();
    List<Medicamento> medicamentos = IntStream.rangeClosed(1, 10)
        .mapToObj(i -> new Medicamento(i, "d".repeat(100), "f".repeat(100), "u".repeat(100), largo)).toList();
    List<Vademecum> productos = IntStream.rangeClosed(1, 10).mapToObj(i -> vad("Producto " + i)).toList();
    RecetaRequest r = new RecetaRequest(new Paciente("P".repeat(150), 130), new Profesional("Q".repeat(150)),
        LocalDate.of(2026, 9, 28), codigos, medicamentos);

    byte[] pdf = generator.generar(r, diagnosticos, productos);
    PdfReader reader = new PdfReader(pdf);
    assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(2);
    String texto = texto(pdf);
    assertThat(texto).contains("J09", "10. Producto 10", "FIN.");
  }

  @Test
  void losMetadatosNoContienenDatosPersonales() throws Exception {
    RecetaRequest r = new RecetaRequest(new Paciente("María Fernanda Torres", 34),
        new Profesional("Dr. Carlos Andrade"), LocalDate.of(1999, 12, 31), List.of("J00"),
        List.of(new Medicamento(1, "1 tableta", "Cada 8 horas", "5 días", null)));
    // CreationDate y ModDate son la hora de generación; ningún campo debe traer datos de la receta.
    byte[] pdf = generator.generar(r, List.of(cie("J00", "Rinofaringitis")), List.of(vad("Paracetamol")));
    assertThat(String.join(" ", new PdfReader(pdf).getInfo().values()))
        .doesNotContain("María", "Torres", "Andrade", "1999", "31/12");
  }

  private static String texto(byte[] pdf) throws Exception {
    PdfReader reader = new PdfReader(pdf);
    PdfTextExtractor extractor = new PdfTextExtractor(reader);
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= reader.getNumberOfPages(); i++) sb.append(extractor.getTextFromPage(i)).append('\n');
    // El extractor parte las líneas largas; se unen para buscar frases completas.
    return sb.toString().replaceAll("\\s+", " ");
  }
  private static Cie10 cie(String codigo, String descripcion) { return new Cie10(codigo, descripcion, null); }
  private static Vademecum vad(String nombre) { return new Vademecum(nombre, null, null, null, null, null, null); }
}
