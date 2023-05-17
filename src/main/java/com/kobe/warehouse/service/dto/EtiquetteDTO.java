package com.kobe.warehouse.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EtiquetteDTO {

    private final String code;
    private final String libelle;

    private final String prix;
    private final boolean print;

    private final String magasin;
    private final String date;
    private final int order;

}
