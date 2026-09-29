package com.kobe.warehouse.service.dashboard.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("DashboardWidgetService — registre des fournisseurs de widgets")
class DashboardWidgetServiceTest {

    private final WidgetAuthorizationService authorization = mock(WidgetAuthorizationService.class);

    @BeforeEach
    void connecter() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("caissier", null, List.of()));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("le contexte transmis au fournisseur porte le login du serveur, pas un paramètre client")
    void loginPoseParLeServeur() {
        WidgetDataProvider provider = provider("ca");
        when(provider.load(any(), any())).thenReturn(new WidgetData.Kpi(10, null, "F", null));
        DashboardWidgetService service = new DashboardWidgetService(providers(provider), authorization);

        service.load("ca", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28), 1, Map.of("login", "admin"));

        verify(provider).load(new WidgetContext(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28), 1, "caissier"), Map.of("login", "admin"));
    }

    @Test
    @DisplayName("un refus de droit empêche l'appel au fournisseur")
    void refusAvantChargement() {
        WidgetDataProvider provider = provider("marge");
        doThrow(new ForbiddenOperationException("refus", WidgetAuthorizationService.ERROR_NON_AUTORISE)).when(authorization).checkCanLoad("marge");
        DashboardWidgetService service = new DashboardWidgetService(providers(provider), authorization);

        assertThatThrownBy(() -> service.load("marge", null, null, null, Map.of())).isInstanceOf(ForbiddenOperationException.class);
        verify(provider, never()).load(any(), any());
    }

    @Test
    @DisplayName("une clé sans fournisseur est refusée")
    void cleInconnue() {
        DashboardWidgetService service = new DashboardWidgetService(providers(), authorization);
        assertThatThrownBy(() -> service.load("inexistant", null, null, null, Map.of()))
            .isInstanceOf(GenericError.class)
            .extracting("errorKey")
            .isEqualTo("widgetInconnu");
    }

    @Test
    @DisplayName("le catalogue garde les widgets de mise en page, qui n'ont pas de fournisseur")
    void catalogue() {
        when(authorization.findAllowedForCurrentUser()).thenReturn(List.of(new AllowedWidgetDTO("ca", true), new AllowedWidgetDTO("note", true)));
        DashboardWidgetService service = new DashboardWidgetService(providers(provider("ca")), authorization);

        assertThat(service.findAllowed()).extracting(AllowedWidgetDTO::key).containsExactly("ca", "note");
    }

    @Test
    @DisplayName("deux fournisseurs sur la même clé font échouer le démarrage")
    void doublon() {
        assertThatThrownBy(() -> new DashboardWidgetService(providers(provider("ca"), provider("ca")), authorization)).isInstanceOf(
            IllegalStateException.class
        );
    }

    private static WidgetDataProvider provider(String key) {
        WidgetDataProvider provider = mock(WidgetDataProvider.class);
        when(provider.key()).thenReturn(key);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<WidgetDataProvider> providers(WidgetDataProvider... providers) {
        ObjectProvider<WidgetDataProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.orderedStream()).thenAnswer(i -> Stream.of(providers));
        return objectProvider;
    }
}
