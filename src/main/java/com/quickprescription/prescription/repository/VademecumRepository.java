package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Vademecum;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VademecumRepository extends JpaRepository<Vademecum, Integer> {
  @Query("select v from Vademecum v where lower(v.nombre) like lower(concat('%', :q, '%')) or lower(v.composicion) like lower(concat('%', :q, '%'))")
  Page<Vademecum> search(@Param("q") String q, Pageable pageable);
  @Query("select v from Vademecum v where lower(v.casaComercial) like lower(concat('%', :casa, '%'))")
  Page<Vademecum> findByCasaComercial(@Param("casa") String casa, Pageable pageable);
  @Query("select v from Vademecum v join v.cie10s c where c.codigo = :codigo")
  Page<Vademecum> findByCie10Codigo(@Param("codigo") String codigo, Pageable pageable);
}
