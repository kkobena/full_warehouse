package com.kobe.warehouse.domain.enumeration;

public enum NavTargetType {
    ROUTE,
    ACTION,
    GROUP,
    SECTION,
    DIVIDER,
    // widget du dashboard personnalisable : jamais affiché dans le menu, can_display = droit de l'ajouter
    WIDGET
}
