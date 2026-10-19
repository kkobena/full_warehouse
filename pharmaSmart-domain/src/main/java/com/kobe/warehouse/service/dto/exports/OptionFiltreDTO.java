package com.kobe.warehouse.service.dto.exports;

/** Une valeur proposée par un filtre d'export : ce qui part dans la demande, et ce que lit le pharmacien. */
public record OptionFiltreDTO(String valeur, String libelle) {}
