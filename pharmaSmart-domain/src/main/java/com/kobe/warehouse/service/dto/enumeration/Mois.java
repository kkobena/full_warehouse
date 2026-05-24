package com.kobe.warehouse.service.dto.enumeration;

import java.time.Month;

public enum Mois {
    JANVIER("Janvier", Month.JANUARY),
    FEVRIER("Février", Month.FEBRUARY),
    MARS("Mars", Month.MARCH),
    AVRIL("Avril", Month.APRIL),
    MAI("Mai", Month.MAY),
    JUIN("Juin", Month.JUNE),
    JUILLET("Juillet", Month.JULY),
    AOUT("Août", Month.AUGUST),
    SEPTEMBRE("Septembre", Month.SEPTEMBER),
    OCTOBRE("Octobre", Month.OCTOBER),
    NOVEMBRE("Novembre", Month.NOVEMBER),
    DECEMBRE("Décembre", Month.DECEMBER);

    private final String libelle;
    private final Month month;

    Mois(String libelle, Month month) {
        this.libelle = libelle;
        this.month = month;
    }

    public Month getMonth() {
        return month;
    }

    public String getLibelle() {
        return libelle;
    }

    public static Mois fromMonth(Month month) {
        for (Mois m : values()) {
            if (m.month == month) {
                return m;
            }
        }
        throw new IllegalArgumentException("Mois inconnu pour: " + month);
    }
}
