package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dictionnaire des indicateurs du pilotage")
class DictionnaireIndicateursServiceImplTest {

    @Mock
    private NavAccessService navAccessService;

    @InjectMocks
    private DictionnaireIndicateursServiceImpl service;

    @Test
    @DisplayName("ne rend que les indicateurs des onglets autorisés, dans l'ordre du dictionnaire")
    void filtreParDroit() {
        autoriser(Set.of(DroitPilotage.PAGE.getCode(), DroitPilotage.RENTABILITE_REMISES.getCode()));

        List<String> codes = service.listerIndicateursAutorises().stream().map(IndicateurPilotageDTO::code).toList();

        assertThat(codes).contains("CA_TTC", "PANIER_MOYEN", "MARGE_BRUTE", "REMISES").doesNotContain("ACHATS_TTC", "PART_TIERS_PAYANT");
        assertThat(codes.indexOf("CA_TTC")).isLessThan(codes.indexOf("MARGE_BRUTE"));
    }

    @Test
    @DisplayName("sans aucun droit, le dictionnaire est vide")
    void videSansDroit() {
        autoriser(Set.of());

        assertThat(service.listerIndicateursAutorises()).isEmpty();
    }

    @Test
    @DisplayName("chaque indicateur porte le code nav_item de son droit")
    void porteLeCodeDuDroit() {
        autoriser(Set.of(DroitPilotage.PAGE.getCode()));

        IndicateurPilotageDTO caTtc = service.listerIndicateursAutorises().getFirst();

        assertThat(caTtc.code()).isEqualTo(IndicateurPilotage.CA_TTC.name());
        assertThat(caTtc.droit()).isEqualTo("pilotage");
    }

    private void autoriser(Set<String> codesAutorises) {
        when(navAccessService.isAllowed(anyCollection(), eq(NavAction.DISPLAY))).thenAnswer(invocation -> {
            Collection<String> codes = invocation.getArgument(0);
            return codes.stream().anyMatch(codesAutorises::contains);
        });
    }
}
