package com.quickprescription.prescription.service;

import com.quickprescription.prescription.config.*;
import com.quickprescription.prescription.dto.ApiDtos.*;
import com.quickprescription.prescription.mapper.CatalogMapper;
import com.quickprescription.prescription.model.*;
import com.quickprescription.prescription.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class CatalogService {
  private static final Set<String> CIE_SORTS = Set.of("codigo", "descripcion");
  private static final Set<String> VADE_SORTS = Set.of("nombre", "casaComercial");
  private final Cie10Repository cie10s;
  private final VademecumRepository vademecums;
  private final CatalogMapper mapper;

  public CatalogService(Cie10Repository cie10s, VademecumRepository vademecums, CatalogMapper mapper) {
    this.cie10s = cie10s; this.vademecums = vademecums; this.mapper = mapper;
  }

  public PageResponse<Cie10Response> listCie(int page, int size, String sort, String q, String categoria) {
    Pageable pageable = PageRequestFactory.create(page, size, sort, CIE_SORTS);
    Page<Cie10> result = q != null && !q.isBlank() ? cie10s.search(q, pageable)
        : categoria != null && !categoria.isBlank() ? cie10s.findByCategoria(categoria, pageable) : cie10s.findAll(pageable);
    return mapper.toPage(result.map(mapper::toResponse));
  }
  public Cie10Response getCie(String codigo) {
    return cie10s.findByCodigo(codigo).map(mapper::toResponse)
        .orElseThrow(() -> new ApiException(404, "CIE-10 no encontrado"));
  }
  public PageResponse<VademecumResponse> relatedVademecum(String codigo, int page, int size, String sort) {
    getCie(codigo);
    Page<Vademecum> result = vademecums.findByCie10Codigo(codigo,
        PageRequestFactory.create(page, size, sort, VADE_SORTS));
    return mapper.toPage(result.map(mapper::toResponse));
  }
  public PageResponse<VademecumResponse> listVade(int page, int size, String sort, String q, String casaComercial) {
    Pageable pageable = PageRequestFactory.create(page, size, sort, VADE_SORTS);
    Page<Vademecum> result = q != null && !q.isBlank() ? vademecums.search(q, pageable)
        : casaComercial != null && !casaComercial.isBlank() ? vademecums.findByCasaComercial(casaComercial, pageable) : vademecums.findAll(pageable);
    return mapper.toPage(result.map(mapper::toResponse));
  }
  public VademecumResponse getVade(Integer id) {
    return vademecums.findById(id).map(mapper::toResponse)
        .orElseThrow(() -> new ApiException(404, "Vademécum no encontrado"));
  }
  public PageResponse<Cie10Response> relatedCie(Integer id, int page, int size, String sort) {
    getVade(id);
    Page<Cie10> result = cie10s.findByVademecumId(id,
        PageRequestFactory.create(page, size, sort, CIE_SORTS));
    return mapper.toPage(result.map(mapper::toResponse));
  }
}
