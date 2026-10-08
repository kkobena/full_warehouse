package com.kobe.warehouse.service.dto.controle;

/** Produit déjà pris par le client ; {@code produitId} est nul pour un traitement connu par sa seule molécule. */
public record TraitementEnCoursDTO(Integer produitId, String produitLibelle, MoleculeDTO molecule) {}
