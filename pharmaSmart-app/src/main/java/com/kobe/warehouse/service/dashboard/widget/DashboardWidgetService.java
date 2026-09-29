package com.kobe.warehouse.service.dashboard.widget;

import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Point d'entrée unique des données de widgets : vérifie les droits, puis délègue au fournisseur. */
@Service
@Transactional(readOnly = true)
public class DashboardWidgetService {

    private final Map<String, WidgetDataProvider> providers;
    private final WidgetAuthorizationService widgetAuthorizationService;

    public DashboardWidgetService(ObjectProvider<WidgetDataProvider> providers, WidgetAuthorizationService widgetAuthorizationService) {
        // toMap refuse les doublons : deux fournisseurs sur la même clé font échouer le démarrage
        this.providers = providers.orderedStream().collect(Collectors.toMap(WidgetDataProvider::key, Function.identity()));
        this.widgetAuthorizationService = widgetAuthorizationService;
    }

    /**
     * Tous les widgets autorisés, y compris ceux sans fournisseur : les widgets de mise en page
     * (note, titre) n'ont pas de données. Le front ne propose que ceux qu'il sait afficher.
     */
    public List<AllowedWidgetDTO> findAllowed() {
        return widgetAuthorizationService.findAllowedForCurrentUser();
    }

    public WidgetData load(String key, LocalDate startDate, LocalDate endDate, Integer magasinId, Map<String, String> params) {
        WidgetDataProvider provider = providers.get(key);
        if (provider == null) {
            throw new GenericError("Widget inconnu : « %s ».".formatted(key), "widgetInconnu");
        }
        widgetAuthorizationService.checkCanLoad(key);
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
        return provider.load(new WidgetContext(startDate, endDate, magasinId, login), params);
    }
}
