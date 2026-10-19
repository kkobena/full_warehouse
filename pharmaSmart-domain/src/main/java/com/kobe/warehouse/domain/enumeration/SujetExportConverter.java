package com.kobe.warehouse.domain.enumeration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * L'entrée du catalogue rangée dans la colonne {@code export} par son nom de constante — comme un {@code @Enumerated(STRING)},
 * mais sur deux énumérations : les exports de données et les éditions. Les lignes écrites avant les éditions se relisent sans
 * reprise, leur code n'ayant pas changé.
 */
@Converter
public class SujetExportConverter implements AttributeConverter<SujetExport, String> {

    @Override
    public String convertToDatabaseColumn(SujetExport sujet) {
        return sujet == null ? null : sujet.name();
    }

    @Override
    public SujetExport convertToEntityAttribute(String code) {
        return code == null ? null : SujetsExport.lire(code);
    }
}
