package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageDemarqueRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pilotage — démarque")
class DemarquePilotageServiceImplTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final PeriodeDTO REFERENCE = new PeriodeDTO(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 9));
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, true, null, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private PilotageDemarqueRepository pilotageDemarqueRepository;

    @Mock
    private LecteurMesuresPilotage lecteurMesuresPilotage;

    @InjectMocks
    private DemarquePilotageServiceImpl service;

    @Test
    @DisplayName("motifs rapprochés de la référence, un motif absent valant 0 ; part du CA")
    void motifsCompares() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(PERIODE, REFERENCE, true));
        when(pilotageDemarqueRepository.sommerParMotif(eq(PERIODE.du().atStartOfDay()), eq(LocalDate.of(2026, 10, 10).atStartOfDay()), any(), any())).thenReturn(
            List.of(new MontantDemarqueDTO("1", "Périmés", 10L, 3_000L), new MontantDemarqueDTO("2", "Casse", 2L, 1_000L))
        );
        when(pilotageDemarqueRepository.sommerParMotif(eq(REFERENCE.du().atStartOfDay()), any(), any(), any())).thenReturn(
            List.of(new MontantDemarqueDTO("1", "Périmés", 8L, 2_000L), new MontantDemarqueDTO("3", "Vol", 1L, 500L))
        );
        when(lecteurMesuresPilotage.lireTotal(PERIODE)).thenReturn(ca(200_000));
        when(lecteurMesuresPilotage.lireTotal(REFERENCE)).thenReturn(ca(250_000));

        DemarqueRentabiliteDTO demarque = service.analyserDemarque(REQUETE);

        assertThat(demarque.valeur().valeur()).isEqualTo(4_000);
        assertThat(demarque.valeur().valeurReference()).isEqualTo(2_500);
        assertThat(demarque.partDuCa().valeur()).isCloseTo(2.0, within(1e-9));
        assertThat(demarque.partDuCa().valeurReference()).isCloseTo(1.0, within(1e-9));
        assertThat(demarque.parMotif())
            .extracting(LigneDemarqueDTO::libelle, ligne -> ligne.valeur().valeur(), ligne -> ligne.valeur().valeurReference())
            .containsExactly(tuple("Périmés", 3_000.0, 2_000.0), tuple("Casse", 1_000.0, 0.0), tuple("Vol", 0.0, 500.0));
    }

    private static MesuresPilotage ca(long montant) {
        return new MesuresPilotage(1, 0, montant, montant, 0, 0, montant, montant, 0, 0, 0, 0, 1);
    }
}
