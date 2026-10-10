package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pilotage — familles × années")
class CroiseurAnneesTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 3, 15);

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @InjectMocks
    private CroiseurAnnees croiseur;

    @Test
    @DisplayName("à date, chaque année s'arrête au même jour ; lignes classées sur l'année en cours, comparées à la précédente")
    void famillesParAnnee() {
        ventiler(2025, element("1", "Antalgiques", 3_000), element("2", "Vitamines", 1_000));
        ventiler(2026, element("1", "Antalgiques", 2_400), element("2", "Vitamines", 2_500));

        CroiseAnalyseDTO croise = croiseur.croiser(AxeAnalyse.FAMILLE, IndicateurPilotage.CA_TTC, List.of(2025, 2026), AUJOURDHUI, true, List.of());

        assertThat(croise.colonnes()).extracting(MembreAnalyseDTO::libelle).containsExactly("2025", "2026");
        assertThat(croise.lignes()).extracting(LigneCroiseeDTO::libelle).containsExactly("Vitamines", "Antalgiques");
        assertThat(croise.lignes().getLast().cellules())
            .extracting(cellule -> cellule.valeur(), cellule -> cellule.valeurReference(), cellule -> cellule.ecartPct())
            .containsExactly(tuple(3_000.0, null, null), tuple(2_400.0, 3_000.0, -20.0));
        verify(ventilateurPilotage).comparer(any(), argThat(c -> c.periode().au().equals(LocalDate.of(2025, 3, 15))), eq(AxeAnalyse.FAMILLE), any(), anyList(), any());
    }

    @Test
    @DisplayName("le nombre de ventes ne se ventile pas par famille : pas de tableau")
    void indicateurNonVentilable() {
        assertThat(croiseur.croiser(AxeAnalyse.FAMILLE, IndicateurPilotage.NB_VENTES, List.of(2025, 2026), AUJOURDHUI, true, List.of())).isNull();
    }

    private void ventiler(int annee, MesuresComparees... elements) {
        when(ventilateurPilotage.comparer(any(), argThat(c -> c != null && c.periode().du().getYear() == annee), eq(AxeAnalyse.FAMILLE), any(), anyList(), any())).thenReturn(
            new Ventilation(List.of(elements), MesuresComparees.AUCUNES)
        );
    }

    private static MesuresComparees element(String cle, String libelle, long ca) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, new MesuresPilotage(0, 0, ca, ca, 0, 0, ca, ca, 0, 0, 0, 0, 0), MesuresPilotage.AUCUNE);
    }
}
