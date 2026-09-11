package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.facturation.dto.AvoirCommand;
import com.kobe.warehouse.service.facturation.dto.AvoirDto;
import com.kobe.warehouse.service.facturation.dto.AvoirLineDto;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.facturation.service.AvoirService} sur un vrai PostgreSQL.
 *
 * <p>Un avoir suit une machine à états — brouillon, émis, puis imputé ou annulé — et l'imputation
 * est la seule étape qui ait un effet comptable : elle se comporte comme un règlement sans
 * encaissement, augmente le montant réglé de la facture visée et la fait passer à soldée. Ce
 * couplage entre l'avoir et la facture ne se vérifie qu'en base, tout comme la numérotation
 * annuelle, qui lit le plus grand numéro déjà émis pour l'année.
 */
@DisplayName("AvoirService — avoirs tiers payant sur PostgreSQL")
class AvoirServiceIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("Un avoir naît en brouillon avec un numéro annuel et ses lignes")
    void creationDUnAvoir() {
        TiersPayant organisme = tiersPayant("CNAM AVOIR");
        // Une ligne d'avoir désigne le dossier rejeté : la base exige qu'il existe pour de bon.
        ThirdPartySaleLine dossierRejete = dossier(compte(organisme), 20_000);
        FactureTiersPayant facture = factureAvecMontant(organisme, 120_000);
        viderLeCache();

        AvoirDto cree = services.avoirService.creerAvoir(
            new AvoirCommand(
                facture.getId().getId(),
                facture.getId().getInvoiceDate(),
                organisme.getId(),
                BigDecimal.valueOf(20_000),
                BigDecimal.valueOf(3_600),
                BigDecimal.valueOf(16_400),
                "bons rejetés",
                List.of(
                    new AvoirLineDto(
                        null,
                        dossierRejete.getId().getId(),
                        dossierRejete.getId().getSaleDate(),
                        BigDecimal.valueOf(20_000),
                        "hors nomenclature"
                    )
                )
            )
        );
        viderLeCache();

        AvoirTiersPayant relu = em.find(AvoirTiersPayant.class, cree.id());
        assertEquals(AvoirStatut.DRAFT, relu.getStatut());
        assertEquals("AV-" + Year.now().getValue() + "_0001", relu.getNumAvoir(), "premier avoir de l'année");
        assertEquals(0, BigDecimal.valueOf(20_000).compareTo(relu.getMontantAvoir()));
        assertEquals(1, relu.getLignes().size(), "les lignes suivent l'avoir en cascade");
        assertEquals("bons rejetés", relu.getMotif());
        assertNotNull(relu.getCreated(), "l'horodatage est posé à la persistance");
    }

    @Test
    @DisplayName("Le numéro d'avoir s'incrémente sur l'année en cours")
    void numerotationAnnuelle() {
        FactureTiersPayant facture = factureAvecMontant("NUM AVOIR", 80_000);
        viderLeCache();
        services.avoirService.creerAvoir(commande(facture, 10_000));
        viderLeCache();

        AvoirDto second = services.avoirService.creerAvoir(commande(facture, 5_000));
        viderLeCache();

        assertEquals("AV-" + Year.now().getValue() + "_0002", em.find(AvoirTiersPayant.class, second.id()).getNumAvoir());
    }

    @Test
    @DisplayName("Un avoir sans montant est ramené à zéro plutôt que laissé nul")
    void avoirSansMontant() {
        FactureTiersPayant facture = factureAvecMontant("SANS MONTANT", 50_000);
        viderLeCache();

        AvoirDto cree = services.avoirService.creerAvoir(
            new AvoirCommand(
                facture.getId().getId(),
                facture.getId().getInvoiceDate(),
                facture.getTiersPayant().getId(),
                null,
                null,
                null,
                "à chiffrer",
                List.of()
            )
        );
        viderLeCache();

        AvoirTiersPayant relu = em.find(AvoirTiersPayant.class, cree.id());
        assertEquals(0, BigDecimal.ZERO.compareTo(relu.getMontantAvoir()));
        assertEquals(0, BigDecimal.ZERO.compareTo(relu.getMontantTva()));
    }

    @Test
    @DisplayName("Émettre fait passer l'avoir du brouillon à émis")
    void emissionDUnAvoir() {
        AvoirDto brouillon = avoirBrouillon("EMISSION", 100_000, 15_000);
        viderLeCache();

        AvoirDto emis = services.avoirService.emettre(brouillon.id());
        viderLeCache();

        assertEquals(AvoirStatut.EMIS, emis.statut());
        assertEquals(AvoirStatut.EMIS, em.find(AvoirTiersPayant.class, brouillon.id()).getStatut());
    }

    @Test
    @DisplayName("Un avoir déjà émis ne se réémet pas")
    void doubleEmission() {
        AvoirDto brouillon = avoirBrouillon("DOUBLE", 100_000, 10_000);
        services.avoirService.emettre(brouillon.id());
        viderLeCache();

        assertThrows(IllegalStateException.class, () -> services.avoirService.emettre(brouillon.id()));
    }

    @Test
    @DisplayName("Imputer un avoir réduit ce que la facture visée reste à devoir")
    void imputationPartielle() {
        AvoirDto brouillon = avoirBrouillon("IMPUTATION", 100_000, 30_000);
        services.avoirService.emettre(brouillon.id());
        FactureTiersPayant cible = em.find(AvoirTiersPayant.class, brouillon.id()).getFactureTiersPayant();
        viderLeCache();

        services.avoirService.imputer(brouillon.id(), cible.getId().getId(), cible.getId().getInvoiceDate());
        viderLeCache();

        FactureTiersPayant relue = em.find(FactureTiersPayant.class, cible.getId());
        assertEquals(30_000, relue.getMontantRegle(), "l'avoir s'impute comme un règlement sans encaissement");
        assertEquals(InvoiceStatut.PARTIALLY_PAID, relue.getStatut(), "il reste 70 000 à recouvrer");

        AvoirTiersPayant avoir = em.find(AvoirTiersPayant.class, brouillon.id());
        assertEquals(AvoirStatut.IMPUTE, avoir.getStatut());
        assertEquals(cible.getId(), avoir.getFactureImputation().getId(), "la facture visée est conservée sur l'avoir");
    }

    @Test
    @DisplayName("Un avoir couvrant la totalité solde la facture")
    void imputationTotale() {
        AvoirDto brouillon = avoirBrouillon("SOLDE", 50_000, 50_000);
        services.avoirService.emettre(brouillon.id());
        FactureTiersPayant cible = em.find(AvoirTiersPayant.class, brouillon.id()).getFactureTiersPayant();
        viderLeCache();

        services.avoirService.imputer(brouillon.id(), cible.getId().getId(), cible.getId().getInvoiceDate());
        viderLeCache();

        assertEquals(InvoiceStatut.PAID, em.find(FactureTiersPayant.class, cible.getId()).getStatut());
    }

    @Test
    @DisplayName("Un avoir s'impute sur une autre facture que celle qui l'a motivé")
    void imputationSurUneAutreFacture() {
        TiersPayant organisme = tiersPayant("REPORT");
        FactureTiersPayant origine = factureAvecMontant(organisme, 40_000);
        FactureTiersPayant autre = factureAvecMontant(organisme, 90_000);
        viderLeCache();
        AvoirDto brouillon = services.avoirService.creerAvoir(commande(origine, 25_000));
        services.avoirService.emettre(brouillon.id());
        viderLeCache();

        services.avoirService.imputer(brouillon.id(), autre.getId().getId(), autre.getId().getInvoiceDate());
        viderLeCache();

        assertEquals(25_000, em.find(FactureTiersPayant.class, autre.getId()).getMontantRegle());
        assertEquals(0, em.find(FactureTiersPayant.class, origine.getId()).getMontantRegle(), "la facture d'origine n'est pas touchée");
    }

    @Test
    @DisplayName("Un avoir encore en brouillon ne peut pas être imputé")
    void imputationDUnBrouillon() {
        AvoirDto brouillon = avoirBrouillon("BROUILLON", 60_000, 10_000);
        FactureTiersPayant cible = em.find(AvoirTiersPayant.class, brouillon.id()).getFactureTiersPayant();
        var factureId = cible.getId();
        viderLeCache();

        assertThrows(
            IllegalStateException.class,
            () -> services.avoirService.imputer(brouillon.id(), factureId.getId(), factureId.getInvoiceDate())
        );
        assertEquals(0, em.find(FactureTiersPayant.class, factureId).getMontantRegle(), "la facture reste intacte");
    }

    @Test
    @DisplayName("Annuler un avoir en consigne le motif")
    void annulation() {
        AvoirDto brouillon = avoirBrouillon("ANNULE", 40_000, 8_000);
        viderLeCache();

        services.avoirService.annuler(brouillon.id(), "saisi par erreur");
        viderLeCache();

        AvoirTiersPayant relu = em.find(AvoirTiersPayant.class, brouillon.id());
        assertEquals(AvoirStatut.ANNULE, relu.getStatut());
        assertEquals("saisi par erreur", relu.getMotif());
    }

    @Test
    @DisplayName("Annuler sans motif laisse celui de la création")
    void annulationSansMotif() {
        AvoirDto brouillon = avoirBrouillon("MOTIF", 40_000, 8_000);
        viderLeCache();

        services.avoirService.annuler(brouillon.id(), "  ");
        viderLeCache();

        assertEquals("bons rejetés", em.find(AvoirTiersPayant.class, brouillon.id()).getMotif());
    }

    @Test
    @DisplayName("Un avoir déjà imputé ne s'annule pas : son effet comptable est passé")
    void annulationDUnAvoirImpute() {
        AvoirDto brouillon = avoirBrouillon("IMPUTE ANNUL", 40_000, 10_000);
        services.avoirService.emettre(brouillon.id());
        FactureTiersPayant cible = em.find(AvoirTiersPayant.class, brouillon.id()).getFactureTiersPayant();
        services.avoirService.imputer(brouillon.id(), cible.getId().getId(), cible.getId().getInvoiceDate());
        viderLeCache();

        assertThrows(IllegalStateException.class, () -> services.avoirService.annuler(brouillon.id(), "trop tard"));
    }

    @Test
    @DisplayName("Un avoir déjà annulé ne s'annule pas deux fois")
    void doubleAnnulation() {
        AvoirDto brouillon = avoirBrouillon("DOUBLE ANNUL", 40_000, 10_000);
        services.avoirService.annuler(brouillon.id(), "erreur");
        viderLeCache();

        assertThrows(IllegalStateException.class, () -> services.avoirService.annuler(brouillon.id(), "encore"));
    }

    @Test
    @DisplayName("La recherche filtre par organisme et par statut")
    void rechercheParOrganismeEtStatut() {
        TiersPayant vise = tiersPayant("RECHERCHE A");
        TiersPayant autre = tiersPayant("RECHERCHE B");
        FactureTiersPayant factureVisee = factureAvecMontant(vise, 50_000);
        viderLeCache();
        AvoirDto emis = services.avoirService.creerAvoir(commande(factureVisee, 10_000));
        services.avoirService.emettre(emis.id());
        services.avoirService.creerAvoir(commande(factureVisee, 5_000));
        services.avoirService.creerAvoir(commande(factureAvecMontant(autre, 30_000), 4_000));
        viderLeCache();

        List<AvoirDto> emisDuVise = services.avoirService
            .findAll(new AvoirSearchParams(vise.getId(), null, null, List.of(AvoirStatut.EMIS), null), page())
            .getContent();
        List<AvoirDto> tousDuVise = services.avoirService
            .findAll(new AvoirSearchParams(vise.getId(), null, null, List.of(), null), page())
            .getContent();

        assertEquals(1, emisDuVise.size());
        assertEquals(emis.id(), emisDuVise.getFirst().id());
        assertEquals(2, tousDuVise.size(), "sans statut, tous les statuts sont retenus");
        assertTrue(tousDuVise.stream().allMatch(a -> a.tiersPayantId().equals(vise.getId())));
    }

    @Test
    @DisplayName("La recherche par numéro d'avoir est insensible à la casse")
    void rechercheParNumero() {
        FactureTiersPayant facture = factureAvecMontant("NUM RECHERCHE", 50_000);
        viderLeCache();
        AvoirDto avoir = services.avoirService.creerAvoir(commande(facture, 10_000));
        viderLeCache();

        List<AvoirDto> resultats = services.avoirService
            .findAll(new AvoirSearchParams(null, null, null, List.of(), avoir.numAvoir().toLowerCase()), page())
            .getContent();

        assertEquals(1, resultats.size());
        assertEquals(avoir.numAvoir(), resultats.getFirst().numAvoir());
    }

    @Test
    @DisplayName("Un avoir hors de la période cherchée ne remonte pas")
    void rechercheHorsPeriode() {
        FactureTiersPayant facture = factureAvecMontant("PERIODE AVOIR", 50_000);
        viderLeCache();
        services.avoirService.creerAvoir(commande(facture, 10_000));
        viderLeCache();

        List<AvoirDto> resultats = services.avoirService
            .findAll(
                new AvoirSearchParams(null, LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2), List.of(), null),
                page()
            )
            .getContent();

        assertTrue(resultats.isEmpty(), "l'avoir est daté d'aujourd'hui");
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private FactureTiersPayant factureAvecMontant(String nomOrganisme, int montantTtc) {
        return factureAvecMontant(tiersPayant(nomOrganisme), montantTtc);
    }

    private FactureTiersPayant factureAvecMontant(TiersPayant organisme, int montantTtc) {
        FactureTiersPayant facture = facture(organisme, false, List.of());
        facture.setMontantTtc(BigDecimal.valueOf(montantTtc));
        facture.setMontantNet(BigDecimal.valueOf(montantTtc));
        em.flush();
        return facture;
    }

    private AvoirCommand commande(FactureTiersPayant facture, int montant) {
        return new AvoirCommand(
            facture.getId().getId(),
            facture.getId().getInvoiceDate(),
            facture.getTiersPayant().getId(),
            BigDecimal.valueOf(montant),
            BigDecimal.ZERO,
            BigDecimal.valueOf(montant),
            "bons rejetés",
            List.of()
        );
    }

    private AvoirDto avoirBrouillon(String nomOrganisme, int montantFacture, int montantAvoir) {
        FactureTiersPayant facture = factureAvecMontant(nomOrganisme, montantFacture);
        viderLeCache();
        return services.avoirService.creerAvoir(commande(em.find(FactureTiersPayant.class, facture.getId()), montantAvoir));
    }
}
