package com.kobe.warehouse.service.dto;

public class EtiquetteDTO {

  private String code;
  private String libelle;

  private String prix;
  private boolean print;

  private String magasin;
  private String date;
  private int order;

  public EtiquetteDTO() {}

  public EtiquetteDTO(
      String code,
      String libelle,
      String prix,
      boolean print,
      String magasin,
      String date,
      int order) {
    this.code = code;
    this.libelle = libelle;
    this.prix = prix;
    this.print = print;
    this.magasin = magasin;
    this.date = date;
    this.order = order;
  }

  public String getCode() {
    return code;
  }

  public EtiquetteDTO setCode(String code) {
    this.code = code;
    return this;
  }

  public String getLibelle() {
    return libelle;
  }

  public EtiquetteDTO setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public String getPrix() {
    return prix;
  }

  public EtiquetteDTO setPrix(String prix) {
    this.prix = prix;
    return this;
  }

  public boolean isPrint() {
    return print;
  }

  public EtiquetteDTO setPrint(boolean print) {
    this.print = print;
    return this;
  }

  public String getMagasin() {
    return magasin;
  }

  public EtiquetteDTO setMagasin(String magasin) {
    this.magasin = magasin;
    return this;
  }

  public String getDate() {
    return date;
  }

  public EtiquetteDTO setDate(String date) {
    this.date = date;
    return this;
  }

  public int getOrder() {
    return order;
  }

  public EtiquetteDTO setOrder(int order) {
    this.order = order;
    return this;
  }
}
