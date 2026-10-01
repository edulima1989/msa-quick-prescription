package com.quickprescription.prescription.service;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma el PDF de la receta en memoria. No escribe archivos, no registra logs y no pone datos
 * personales en los metadatos del documento.
 * Usa Liberation Sans embebida con Identity-H: el texto va en Unicode (tildes, ñ y cualquier
 * carácter que tenga la fuente) y solo se incrusta el subconjunto de glifos usados.
 */
@Component
public class RecetaPdfGenerator {
  static final String PIE = "Documento generado automáticamente por RecetaRápida. No requiere firma manuscrita.";
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private final BaseFont regular;
  private final BaseFont negrita;

  public RecetaPdfGenerator() {
    // Se cargan al arrancar: si falta la fuente, la aplicación no inicia.
    this.regular = fuente("liberation/LiberationSans-Regular.ttf");
    this.negrita = fuente("liberation/LiberationSans-Bold.ttf");
  }

  public byte[] generar(RecetaRequest r, List<Cie10> diagnosticos, List<Vademecum> productos) {
    Font titulo = new Font(negrita, 16);
    Font seccion = new Font(negrita, 12);
    Font etiqueta = new Font(negrita, 10);
    Font texto = new Font(regular, 10);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document(PageSize.A4, 50, 50, 50, 60);
    try {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      writer.setPageEvent(new Pie(new Font(regular, 8)));
      doc.open();

      Paragraph encabezado = new Paragraph("Receta médica", titulo);
      encabezado.setAlignment(Element.ALIGN_CENTER);
      doc.add(encabezado);
      Paragraph fecha = new Paragraph("Fecha de emisión: " + r.fecha().format(FECHA), texto);
      fecha.setAlignment(Element.ALIGN_CENTER);
      fecha.setSpacingAfter(12);
      doc.add(fecha);

      doc.add(titulo("Paciente", seccion));
      doc.add(campo("Nombre: ", r.paciente().nombre(), etiqueta, texto));
      doc.add(campo("Edad: ", r.paciente().edad() + (r.paciente().edad() == 1 ? " año" : " años"), etiqueta, texto));

      doc.add(titulo("Profesional", seccion));
      doc.add(campo("Nombre: ", r.profesional().nombre(), etiqueta, texto));

      doc.add(titulo("Diagnósticos (CIE-10)", seccion));
      PdfPTable tabla = new PdfPTable(new float[] {1, 5});
      tabla.setWidthPercentage(100);
      tabla.setHeaderRows(1);
      tabla.setSplitLate(false);
      tabla.setSpacingBefore(4);
      tabla.addCell(celda("Código", etiqueta));
      tabla.addCell(celda("Descripción", etiqueta));
      for (Cie10 c : diagnosticos) {
        tabla.addCell(celda(c.getCodigo(), texto));
        tabla.addCell(celda(c.getDescripcion(), texto));
      }
      doc.add(tabla);

      doc.add(titulo("Medicamentos", seccion));
      List<Medicamento> medicamentos = r.medicamentos();
      for (int i = 0; i < medicamentos.size(); i++) {
        Medicamento m = medicamentos.get(i);
        Paragraph nombre = new Paragraph((i + 1) + ". " + productos.get(i).getNombre(), etiqueta);
        nombre.setSpacingBefore(6);
        doc.add(nombre);
        doc.add(sangria(campo("Dosis: ", m.dosis(), etiqueta, texto)));
        doc.add(sangria(campo("Frecuencia: ", m.frecuencia(), etiqueta, texto)));
        doc.add(sangria(campo("Duración: ", m.duracion(), etiqueta, texto)));
        if (m.indicaciones() != null && !m.indicaciones().isBlank())
          doc.add(sangria(campo("Indicaciones: ", m.indicaciones(), etiqueta, texto)));
      }
      doc.close();
    } catch (DocumentException e) {
      // Sin mensaje ni causa: podrían contener texto de la receta.
      throw new IllegalStateException();
    }
    return out.toByteArray();
  }

  private static Paragraph titulo(String texto, Font f) {
    Paragraph p = new Paragraph(texto, f);
    p.setSpacingBefore(10);
    p.setSpacingAfter(2);
    return p;
  }
  private static Paragraph campo(String etiqueta, String valor, Font fe, Font fv) {
    Paragraph p = new Paragraph();
    p.add(new Chunk(etiqueta, fe));
    p.add(new Chunk(valor, fv));
    return p;
  }
  private static Paragraph sangria(Paragraph p) {
    p.setIndentationLeft(14);
    return p;
  }
  private static PdfPCell celda(String texto, Font f) {
    PdfPCell c = new PdfPCell(new Phrase(texto, f));
    c.setPadding(4);
    return c;
  }
  private static BaseFont fuente(String recurso) {
    try (InputStream in = RecetaPdfGenerator.class.getClassLoader().getResourceAsStream(recurso)) {
      if (in == null) throw new IllegalStateException("No se encontró la fuente " + recurso);
      String nombre = recurso.substring(recurso.lastIndexOf('/') + 1);
      return BaseFont.createFont(nombre, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
    } catch (IOException | DocumentException e) {
      throw new IllegalStateException("No se pudo cargar la fuente " + recurso, e);
    }
  }

  /** Pie en todas las páginas: nota de generación automática y número de página. */
  private static final class Pie extends PdfPageEventHelper {
    private final Font font;
    Pie(Font font) { this.font = font; }
    @Override public void onEndPage(PdfWriter writer, Document doc) {
      PdfContentByte cb = writer.getDirectContent();
      float y = doc.bottom() - 25;
      ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(PIE, font), doc.left(), y, 0);
      ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
          new Phrase("Página " + writer.getPageNumber(), font), doc.right(), y, 0);
    }
  }
}
