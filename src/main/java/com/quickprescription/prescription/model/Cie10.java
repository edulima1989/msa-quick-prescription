package com.quickprescription.prescription.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "cie10", uniqueConstraints = @UniqueConstraint(name = "uk_cie10_codigo", columnNames = "codigo"))
public class Cie10 {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Column(nullable = false, length = 10) private String codigo;
  @Column(nullable = false, length = 255) private String descripcion;
  @Column(length = 100) private String categoria;
  @ManyToMany(mappedBy = "cie10s", fetch = FetchType.LAZY)
  private Set<Vademecum> vademecums = new HashSet<>();

  public Cie10(String codigo, String descripcion, String categoria) {
    this.codigo = codigo; this.descripcion = descripcion; this.categoria = categoria;
  }
}
