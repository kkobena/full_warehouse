package com.kobe.warehouse.service.dashboard.widget;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Source de données d'un widget du dashboard personnalisable.
 *
 * <p>Chaque implémentation est un bean Spring. Sa clé correspond au {@code nav_item} de type
 * {@code WIDGET} de code {@code widget.<clé>}, qui porte les droits par rôle et la fonctionnalité
 * de licence requise. Un fournisseur sans {@code nav_item} n'est accessible à personne.
 *
 * <p>Les droits sont vérifiés avant l'appel à {@link #load} : un fournisseur n'a pas à les
 * contrôler, seulement à filtrer sur {@link WidgetContext#login()} s'il sert des données « moi ».
 */
public interface WidgetDataProvider {
    String key();

    WidgetData load(WidgetContext context, Map<String, String> params);

    /** Fournisseur déclaré en une ligne dans une classe {@code @Configuration}. */
    static WidgetDataProvider of(String key, BiFunction<WidgetContext, Map<String, String>, WidgetData> loader) {
        return new WidgetDataProvider() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public WidgetData load(WidgetContext context, Map<String, String> params) {
                return loader.apply(context, params);
            }
        };
    }
}
