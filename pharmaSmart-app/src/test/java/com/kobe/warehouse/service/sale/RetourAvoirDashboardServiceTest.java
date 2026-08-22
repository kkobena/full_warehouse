package com.kobe.warehouse.service.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetourAvoirDashboardServiceTest {

    @Mock
    private RetourClientRepository retourClientRepository;
    @Mock
    private AvoirClientRepository avoirClientRepository;

    private RetourAvoirDashboardService service;

    @BeforeEach
    void setUp() {
        service = new RetourAvoirDashboardService(retourClientRepository, avoirClientRepository);
    }

    @Test
    void getStats_MappeLesAgregatsEtUtiliseLesBornesDuMois() {
        YearMonth mois = YearMonth.of(2026, 7);
        LocalDateTime debut = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 8, 1, 0, 0);

        when(retourClientRepository.statsGlobales(debut, fin))
            .thenReturn(List.<Object[]>of(new Object[]{4L, 15_000L}));
        when(retourClientRepository.statsParMotif(debut, fin))
            .thenReturn(List.<Object[]>of(
                new Object[]{MotifRetourClient.PRODUIT_DEFECTUEUX, 3L},
                new Object[]{MotifRetourClient.ERREUR_QUANTITE, 1L}));
        when(retourClientRepository.produitsEnAlerte(debut, fin, 5))
            .thenReturn(List.<Object[]>of(new Object[]{10, "Produit A", "CIP10", 6L}));
        when(retourClientRepository.clientsEnAlerte(debut, fin, 3))
            .thenReturn(List.<Object[]>of(new Object[]{20, "Jean", "Kouadio", 4L}));
        when(avoirClientRepository.statsAvoirsOuverts())
            .thenReturn(List.<Object[]>of(new Object[]{2L, 7_500L}));
        when(avoirClientRepository.countAvoirsProchesExpiration(LocalDate.now().plusDays(7)))
            .thenReturn(1L);

        RetourAvoirStatsDTO result = service.getStats(mois);

        assertEquals(4, result.nbRetoursMois());
        assertEquals(15_000, result.montantTotalRetoursMois());
        assertEquals(2, result.statsParMotif().size());
        assertEquals(MotifRetourClient.PRODUIT_DEFECTUEUX,
            result.statsParMotif().getFirst().motif());
        assertEquals(3L, result.statsParMotif().getFirst().count());
        assertEquals(10, result.produitsEnAlerte().getFirst().produitId());
        assertEquals("Produit A", result.produitsEnAlerte().getFirst().libelle());
        assertEquals("CIP10", result.produitsEnAlerte().getFirst().codeCip());
        assertEquals(6L, result.produitsEnAlerte().getFirst().nbRetours());
        assertEquals("Jean Kouadio", result.clientsEnAlerte().getFirst().nom());
        assertEquals(4L, result.clientsEnAlerte().getFirst().nbRetours());
        assertEquals(2, result.nbAvoirsOuverts());
        assertEquals(7_500, result.montantTotalAvoirsOuverts());
        assertEquals(1, result.nbAvoirsProchesExpiration());
        verify(retourClientRepository).produitsEnAlerte(debut, fin, 5);
        verify(retourClientRepository).clientsEnAlerte(debut, fin, 3);
    }

    @Test
    void getStats_MoisNullEtAgregatsNull_RetourneDesZeros() {
        YearMonth moisCourant = YearMonth.now();
        LocalDateTime debut = moisCourant.atDay(1).atStartOfDay();
        LocalDateTime fin = moisCourant.atEndOfMonth().plusDays(1).atStartOfDay();

        when(retourClientRepository.statsGlobales(debut, fin))
            .thenReturn(List.<Object[]>of(new Object[]{null, null}));
        when(retourClientRepository.statsParMotif(debut, fin)).thenReturn(List.of());
        when(retourClientRepository.produitsEnAlerte(debut, fin, 5)).thenReturn(List.of());
        when(retourClientRepository.clientsEnAlerte(debut, fin, 3)).thenReturn(List.of());
        when(avoirClientRepository.statsAvoirsOuverts()).thenReturn(List.<Object[]>of(new Object[]{null, null}));
        when(avoirClientRepository.countAvoirsProchesExpiration(LocalDate.now().plusDays(7)))
            .thenReturn(0L);

        RetourAvoirStatsDTO result = service.getStats(null);

        assertEquals(0, result.nbRetoursMois());
        assertEquals(0, result.montantTotalRetoursMois());
        assertEquals(0, result.nbAvoirsOuverts());
        assertEquals(0, result.montantTotalAvoirsOuverts());
        assertEquals(0, result.nbAvoirsProchesExpiration());
        assertEquals(List.of(), result.statsParMotif());
        assertEquals(List.of(), result.produitsEnAlerte());
        assertEquals(List.of(), result.clientsEnAlerte());
    }
}

