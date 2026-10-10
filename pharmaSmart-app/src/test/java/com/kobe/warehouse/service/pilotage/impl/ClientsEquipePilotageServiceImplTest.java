package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageClientsRepository;
import com.kobe.warehouse.service.dto.pilotage.ClienteleDTO;
import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pilotage — clients et équipe")
class ClientsEquipePilotageServiceImplTest {

    private static final PeriodeDTO SEPTEMBRE = new PeriodeDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    private static final PeriodeDTO SEPTEMBRE_N1 = new PeriodeDTO(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(SEPTEMBRE.du(), SEPTEMBRE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, false, null, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private PilotageClientsRepository repository;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @InjectMocks
    private ClientsEquipePilotageServiceImpl service;

    @BeforeEach
    void comparerSeptembreN1() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(SEPTEMBRE, SEPTEMBRE_N1, false));
    }

    @Test
    @DisplayName("part du CA avec un client identifié, CA moyen par client, clients qui reculent le plus")
    void clients() {
        when(repository.lireClientele(eq(SEPTEMBRE.du()), any(), any(), any())).thenReturn(new ClienteleDTO(50L, 200_000L, 80_000L));
        when(repository.lireClientele(eq(SEPTEMBRE_N1.du()), any(), any(), any())).thenReturn(new ClienteleDTO(40L, 150_000L, 75_000L));
        when(repository.sommerCaParClient(eq(SEPTEMBRE.du()), any(), any(), any())).thenReturn(List.of(new ComptageDTO("1", "Awa", 2L, 0L, 1_000L)));
        when(repository.sommerCaParClient(eq(SEPTEMBRE_N1.du()), any(), any(), any())).thenReturn(
            List.of(new ComptageDTO("1", "Awa", 3L, 0L, 4_000L), new ComptageDTO("2", "Koffi", 1L, 0L, 6_000L), new ComptageDTO("3", "Yao", 1L, 0L, 500L))
        );

        ClientsPilotageDTO clients = service.analyserClients(REQUETE);

        assertThat(clients.partCaIdentifie().valeur()).isCloseTo(40.0, within(1e-9));
        assertThat(clients.partCaIdentifie().ecart()).isCloseTo(-10.0, within(1e-9));
        assertThat(clients.caMoyenParClient().valeur()).isCloseTo(1_600.0, within(1e-9));
        assertThat(clients.enBaisse()).extracting(ContributionDTO::libelle, ContributionDTO::ecart).containsExactly(
            tuple("Koffi", -6_000L),
            tuple("Awa", -3_000L),
            tuple("Yao", -500L)
        );
    }

    @Test
    @DisplayName("équipe : articles par vente des lignes rapportés aux ventes des en-têtes, ordonnance, annulations, ligne d'équipe")
    void equipe() {
        MesuresComparees awaEntetes = entetes("7", "Awa", 10, 20_000, 1_000);
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.ENTETES), any(), eq(AxeAnalyse.VENDEUR), isNull(), anyList(), any())).thenReturn(
            new Ventilation(List.of(awaEntetes), awaEntetes)
        );
        MesuresComparees awaLignes = new MesuresComparees(new MembreAnalyseDTO("7", "Awa"), null, lignes(25, 18_000, 13_500), MesuresPilotage.AUCUNE);
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), any(), eq(AxeAnalyse.VENDEUR), isNull(), anyList(), any())).thenReturn(
            new Ventilation(List.of(awaLignes), awaLignes)
        );
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.ENTETES), any(), eq(AxeAnalyse.VENDEUR), eq(AxeAnalyse.TYPE_PRESCRIPTION), anyList(), any())).thenReturn(
            new Ventilation(
                List.of(
                    new MesuresComparees(new MembreAnalyseDTO("7", "Awa"), new MembreAnalyseDTO("PRESCRIPTION", "Ordonnance"), entetes("7", "Awa", 4, 9_000, 0).periode(), MesuresPilotage.AUCUNE),
                    new MesuresComparees(new MembreAnalyseDTO("7", "Awa"), new MembreAnalyseDTO("CONSEIL", "Conseil"), entetes("7", "Awa", 6, 11_000, 0).periode(), MesuresPilotage.AUCUNE)
                ),
                MesuresComparees.AUCUNES
            )
        );
        when(repository.compterAnnulationsParVendeur(eq(SEPTEMBRE.du()), any(), any())).thenReturn(List.of(new ComptageDTO("7", "", 2L, 0L, 3_000L)));
        when(repository.compterAvoirsParVendeur(any(), any())).thenReturn(List.of());

        EquipePilotageDTO equipe = service.analyserEquipe(REQUETE);

        LigneVendeurDTO awa = equipe.vendeurs().getFirst();
        assertThat(awa.articlesParVente().valeur()).isCloseTo(2.5, within(1e-9));
        assertThat(awa.tauxMarge().valeur()).isCloseTo(25.0, within(1e-9));
        assertThat(awa.tauxRemise().valeur()).isCloseTo(5.0, within(1e-9));
        assertThat(awa.partOrdonnance().valeur()).isCloseTo(40.0, within(1e-9));
        assertThat(awa.annulations().valeur()).isEqualTo(2);
        assertThat(equipe.equipe().libelle()).isEqualTo("Équipe");
        assertThat(equipe.equipe().annulations().valeur()).isEqualTo(2);
    }

    private static MesuresComparees entetes(String cle, String libelle, long ventes, long ca, long remises) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, new MesuresPilotage(ventes, 0, ca, ca, remises, 0, 0, 0, 0, 0, 0, 0, 0), MesuresPilotage.AUCUNE);
    }

    private static MesuresPilotage lignes(long quantite, long caHt, long coutHt) {
        return new MesuresPilotage(0, 0, caHt, caHt, 0, 0, caHt, caHt, coutHt, quantite, 0, 0, 0);
    }
}
