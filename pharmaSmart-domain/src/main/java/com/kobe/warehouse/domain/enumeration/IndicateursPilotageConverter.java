package com.kobe.warehouse.domain.enumeration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Liste d'indicateurs rangée en une colonne texte : {@code CA_TTC,MARGE_BRUTE}. */
@Converter
public class IndicateursPilotageConverter implements AttributeConverter<List<IndicateurPilotage>, String> {

    private static final String SEPARATEUR = ",";

    @Override
    public String convertToDatabaseColumn(List<IndicateurPilotage> indicateurs) {
        return indicateurs == null ? null : indicateurs.stream().map(Enum::name).collect(Collectors.joining(SEPARATEUR));
    }

    @Override
    public List<IndicateurPilotage> convertToEntityAttribute(String colonne) {
        return colonne == null || colonne.isBlank() ? List.of() : Arrays.stream(colonne.split(SEPARATEUR)).map(IndicateurPilotage::valueOf).toList();
    }
}
