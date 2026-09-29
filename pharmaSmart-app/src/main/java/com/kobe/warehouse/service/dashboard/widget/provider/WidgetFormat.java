package com.kobe.warehouse.service.dashboard.widget.provider;

import com.kobe.warehouse.service.dto.records.VenteRecord;
import java.util.LinkedHashMap;
import java.util.Map;

/** Petits utilitaires partagés par les fournisseurs de widgets. */
final class WidgetFormat {

    /** Aucune vente sur la période : les services renvoient parfois null plutôt qu'un total nul. */
    static final VenteRecord EMPTY_VENTE = new VenteRecord(0, 0, 0, 0, 0, 0, 0d, 0, 0, 0, 0, 0, 0, 0, 0L, null, null, null);

    private WidgetFormat() {}

    static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    static long nz(Long value) {
        return value == null ? 0 : value;
    }

    /** Ligne de tableau à partir de paires champ / valeur, dans l'ordre donné. */
    static Map<String, Object> row(Object... fieldsAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i + 1 < fieldsAndValues.length; i += 2) {
            row.put((String) fieldsAndValues[i], fieldsAndValues[i + 1]);
        }
        return row;
    }
}
