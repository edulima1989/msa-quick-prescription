package com.quickprescription.prescription.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "vademecum")
public class Vademecum {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Column(nullable = false, length = 100) private String nombre;
  @Column(columnDefinition = "TEXT") private String composicion;
  @Column(length = 255) private String funcion;
  @Column(length = 100) private String presentacion;
  @Column(length = 100) private String dosificacion;
  @Column(name = "casa_comercial", length = 100) private String casaComercial;
  @Column(columnDefinition = "TEXT") private String contraindicaciones;
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "vademecum_cie10",
      joinColumns = @JoinColumn(name = "id_vademecum"),
      inverseJoinColumns = @JoinColumn(name = "id_cie10"))
  private Set<Cie10> cie10s = new HashSet<>();

  public Vademecum(String nombre, String composicion, String funcion, String presentacion,
      String dosificacion, String casaComercial, String contraindicaciones) {
    this.nombre = nombre; this.composicion = composicion; this.funcion = funcion;
    this.presentacion = presentacion; this.dosificacion = dosificacion;
    this.casaComercial = casaComercial; this.contraindicaciones = contraindicaciones;
  }
}
