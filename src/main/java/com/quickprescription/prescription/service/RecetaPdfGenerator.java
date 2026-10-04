package com.quickprescription.prescription.service;

import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma el PDF de la receta en memoria siguiendo la plantilla "Prescription" (bandas azules,
 * dos columnas de datos, bloque Rx, firma y pie con contacto). No escribe archivos, no registra
 * logs y no pone datos personales en los metadatos del documento.
 * Usa Liberation Sans embebida con Identity-H para soportar tildes y ñ.
 */
@Component
public class RecetaPdfGenerator {
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  // Paleta tomada de la plantilla.
  private static final Color AZUL_OSCURO = new Color(0x17, 0x49, 0x6E);
  private static final Color AZUL_MEDIO = new Color(0x3E, 0x7C, 0xB0);
  private static final Color AZUL_CLARO = new Color(0x8F, 0xBF, 0xE0);
  private static final Color GRIS_LINEA = new Color(0xB9, 0xCB, 0xDA);
  private static final Color GRIS_TEXTO = new Color(0x55, 0x66, 0x75);

  // Márgenes del contenido (coinciden con las bandas de color).
  private static final float MARGEN_X = 50f;
  private static final float HEADER_ALTO = 95f;
  private static final float FOOTER_ALTO = 60f;

  private final BaseFont regular;
  private final BaseFont negrita;

  public RecetaPdfGenerator() {
    this.regular = fuente("liberation/LiberationSans-Regular.ttf");
    this.negrita = fuente("liberation/LiberationSans-Bold.ttf");
  }

