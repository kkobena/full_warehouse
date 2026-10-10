package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.PilotageVue;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.repository.PilotageAnalyseRepository;
import com.kobe.warehouse.repository.PilotageDemarqueRepository;
import com.kobe.warehouse.repository.PilotageMesuresRepository;
import com.kobe.warehouse.repository.PilotageVenteJourRepository;
import com.kobe.warehouse.repository.PilotageVenteRepository;
import com.kobe.warehouse.repository.PilotageVentesDetailRepository;
import com.kobe.warehouse.repository.PilotageVueRepository;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteVentilationDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.VentesJourDTO;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Phase 1 du pilotage : les agrégats journaliers redisent ce que disent les tables source, et leur recalcul est
 * rejouable sans doublon.
 */
@DisplayName("Pilotage — agrégats journaliers et photographie du stock")
class AgregatsPilotageIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions des tables source du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);
    private static final int PRIX = 2_000;
    private static final int REMISE = 500;

    private PilotageAgregatRepository pilotageAgregatRepository;
    private Produit produit;

    @BeforeEach
    void constituerLaJournee() {
        pilotageAgregatRepository = IntegrationPostgresDatabase.bean(PilotageAgregatRepository.class);
        produit = produitAnalysable(unique("PILOTAGE"), PRIX);
        referencement(produit, fournisseur(unique("FRS PILOTAGE")));

        remiser(vendu(produit, 3, JOUR));
        importer(vendu(produit, 2, JOUR).getSales());
        vendu(produit, 1, JOUR, true, CategorieChiffreAffaire.CA);
        venteDepot(JOUR, 7_000);
    }

    @Test
    @DisplayName("l'agrégat des en-têtes donne le CA de référence")
    void lesEntetesSuiventLaReference() {
        recalculerLaJournee();

        assertThat(IntegrationPostgresDatabase.bean(PilotageVenteJourRepository.class).calculerChiffreAffairesOfficine(JOUR, JOUR))
            .isEqualTo(IntegrationPostgresDatabase.bean(PilotageVenteRepository.class).calculerChiffreAffairesOfficine(JOUR, JOUR));
    }

    @Test
    @DisplayName("les lignes comptent les ventes non annulées, coût sur la quantité demandée")
    void lesLignesComptentLesVentesNonAnnulees() {
        recalculerLaJournee();

        long[] lignes = sommes(
            "SELECT sum(quantite_demandee), sum(montant_ttc), sum(remise), sum(cout_ttc), sum(cout_ht) FROM pilotage_vente_ligne_jour " +
            "WHERE jour = :jour AND produit_id = :produit AND categorie_ca = 'CA'"
        );

        assertThat(lignes).containsExactly(5, 5L * PRIX, REMISE, 5L * produit.getCostAmount(), 5L * produit.getCostAmount());
    }

    @Test
    @DisplayName("l'annulation reste visible dans les en-têtes, marquée")
    void lAnnulationEstMarquee() {
        recalculerLaJournee();

        long[] annulees = sommes("SELECT sum(nb_ventes), sum(montant_ttc) FROM pilotage_vente_jour WHERE jour = :jour AND annulee");

        assertThat(annulees).containsExactly(1, PRIX);
    }

    @Test
    @DisplayName("le recalcul est rejouable sans doublon")
    void leRecalculEstRejouable() {
        recalculerLaJournee();
        long[] premierPassage = sommes("SELECT count(*), sum(montant_ttc) FROM pilotage_vente_jour WHERE jour = :jour");

        recalculerLaJournee();

        assertThat(sommes("SELECT count(*), sum(montant_ttc) FROM pilotage_vente_jour WHERE jour = :jour")).containsExactly(premierPassage);
    }

    @Test
    @DisplayName("les achats sont datés à la réception, au prix d'achat TTC")
    void lesAchatsSontDatesALaReception() {
        reception(fournisseur(unique("FRS RECEPTION")), JOUR.minusDays(3), JOUR, 60_000, 10, 8);
        recalculerLaJournee();

        long[] achats = sommes("SELECT sum(quantite_recue), sum(montant_ttc), sum(montant_ht), sum(nb_bons) FROM pilotage_achat_jour WHERE jour = :jour");

        assertThat(achats).containsExactly(8, 8L * 6_000, 8L * 6_000, 1);
    }

    @Test
    @DisplayName("les encaissements reprennent les règlements, par mode")
    void lesEncaissementsReprennentLesReglements() {
        CashRegister caisse = caisseOuverte();
        Sales vente = venteFermee(JOUR);
        reglement(vente, caisse, "CASH", 12_000);
        reglement(vente, caisse, "CB", 8_000);
        recalculerLaJournee();

        long[] especes = sommes("SELECT sum(montant), sum(nb_transactions) FROM pilotage_encaissement_jour WHERE jour = :jour AND mode_paiement = 'CASH'");
        long[] total = sommes("SELECT sum(montant) FROM pilotage_encaissement_jour WHERE jour = :jour");

        assertThat(especes).containsExactly(12_000, 1);
        assertThat(total).containsExactly(20_000);
    }

    @Test
    @DisplayName("la photographie range le stock au dernier jour du mois, aux prix de la valorisation")
    void laPhotographieRangeLeStockEnFinDeMois() {
        stock(produit, 25);
        pilotageAgregatRepository.photographierStock(JOUR);

        long[] photo = sommes(
            "SELECT sum(quantite), sum(valeur_achat), sum(valeur_vente), count(*) FROM pilotage_stock_mensuel " +
            "WHERE produit_id = :produit AND mois = '" + JOUR.withDayOfMonth(JOUR.lengthOfMonth()) + "'"
        );

        assertThat(photo).containsExactly(25, 25L * produit.getCostAmount(), 25L * PRIX, 1);
    }

    @Test
    @DisplayName("un jour ouvré est un jour avec ventes au CA de l'officine")
    void unJourOuvreEstUnJourAvecVentes() {
        venteDepot(JOUR.plusDays(1), 7_000);
        recalculer(JOUR.minusDays(1), JOUR.plusDays(1));

        long joursOuvres = IntegrationPostgresDatabase.bean(PilotageVenteJourRepository.class).compterJoursOuvres(JOUR.minusDays(1), JOUR.plusDays(1));

        assertThat(joursOuvres).as("la veille est sans vente, le lendemain n'a qu'une vente au dépôt").isEqualTo(1);
    }

    @Test
    @DisplayName("les mesures journalières lisent le CA de l'officine : annulée à part, dépôt exclu")
    void lesMesuresJournalieresLisentLAgregat() {
        recalculerLaJournee();
        PilotageMesuresRepository mesures = IntegrationPostgresDatabase.bean(PilotageMesuresRepository.class);

        VentesJourDTO ventes = mesures.listerVentesParJour(JOUR, JOUR, CategorieChiffreAffaire.officine()).getFirst();
        LignesJourDTO lignes = mesures.listerLignesParJour(JOUR, JOUR, CategorieChiffreAffaire.officine()).getFirst();

        assertThat(ventes.nbVentes()).isEqualTo(2);
        assertThat(ventes.nbVentesAnnulees()).isEqualTo(1);
        assertThat(ventes.caTtc()).isEqualTo(5L * PRIX);
        assertThat(lignes.quantiteServie()).isEqualTo(5);
        assertThat(lignes.coutHt()).isEqualTo(5L * produit.getCostAmount());
    }

    @Test
    @DisplayName("les ventilations rendent le CA du produit, de sa famille, du vendeur et du type de vente")
    void lesVentilationsRendentLeCa() {
        recalculerLaJournee();

        assertThat(ventiler(SourceAnalyse.LIGNES, AxeAnalyse.PRODUIT))
            .filteredOn(ligne -> ligne.cle1().equals(produit.getId().toString()))
            .extracting(MesuresVentileesDTO::caTtc, MesuresVentileesDTO::quantite)
            .containsExactly(tuple(5L * PRIX, 5L));
        assertThat(ventiler(SourceAnalyse.LIGNES, AxeAnalyse.FAMILLE)).extracting(MesuresVentileesDTO::libelle1).contains(produit.getFamille().getLibelle());
        assertThat(ventiler(SourceAnalyse.LIGNES, AxeAnalyse.VENDEUR)).extracting(MesuresVentileesDTO::cle1).contains(utilisateur.getId().toString());
        assertThat(ventiler(SourceAnalyse.LIGNES, AxeAnalyse.NATURE_VENTE)).extracting(MesuresVentileesDTO::cle1).contains("COMPTANT");
    }

    @Test
    @DisplayName("chaque axe se lit sur chaque agrégat qui le connaît, et redonne le même total")
    void chaqueAxeSeLit() {
        recalculerLaJournee();
        for (SourceAnalyse source : SourceAnalyse.values()) {
            long total = ventiler(source).stream().mapToLong(MesuresVentileesDTO::caTtc).sum();
            for (AxeAnalyse axe : AxeAnalyse.values()) {
                if (axe.estLuEnBase() && axe.getSources().contains(source)) {
                    assertThat(ventiler(source, axe).stream().mapToLong(MesuresVentileesDTO::caTtc).sum()).as("%s par %s", source, axe).isEqualTo(total);
                }
            }
        }
    }

    @Test
    @DisplayName("un filtre de descente restreint la ventilation ; les en-têtes comptent les ventes par heure")
    void lesFiltresEtLesEntetes() {
        recalculerLaJournee();
        String famille = produit.getFamille().getId().toString();

        assertThat(
            analyse().ventiler(requete(SourceAnalyse.LIGNES, List.of(AxeAnalyse.PRODUIT), List.of(new FiltreAnalyseDTO(AxeAnalyse.FAMILLE, List.of(famille)))))
        )
            .extracting(MesuresVentileesDTO::cle1)
            .containsOnly(produit.getId().toString());
        assertThat(ventiler(SourceAnalyse.ENTETES, AxeAnalyse.HEURE).stream().mapToLong(MesuresVentileesDTO::nbVentes).sum()).isEqualTo(2);
    }

    @Test
    @DisplayName("chaque vente porte son taux de remise et son mode d'octroi ; une tranche se filtre")
    void lesRemisesSontClassees() {
        recalculerLaJournee();

        // 500 de remise sur 3 × 2 000 : 8 %, accordée par privilège (aucune clé de sécurité saisie).
        assertThat(ventiler(SourceAnalyse.ENTETES, AxeAnalyse.REMISE)).extracting(MesuresVentileesDTO::cle1).contains("8", "0");
        assertThat(ventiler(SourceAnalyse.ENTETES, AxeAnalyse.OCTROI_REMISE))
            .filteredOn(ligne -> ligne.cle1().equals("PRIVILEGE"))
            .extracting(MesuresVentileesDTO::nbVentes, MesuresVentileesDTO::remise)
            .containsExactly(tuple(1L, (long) REMISE));
        assertThat(
            analyse().ventiler(
                requete(SourceAnalyse.LIGNES, List.of(AxeAnalyse.PRODUIT), List.of(new FiltreAnalyseDTO(AxeAnalyse.REMISE, List.of("5-9"))))
            )
        )
            .extracting(MesuresVentileesDTO::quantite)
            .containsExactly(3L);
    }

    @Test
    @DisplayName("les listes de ventes et la démarque se lisent sur la base")
    void lesListesDeVentesSeLisent() {
        PilotageVentesDetailRepository ventes = IntegrationPostgresDatabase.bean(PilotageVentesDetailRepository.class);
        var officine = CategorieChiffreAffaire.officine();

        assertThat(ventes.listerPlusFortesRemises(JOUR, JOUR, SalesStatut.CLOSED, officine, "PR_AJOUTER_REMISE_VENTE", PageRequest.of(0, 10)))
            .extracting(VenteRemiseeDTO::remise)
            .contains((long) REMISE);
        assertThat(ventes.listerVentesAMargeNegative(JOUR, JOUR, SalesStatut.CLOSED, officine, PageRequest.of(0, 10))).isNotNull();
        PilotageDemarqueRepository demarque = IntegrationPostgresDatabase.bean(PilotageDemarqueRepository.class);
        assertThat(demarque.sommerParMotif(JOUR.atStartOfDay(), JOUR.plusDays(1).atStartOfDay(), AjustementStatut.CLOSED, AjustType.AJUSTEMENT_OUT)).isNotNull();
        assertThat(
            demarque.listerProduitsLesPlusTouches(JOUR.atStartOfDay(), JOUR.plusDays(1).atStartOfDay(), AjustementStatut.CLOSED, AjustType.AJUSTEMENT_OUT, PageRequest.of(0, 5))
        ).isNotNull();
    }

    @Test
    @DisplayName("les vues livrées d'office sont visibles de tous ; une vue enregistrée garde ses indicateurs")
    void lesVuesEnregistrees() {
        PilotageVueRepository vues = IntegrationPostgresDatabase.bean(PilotageVueRepository.class);
        PilotageVue vue = vues.save(
            new PilotageVue()
                .setLibelle("Mes antalgiques")
                .setIndicateurs(List.of(IndicateurPilotage.CA_TTC, IndicateurPilotage.MARGE_BRUTE))
                .setAxe(AxeAnalyse.PRODUIT)
                .setNombreElements(20)
                .setTri(TriAnalyse.VALEUR)
                .setAffichage(AffichageAnalyse.TABLEAU)
                .setProprietaire(utilisateur)
        );
        em.flush();
        em.clear();

        List<PilotageVue> visibles = vues.listerVisibles(utilisateur.getId());
        assertThat(visibles).filteredOn(PilotageVue::estLivree).hasSize(9);
        assertThat(visibles.getLast().getId()).isEqualTo(vue.getId());
        assertThat(visibles.getLast().getIndicateurs()).containsExactly(IndicateurPilotage.CA_TTC, IndicateurPilotage.MARGE_BRUTE);
    }

    @Test
    @DisplayName("un jour dont une vente vient de changer est repéré pour recalcul")
    void unJourModifieEstRepere() {
        assertThat(pilotageAgregatRepository.listerJoursModifiesDepuis(LocalDateTime.now().minusMinutes(5))).contains(JOUR);
    }

    private List<MesuresVentileesDTO> ventiler(SourceAnalyse source, AxeAnalyse... axes) {
        return analyse().ventiler(requete(source, List.of(axes), List.of()));
    }

    private static RequeteVentilationDTO requete(SourceAnalyse source, List<AxeAnalyse> axes, List<FiltreAnalyseDTO> filtres) {
        return new RequeteVentilationDTO(JOUR, JOUR, source, axes, false, filtres, CategorieChiffreAffaire.officine());
    }

    private static PilotageAnalyseRepository analyse() {
        return IntegrationPostgresDatabase.bean(PilotageAnalyseRepository.class);
    }

    private void recalculerLaJournee() {
        recalculer(JOUR, JOUR);
    }

    private void recalculer(LocalDate du, LocalDate au) {
        em.flush();
        pilotageAgregatRepository.recalculer(du, au);
    }

    private void remiser(SalesLine ligne) {
        ligne.setDiscountAmount(REMISE);
        ligne.getSales().setDiscountAmount(REMISE);
        em.flush();
    }

    private void importer(Sales vente) {
        vente.setImported(true);
        em.flush();
    }

    private long[] sommes(String sql) {
        var requete = em.createNativeQuery(sql);
        if (sql.contains(":jour")) {
            requete.setParameter("jour", JOUR);
        }
        if (sql.contains(":produit")) {
            requete.setParameter("produit", produit.getId());
        }
        Object resultat = requete.getSingleResult();
        Object[] colonnes = resultat instanceof Object[] tableau ? tableau : new Object[] { resultat };
        long[] valeurs = new long[colonnes.length];
        for (int i = 0; i < colonnes.length; i++) {
            valeurs[i] = colonnes[i] == null ? 0 : ((Number) colonnes[i]).longValue();
        }
        return valeurs;
    }
}
