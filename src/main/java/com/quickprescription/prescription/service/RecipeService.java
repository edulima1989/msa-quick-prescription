package com.quickprescription.prescription.service;

import com.quickprescription.prescription.config.ApiException;
import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.repository.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class RecipeService {
  private final Cie10Repository cie10s;
  private final VademecumRepository vademecums;
  public RecipeService(Cie10Repository cie10s, VademecumRepository vademecums) {
    this.cie10s = cie10s; this.vademecums = vademecums;
  }
  public byte[] generate(RecetaRequest request) {
    if (request.codigosCie10().stream().distinct().count() != request.codigosCie10().size())
      throw new ApiException(400, "codigosCie10 no puede contener repetidos");
    if (request.codigosCie10().stream().anyMatch(c -> cie10s.findByCodigo(c).isEmpty()))
      throw new ApiException(422, "Uno o más códigos CIE-10 no existen");
    if (request.medicamentos().stream().anyMatch(m -> !vademecums.existsById(m.vademecumId())))
      throw new ApiException(422, "Uno o más vademecumId no existen");
    String text = "RecetaRapida\\nPaciente: " + request.paciente().nombre() + "\\nFecha: " + request.fecha()
        + "\\nProfesional: " + request.profesional().nombre();
    return pdf(text);
  }
  private byte[] pdf(String text) {
    String stream = "BT /F1 12 Tf 72 720 Td (" + text.replace("\\n", ") Tj 0 -18 Td (") + ") Tj ET";
    String body = "%PDF-1.4\\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\\n"
        + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>endobj\\n"
        + "4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\\n5 0 obj<</Length " + stream.length() + ">>stream\\n"
        + stream + "\\nendstream endobj\\ntrailer<</Root 1 0 R>>\\n%%EOF";
    return body.getBytes(StandardCharsets.US_ASCII);
  }
}
