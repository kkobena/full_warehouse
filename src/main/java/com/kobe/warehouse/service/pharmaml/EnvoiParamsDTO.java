package com.kobe.warehouse.service.pharmaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnvoiParamsDTO {
  private Long commandeId;
  private LocalDate dateLivraisonSouhaitee;
  private int typeCommande;
  private String typeCommandeExecptionel;
  private String commentaire;
  private Long ruptureId;
  Long grossisteId;
}