  public byte[] generar(RecetaRequest r, List<Cie10> diagnosticos, List<Vademecum> productos) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // El margen superior/inferior deja espacio para las bandas de encabezado y pie.
    Document doc = new Document(PageSize.A4, MARGEN_X, MARGEN_X, HEADER_ALTO + 24f, FOOTER_ALTO + 20f);
    try {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      writer.setPageEvent(new Decoracion(r));
      doc.open();

      float anchoUtil = doc.right() - doc.left();
      float colGap = 24f;
      float colAncho = (anchoUtil - colGap) / 2f;

      // --- Datos en dos columnas: Paciente/Fecha (izq) y Diagnósticos (der) ---
      PdfPTable datos = new PdfPTable(new float[] { colAncho, colGap, colAncho });
      datos.setWidthPercentage(100);
      datos.setSpacingAfter(18f);

      datos.addCell(bloqueIzquierdo(r));
      datos.addCell(celdaVacia());
      datos.addCell(bloqueDerecho(diagnosticos));
      doc.add(datos);

      // --- Rx ---
      Paragraph rx = new Paragraph("Rx", new Font(negrita, 26, Font.NORMAL, AZUL_OSCURO));
      rx.setSpacingAfter(8f);
      doc.add(rx);

      // --- Medicamentos ---
      Font etiqueta = new Font(negrita, 10, Font.NORMAL, AZUL_OSCURO);
      Font texto = new Font(regular, 10, Font.NORMAL, Color.BLACK);
      List<Medicamento> medicamentos = r.medicamentos();
      for (int i = 0; i < medicamentos.size(); i++) {
        Medicamento m = medicamentos.get(i);
        Paragraph nombre = new Paragraph((i + 1) + ". " + productos.get(i).getNombre(),
            new Font(negrita, 11, Font.NORMAL, Color.BLACK));
        nombre.setSpacingBefore(6f);
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

  /** Columna izquierda: Paciente, Edad y Fecha, con línea inferior en cada campo. */
  private PdfPCell bloqueIzquierdo(RecetaRequest r) {
    PdfPTable t = new PdfPTable(1);
    t.setWidthPercentage(100);
    t.addCell(campoSubrayado("Nombre del paciente", r.paciente().nombre()));
    t.addCell(campoSubrayado("Edad", r.paciente().edad() + (r.paciente().edad() == 1 ? " año" : " años")));
    t.addCell(campoSubrayado("Fecha", r.fecha().format(FECHA)));
    return envolver(t);
  }

  /** Columna derecha: Diagnósticos CIE-10 (uno por línea), con línea inferior. */
  private PdfPCell bloqueDerecho(List<Cie10> diagnosticos) {
    PdfPTable t = new PdfPTable(1);
    t.setWidthPercentage(100);
    StringBuilder sb = new StringBuilder();
    for (Cie10 c : diagnosticos) {
      if (sb.length() > 0) sb.append('\n');
      sb.append(c.getCodigo()).append(" - ").append(c.getDescripcion());
    }
    t.addCell(campoSubrayado("Diagnóstico(s) (CIE-10)", sb.toString()));
    return envolver(t);
  }

  /** Campo con etiqueta pequeña en azul y valor debajo, con borde inferior a modo de línea. */
  private PdfPCell campoSubrayado(String etiqueta, String valor) {
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.BOTTOM);
    c.setBorderColor(GRIS_LINEA);
    c.setBorderWidthBottom(1f);
    c.setPaddingTop(6f);
    c.setPaddingBottom(4f);

    Paragraph label = new Paragraph(etiqueta, new Font(negrita, 8, Font.NORMAL, AZUL_OSCURO));
    label.setSpacingAfter(2f);
    Paragraph value = new Paragraph(valor == null || valor.isBlank() ? " " : valor,
        new Font(regular, 10, Font.NORMAL, Color.BLACK));
    c.addElement(label);
    c.addElement(value);
    return c;
  }

  private PdfPCell envolver(PdfPTable t) {
    PdfPCell c = new PdfPCell(t);
    c.setBorder(Rectangle.NO_BORDER);
    c.setPadding(0);
    return c;
  }

  private PdfPCell celdaVacia() {
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.NO_BORDER);
    return c;
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
  private static BaseFont fuente(String recurso) {
    try (InputStream in = RecetaPdfGenerator.class.getClassLoader().getResourceAsStream(recurso)) {
      if (in == null) throw new IllegalStateException("No se encontró la fuente " + recurso);
      String nombre = recurso.substring(recurso.lastIndexOf('/') + 1);
      return BaseFont.createFont(nombre, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
    } catch (IOException | DocumentException e) {
      throw new IllegalStateException("No se pudo cargar la fuente " + recurso, e);
    }
  }

  /**
   * Dibuja en cada página la banda de encabezado (con "PRESCRIPTION", cruz médica y profesional),
   * la banda de pie con el contacto y la firma. Se ejecuta por evento para quedar en el fondo.
   */
  private final class Decoracion extends PdfPageEventHelper {
    private final RecetaRequest receta;
    Decoracion(RecetaRequest receta) { this.receta = receta; }

    @Override public void onEndPage(PdfWriter writer, Document doc) {
      PdfContentByte cb = writer.getDirectContentUnder();
      float anchoPagina = doc.getPageSize().getWidth();
      float altoPagina = doc.getPageSize().getHeight();

      dibujarEncabezado(cb, anchoPagina, altoPagina);
      dibujarCruz(cb, altoPagina);
      dibujarFirma(cb, doc);
      dibujarPie(cb, anchoPagina);
    }

    private void dibujarEncabezado(PdfContentByte cb, float ancho, float alto) {
      float y = alto - HEADER_ALTO;
      // Banda clara de fondo del encabezado.
      cb.setColorFill(new Color(0xEA, 0xF2, 0xFA));
      cb.rectangle(0, y, ancho, HEADER_ALTO);
      cb.fill();
      // Banda azul superior derecha con "PRESCRIPTION".
      cb.setColorFill(AZUL_MEDIO);
      cb.rectangle(ancho * 0.52f, alto - 46f, ancho - (ancho * 0.52f), 46f);
      cb.fill();
      textoDirecto(cb, "PRESCRIPTION", negrita, 13, Color.WHITE,
          ancho - MARGEN_X, alto - 30f, Element.ALIGN_RIGHT);

      // Nombre del profesional y tagline.
      textoDirecto(cb, receta.profesional().nombre(), negrita, 15, AZUL_OSCURO,
          ancho - MARGEN_X, y + 42f, Element.ALIGN_RIGHT);
      textoDirecto(cb, "Profesional de la salud", regular, 9, GRIS_TEXTO,
          ancho - MARGEN_X, y + 26f, Element.ALIGN_RIGHT);
      textoDirecto(cb, "RecetaRápida", regular, 9, AZUL_MEDIO,
          ancho - MARGEN_X, y + 12f, Element.ALIGN_RIGHT);
    }

    /** Cruz médica dibujada (sin logo externo) dentro de un círculo, a la izquierda del encabezado. */
    private void dibujarCruz(PdfContentByte cb, float alto) {
      float cx = MARGEN_X + 28f;
      float cy = alto - 47f;
      cb.setColorStroke(AZUL_MEDIO);
      cb.setLineWidth(2f);
      cb.circle(cx, cy, 26f);
      cb.stroke();
      cb.setColorFill(AZUL_MEDIO);
      float b = 5f, l = 15f; // brazo y largo de la cruz
      cb.rectangle(cx - b, cy - l, 2 * b, 2 * l);
      cb.rectangle(cx - l, cy - b, 2 * l, 2 * b);
      cb.fill();
    }

    private void dibujarFirma(PdfContentByte cb, Document doc) {
      float y = FOOTER_ALTO + 36f;
      float x2 = doc.right();
      float x1 = x2 - 170f;
      cb.setColorStroke(GRIS_LINEA);
      cb.setLineWidth(1f);
      cb.moveTo(x1, y);
      cb.lineTo(x2, y);
      cb.stroke();
      textoDirecto(cb, "Firma", regular, 9, GRIS_TEXTO, x1, y - 12f, Element.ALIGN_LEFT);
    }

    private void dibujarPie(PdfContentByte cb, float ancho) {
      cb.setColorFill(AZUL_MEDIO);
      cb.rectangle(0, 0, ancho, FOOTER_ALTO);
      cb.fill();
      // Acento claro a la derecha.
      cb.setColorFill(AZUL_CLARO);
      cb.rectangle(ancho * 0.72f, 0, ancho - (ancho * 0.72f), FOOTER_ALTO);
      cb.fill();

      textoDirecto(cb, "RecetaRápida", negrita, 10, Color.WHITE, MARGEN_X, FOOTER_ALTO - 22f, Element.ALIGN_LEFT);
      textoDirecto(cb, "Documento generado automáticamente · sin validez legal",
          regular, 8, Color.WHITE, MARGEN_X, FOOTER_ALTO - 38f, Element.ALIGN_LEFT);
    }

    private void textoDirecto(PdfContentByte cb, String texto, BaseFont f, float size, Color color,
        float x, float y, int align) {
      cb.beginText();
      cb.setFontAndSize(f, size);
      cb.setColorFill(color);
      cb.showTextAligned(align, texto, x, y, 0);
      cb.endText();
    }
  }
}
