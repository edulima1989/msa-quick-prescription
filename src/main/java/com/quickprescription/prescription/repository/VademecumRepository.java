package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Vademecum;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VademecumRepository extends JpaRepository<Vademecum, Integer>, JpaSpecificationExecutor<Vademecum> {
  @Query("select v from Vademecum v join v.cie10s c where c.codigo = :codigo")
  Page<Vademecum> findByCie10Codigo(@Param("codigo") String codigo, Pageable pageable);
}
