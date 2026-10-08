package com.kobe.warehouse.service.dto.controle;

/** Traitement chronique actif : désigné par un produit, ou à défaut par sa seule DCI du catalogue. */
public record TraitementChroniqueRefDTO(Integer produitId, String produitLibelle, Integer dciId) {}
