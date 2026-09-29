package com.kobe.warehouse.security.navaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.errors.ExceptionTranslator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

@DisplayName("Sécurisation des endpoints — intercepteur")
class NavAccessInterceptorTest {

    @RequiresNavAccess("ecran")
    static class Controleur {

        public void lire() {}

        @RequiresNavAccess(value = { "ecran", "autre-ecran" }, action = NavAction.EXPORT)
        public void exporter() {}

        @NavAccessExempt("test")
        public void libre() {}
    }

    private final NavAccessService service = mock(NavAccessService.class);

    @BeforeEach
    void refuserParDefaut() {
        when(service.isAllowed(anyCollection(), any())).thenReturn(false);
        when(service.libelleOf(anyCollection())).thenReturn("Écran de test");
    }

    @Test
    @DisplayName("en mode ENFORCE, un accès sans droit est refusé avec le libellé de l'écran")
    void enforceRefuse() {
        assertThatThrownBy(() -> appeler(NavAccessMode.ENFORCE, "GET", "/api/test", "lire"))
            .isInstanceOf(NavAccessDeniedException.class)
            .hasMessage("Accès refusé : Écran de test");
    }

    @Test
    @DisplayName("en mode AUDIT, le même accès passe")
    void auditLaissePasser() throws Exception {
        assertThat(appeler(NavAccessMode.AUDIT, "GET", "/api/test", "lire")).isTrue();
        verify(service).isAllowed(List.of("ecran"), NavAction.ACCESS);
    }

    @Test
    @DisplayName("en mode OFF, aucun droit n'est lu")
    void offNeLitRien() throws Exception {
        assertThat(appeler(NavAccessMode.OFF, "DELETE", "/api/test", "lire")).isTrue();
        verify(service, never()).isAllowed(anyCollection(), any());
    }

    @Test
    @DisplayName("le droit exigé suit le verbe HTTP")
    void droitSelonLeVerbe() throws Exception {
        when(service.isAllowed(anyCollection(), any())).thenReturn(true);
        appeler(NavAccessMode.ENFORCE, "POST", "/api/test", "lire");
        appeler(NavAccessMode.ENFORCE, "PATCH", "/api/test", "lire");
        appeler(NavAccessMode.ENFORCE, "DELETE", "/api/test", "lire");
        verify(service).isAllowed(List.of("ecran"), NavAction.CREATE);
        verify(service).isAllowed(List.of("ecran"), NavAction.EDIT);
        verify(service).isAllowed(List.of("ecran"), NavAction.DELETE);
    }

    @Test
    @DisplayName("l'annotation de la méthode l'emporte sur celle de la classe")
    void methodeLEmporte() throws Exception {
        when(service.isAllowed(anyCollection(), eq(NavAction.EXPORT))).thenReturn(true);
        assertThat(appeler(NavAccessMode.ENFORCE, "GET", "/api/test/export", "exporter")).isTrue();
        verify(service).isAllowed(List.of("ecran", "autre-ecran"), NavAction.EXPORT);

        assertThat(appeler(NavAccessMode.ENFORCE, "GET", "/api/test/libre", "libre")).isTrue();
    }

    @Test
    @DisplayName("/api/admin/** est laissé à la chaîne de filtres")
    void adminLaisseALaChaine() throws Exception {
        assertThat(appeler(NavAccessMode.ENFORCE, "GET", "/api/admin/test", "lire")).isTrue();
        verify(service, never()).isAllowed(anyCollection(), any());
    }

    @Test
    @DisplayName("un refus sort en 403, et non plus en 500")
    void refusEn403() {
        ResponseEntity<Object> reponse = new ExceptionTranslator().handleAccessDenied(new NavAccessDeniedException("Valorisation du stock"));
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ProblemDetail) reponse.getBody()).getDetail()).isEqualTo("Accès refusé : Valorisation du stock");
    }

    private boolean appeler(NavAccessMode mode, String verbe, String uri, String methode) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(verbe, uri);
        HandlerMethod handler = new HandlerMethod(new Controleur(), Controleur.class.getMethod(methode));
        return new NavAccessInterceptor(service, mode).preHandle(request, new MockHttpServletResponse(), handler);
    }
}
