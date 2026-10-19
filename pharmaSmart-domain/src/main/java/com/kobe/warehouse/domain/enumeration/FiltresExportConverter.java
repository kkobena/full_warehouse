package com.kobe.warehouse.domain.enumeration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Filtres d'un export rangés en une colonne texte : {@code FAMILLE=12,45;NATURE_VENTE=COMPTANT}. Les valeurs sont des
 * identifiants, des codes ou des noms de constantes — jamais de saisie libre, donc jamais de séparateur à échapper.
 */
@Converter
public class FiltresExportConverter implements AttributeConverter<Map<FiltreExport, List<String>>, String> {

    private static final String ENTRE_FILTRES = ";";
    private static final String ENTRE_VALEURS = ",";
    private static final String AFFECTATION = "=";

    @Override
    public String convertToDatabaseColumn(Map<FiltreExport, List<String>> filtres) {
        if (filtres == null || filtres.isEmpty()) {
            return null;
        }
        StringBuilder colonne = new StringBuilder();
        for (Map.Entry<FiltreExport, List<String>> filtre : filtres.entrySet()) {
            if (filtre.getValue() == null || filtre.getValue().isEmpty()) {
                continue;
            }
            if (!colonne.isEmpty()) {
                colonne.append(ENTRE_FILTRES);
            }
            colonne.append(filtre.getKey().name()).append(AFFECTATION).append(String.join(ENTRE_VALEURS, filtre.getValue()));
        }
        return colonne.isEmpty() ? null : colonne.toString();
    }

    @Override
    public Map<FiltreExport, List<String>> convertToEntityAttribute(String colonne) {
        Map<FiltreExport, List<String>> filtres = new EnumMap<>(FiltreExport.class);
        if (colonne == null || colonne.isBlank()) {
            return filtres;
        }
        for (String entree : colonne.split(ENTRE_FILTRES)) {
            int affectation = entree.indexOf(AFFECTATION);
            if (affectation <= 0) {
                continue;
            }
            filtres.put(
                FiltreExport.valueOf(entree.substring(0, affectation)),
                Arrays.asList(entree.substring(affectation + 1).split(ENTRE_VALEURS))
            );
        }
        return filtres;
    }
}
