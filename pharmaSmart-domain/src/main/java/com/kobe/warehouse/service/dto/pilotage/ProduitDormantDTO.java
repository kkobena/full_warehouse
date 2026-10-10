package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Produit en stock sans vente depuis le seuil du « dormant » ; dernière vente vide s'il n'a jamais été vendu. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProduitDormantDTO(String cle, String libelle, Long quantite, Long valeur, LocalDate derniereVente) {}
