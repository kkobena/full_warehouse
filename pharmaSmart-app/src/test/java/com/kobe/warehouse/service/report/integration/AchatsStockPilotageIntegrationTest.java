package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.PilotageAchatsStockRepository;
import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.repository.PilotageRupturesRepository;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/** Onglet « Achats & stock » : chaque requête se lit sur la base, et le coût des ventes est celui des lignes. */
@DisplayName("Pilotage — achats, stock, ruptures sur la base")
class AchatsStockPilotageIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions des tables source du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);
    private static final List<OrderStatut> RECUES = List.of(OrderStatut.RECEIVED, OrderStatut.CLOSED);

    private PilotageAchatsStockRepository achatsStock;
    private PilotageRupturesRepository ruptures;
    private Produit produit;

    @BeforeEach
    void vendreEtPhotographier() {
        achatsStock = IntegrationPostgresDatabase.bean(PilotageAchatsStockRepository.class);
        ruptures = IntegrationPostgresDatabase.bean(PilotageRupturesRepository.class);
        produit = produitAnalysable(unique("ACHATS"), 2_000);
        referencement(produit, fournisseur(unique("FRS ACHATS")));
        vendu(produit, 2, JOUR);
        em.flush();
        PilotageAgregatRepository agregats = IntegrationPostgresDatabase.bean(PilotageAgregatRepository.class);
        agregats.recalculer(JOUR, JOUR);
        agregats.photographierStock(JOUR);
    }

    @Test
    @DisplayName("achats, délais, coût des ventes, photographies et dormants se lisent")
    void achatsEtStock() {
        LocalDate fin = JOUR.withDayOfMonth(JOUR.lengthOfMonth());
        var officine = CategorieChiffreAffaire.officine();

        assertThat(achatsStock.sommerAchatsParFournisseur(JOUR, JOUR, RECUES)).isNotNull();
        assertThat(achatsStock.listerDelaisParFournisseur(JOUR, JOUR, RECUES)).isNotNull();
        assertThat(achatsStock.sommerAchatsHtParJour(JOUR, JOUR)).isNotNull();
        assertThat(achatsStock.sommerAchatsParFamille(JOUR, JOUR)).isNotNull();
        long cout = achatsStock.sommerCoutVentesTtc(JOUR, JOUR, officine);
        assertThat(achatsStock.sommerCoutVentesParFamille(JOUR, JOUR, officine)).extracting(MontantsFamilleDTO::montantTtc).contains(cout);
        // Le produit du test est entré en stock après janvier : la photographie de janvier peut être vide.
        assertThat(achatsStock.sommerStockParMois(fin, fin)).isNotNull();
        assertThat(achatsStock.sommerStockParFamille(fin)).isNotNull();
        assertThat(achatsStock.sommerDormantsParFamille(fin, JOUR.plusDays(1))).isNotNull();
        assertThat(achatsStock.listerDormants(fin, JOUR.plusDays(1), PageRequest.of(0, 5))).isNotNull();
    }

    @Test
    @DisplayName("ruptures fournisseurs, ventes manquées et péremptions se lisent")
    void rupturesEtPeremptions() {
        var debut = JOUR.atStartOfDay();
        var fin = JOUR.plusDays(1).atStartOfDay();

        assertThat(ruptures.compterLignesCommandees(JOUR, JOUR)).isNotNegative();
        assertThat(ruptures.compterRuptures(debut, fin)).isNotNegative();
        assertThat(ruptures.sommerRupturesParFournisseur(debut, fin)).isNotNull();
        assertThat(ruptures.sommerRupturesParProduit(debut, fin, PageRequest.of(0, 5))).isNotNull();
        assertThat(ruptures.sommerVentesManqueesParProduit(debut, fin, PageRequest.of(0, 5))).isNotNull();
        assertThat(ruptures.sommerVentesManquees(debut, fin).nombre()).isNotNegative();
        ComptageDTO quantites = ruptures.sommerQuantitesEnAvoir(JOUR, JOUR, CategorieChiffreAffaire.officine());
        assertThat(quantites.nombre()).isEqualTo(2);
        assertThat(ruptures.sommerPeremptionsParMois(JOUR, JOUR.plusYears(1))).isNotNull();
    }
}
