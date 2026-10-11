package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/** Graduations d'un axe de graphe au format des tuiles (« 19,2 M », « 32,5 % »). */
public final class FormatAxeRapport extends NumberFormat {

    private final UniteIndicateur unite;

    public FormatAxeRapport(UniteIndicateur unite) {
        this.unite = unite;
    }

    @Override
    public StringBuffer format(double nombre, StringBuffer cible, FieldPosition position) {
        return cible.append(FormatRapport.formaterGraduation(nombre, unite));
    }

    @Override
    public StringBuffer format(long nombre, StringBuffer cible, FieldPosition position) {
        return format((double) nombre, cible, position);
    }

    @Override
    public Number parse(String source, ParsePosition position) {
        throw new UnsupportedOperationException("Graduation en lecture seule");
    }
}
