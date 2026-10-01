package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Cie10;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface Cie10Repository extends JpaRepository<Cie10, Integer>, JpaSpecificationExecutor<Cie10> {
  Optional<Cie10> findByCodigo(String codigo);
  @Query("select c from Cie10 c join c.vademecums v where v.id = :id")
  Page<Cie10> findByVademecumId(@Param("id") Integer id, Pageable pageable);
}
