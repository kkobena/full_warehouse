package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.enumeration.Status;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = RemiseClientDTO.class, name = "remiseClient"),
  @JsonSubTypes.Type(value = RemiseProduitDTO.class, name = "remiseProduit")
})
public class RemiseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  protected Long id;

  protected String valeur;

  @NotNull protected Float remiseValue;

  protected String typeRemise;
  protected String typeLibelle;
  protected String displayName;

  protected Status status;
  protected LocalDate begin;
  protected LocalDate end;
  protected boolean enable;

  public RemiseDTO() {}

  public RemiseDTO(Remise remise) {
    id = remise.getId();
    valeur = remise.getValeur();
    remiseValue = remise.getRemiseValue();
    status = remise.getStatus();
    end = remise.getEnd();
    begin = remise.getBegin();
    displayName = remise.getValeur() + " " + remise.getRemiseValue() + "%";
    this.enable = remise.isEnable();
  }

  public boolean isEnable() {
    return enable;
  }

  public RemiseDTO setEnable(boolean enable) {
    this.enable = enable;
    return this;
  }

  public String getDisplayName() {
    return displayName;
  }

  public LocalDate getBegin() {
    return begin;
  }

  public RemiseDTO setBegin(LocalDate begin) {
    this.begin = begin;
    return this;
  }

  public LocalDate getEnd() {
    return end;
  }

  public RemiseDTO setEnd(LocalDate end) {
    this.end = end;
    return this;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getValeur() {
    return valeur;
  }

  public void setValeur(String valeur) {
    this.valeur = valeur;
  }

  public Float getRemiseValue() {
    return remiseValue;
  }

  public void setRemiseValue(Float remiseValue) {
    this.remiseValue = remiseValue;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getTypeRemise() {
    return typeRemise;
  }

  public void setTypeRemise(String typeRemise) {
    this.typeRemise = typeRemise;
  }

  public String getTypeLibelle() {
    return typeLibelle;
  }

  public void setTypeLibelle(String typeLibelle) {
    this.typeLibelle = typeLibelle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RemiseDTO)) {
      return false;
    }

    return id != null && id.equals(((RemiseDTO) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return "RemiseDTO{"
        + "id="
        + getId()
        + ", valeur='"
        + getValeur()
        + "'"
        + ", remiseValue="
        + getRemiseValue()
        + ", status='"
        + getStatus()
        + "'"
        + "}";
  }
}
