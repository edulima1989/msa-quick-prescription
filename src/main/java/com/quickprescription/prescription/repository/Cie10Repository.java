package com.quickprescription.prescription.repository;

import com.quickprescription.prescription.model.Cie10;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface Cie10Repository extends JpaRepository<Cie10, Integer> {
  Optional<Cie10> findByCodigo(String codigo);
  @Query("select c from Cie10 c where lower(c.codigo) like lower(concat('%', :q, '%')) or lower(c.descripcion) like lower(concat('%', :q, '%'))")
  Page<Cie10> search(@Param("q") String q, Pageable pageable);
  @Query("select c from Cie10 c where lower(c.categoria) like lower(concat('%', :categoria, '%'))")
  Page<Cie10> findByCategoria(@Param("categoria") String categoria, Pageable pageable);
  @Query("select c from Cie10 c join c.vademecums v where v.id = :id")
  Page<Cie10> findByVademecumId(@Param("id") Integer id, Pageable pageable);
}
