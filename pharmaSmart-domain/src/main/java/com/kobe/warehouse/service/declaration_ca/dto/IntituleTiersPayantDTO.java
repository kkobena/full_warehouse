package com.kobe.warehouse.service.declaration_ca.dto;

/** Un rattachement vente / tiers-payant. Une vente peut en avoir plusieurs. */
public record IntituleTiersPayantDTO(Long saleId, String intitule) {}
