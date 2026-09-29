package com.kobe.warehouse.service.dashboard.widget.provider;

import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Socle des configurations de widgets : la devise affichée vient de {@code APP_DEVISE}, jamais
 * d'une constante — une officine hors zone franc n'a qu'un réglage à changer.
 */
abstract class AbstractWidgets {

    private static final String DEVISE_DEFAUT = "FCFA";

    private final AppConfigurationService appConfigurationService;

    protected AbstractWidgets(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    /** Unité des indicateurs en montant. */
    protected String devise() {
        String devise = appConfigurationService.getDevise();
        return devise == null || devise.isBlank() ? DEVISE_DEFAUT : devise;
    }

    /** Montant pour une ligne de détail : « 12 500 FCFA ». */
    protected String amount(Number value) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(value == null ? 0 : value) + " " + devise();
    }
}
