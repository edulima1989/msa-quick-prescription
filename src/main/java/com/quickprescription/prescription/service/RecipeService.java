package com.quickprescription.prescription.service;

import com.quickprescription.prescription.config.ApiException;
import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.model.Cie10;
import com.quickprescription.prescription.model.Vademecum;
import com.quickprescription.prescription.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RecipeService {
  private final Cie10Repository cie10s;
  private final VademecumRepository vademecums;
  private final RecetaPdfGenerator pdf;
  public RecipeService(Cie10Repository cie10s, VademecumRepository vademecums, RecetaPdfGenerator pdf) {
    this.cie10s = cie10s; this.vademecums = vademecums; this.pdf = pdf;
  }
  public byte[] generate(RecetaRequest request) {
    if (request.codigosCie10().stream().distinct().count() != request.codigosCie10().size())
      throw new ApiException(400, "codigosCie10 no puede contener repetidos");
    // Se conservan el orden de la solicitud y los mismos mensajes de 422.
    List<Optional<Cie10>> diagnosticos = request.codigosCie10().stream().map(cie10s::findByCodigo).toList();
    if (diagnosticos.stream().anyMatch(Optional::isEmpty))
      throw new ApiException(422, "Uno o más códigos CIE-10 no existen");
    List<Optional<Vademecum>> productos = request.medicamentos().stream()
        .map(m -> vademecums.findById(m.vademecumId())).toList();
    if (productos.stream().anyMatch(Optional::isEmpty))
      throw new ApiException(422, "Uno o más vademecumId no existen");
    return pdf.generar(request, diagnosticos.stream().map(Optional::get).toList(),
        productos.stream().map(Optional::get).toList());
  }
}
