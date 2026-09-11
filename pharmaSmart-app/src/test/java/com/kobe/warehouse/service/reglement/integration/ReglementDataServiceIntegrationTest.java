package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.reglement.service.ReglementDataService} sur un vrai PostgreSQL.
 *
 * <p>Ce service relit et annule des règlements déjà écrits : chaque test commence donc par en
 * produire un pour de bon, via le service de règlement correspondant, plutôt que d'inventer des
 * lignes à la main — c'est le seul moyen d'éprouver l'annulation sur exactement ce que la
 * production écrit.
 *
 * <p>L'annulation est l'inverse exact du règlement, et la symétrie porte sur trois compteurs
 * distincts : le réglé de chaque dossier, celui de sa facture, et — pour un règlement groupé —
 * celui de la facture de groupe. Un seul de ces trois oublié et les statuts recalculés deviennent
 * faux ; c'est une régression que seule une base sait montrer.
 */
@DisplayName("ReglementDataService — relecture et annulation des règlements sur PostgreSQL")
class ReglementDataServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Annuler un règlement rend la facture et ses dossiers à leur état d'avant")
    void annulationDUnReglement() throws Exception {
        TiersPayant organisme = tiersPayant("CNAM ANNUL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 45_000);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        viderLeCache();
        ResponseReglementDTO reglement = services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 45_000));
        viderLeCache();

        services.reglementDataService.deleteReglement(reglement.id());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM payment_transaction WHERE id = " + reglement.id().getId()));
        assertEquals(
            0,
            compter("SELECT count(*) FROM invoice_payment_item WHERE invoice_payment_id = " + reglement.id().getId()),
            "les détails partent avec le paiement"
        );
        ThirdPartySaleLine dossierRelu = em.find(ThirdPartySaleLine.class, dossier.getId());
        assertEquals(0, dossierRelu.getMontantRegle());
        assertEquals(ThirdPartySaleStatut.ACTIF, dossierRelu.getStatut());
        FactureTiersPayant factureRelue = em.find(FactureTiersPayant.class, facture.getId());
        assertEquals(0, factureRelue.getMontantRegle());
        assertEquals(InvoiceStatut.NOT_PAID, factureRelue.getStatut());
    }

    @Test
    @DisplayName("Annuler un règlement partiel ramène le dossier à moitié réglé à son reliquat")
    void annulationDUnReglementPartiel() throws Exception {
        TiersPayant organisme = tiersPayant("CNPS ANNUL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 30_000);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        viderLeCache();
        ResponseReglementDTO premier = services.reglementFactureSelectionneesService.doReglement(
            new ReglementParam()
                .setId(facture.getId())
                .setMode(ModeEditionReglement.FACTURE_PARTIEL)
                .setModePaimentCode(ModePaimentCode.CASH)
                .setAmount(10_000)
                .setTotalAmount(30_000)
                .setMontantFacture(30_000)
                .setDossierIds(List.of(dossier.getId().getId()))
        );
        viderLeCache();
        ResponseReglementDTO second = services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 20_000));
        viderLeCache();

        services.reglementDataService.deleteReglement(second.id());
        viderLeCache();

        ThirdPartySaleLine dossierRelu = em.find(ThirdPartySaleLine.class, dossier.getId());
        assertEquals(10_000, dossierRelu.getMontantRegle(), "le premier règlement subsiste");
        assertEquals(ThirdPartySaleStatut.HALF_PAID, dossierRelu.getStatut());
        FactureTiersPayant factureRelue = em.find(FactureTiersPayant.class, facture.getId());
        assertEquals(10_000, factureRelue.getMontantRegle());
        assertEquals(InvoiceStatut.PARTIALLY_PAID, factureRelue.getStatut());
        assertEquals(1, compter("SELECT count(*) FROM payment_transaction WHERE id = " + premier.id().getId()));
    }

    @Test
    @DisplayName("Annuler un règlement groupé décrémente aussi la facture de groupe")
    void annulationDUnReglementGroupe() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE ANNUL");
        FactureTiersPayant premiere = factureDe(groupe, "ADHERENT J", 25_000);
        FactureTiersPayant seconde = factureDe(groupe, "ADHERENT K", 35_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(premiere, seconde));
        viderLeCache();
        ResponseReglementDTO reglement = services.reglementGroupeFactureService.doReglement(
            parametreTotal(factureGroupe, 60_000).setMode(ModeEditionReglement.GROUPE_TOTAL)
        );
        viderLeCache();

        services.reglementDataService.deleteReglement(reglement.id());
        viderLeCache();

        assertEquals(
            0,
            compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'InvoicePayment'"),
            "le porteur et ses fils disparaissent ensemble"
        );
        FactureTiersPayant groupeRelu = em.find(FactureTiersPayant.class, factureGroupe.getId());
        assertEquals(0, groupeRelu.getMontantRegle(), "la facture de groupe est décrémentée du montant annulé");
        assertEquals(InvoiceStatut.NOT_PAID, groupeRelu.getStatut());
        assertEquals(InvoiceStatut.NOT_PAID, em.find(FactureTiersPayant.class, premiere.getId()).getStatut());
        assertEquals(InvoiceStatut.NOT_PAID, em.find(FactureTiersPayant.class, seconde.getId()).getStatut());
    }

    @Test
    @DisplayName("Plusieurs règlements s'annulent en un appel")
    void annulationEnLot() throws Exception {
        TiersPayant organisme = tiersPayant("LOT");
        ClientTiersPayant compte = compte(organisme);
        FactureTiersPayant premiere = facture(organisme, List.of(dossier(compte, 10_000)));
        FactureTiersPayant seconde = facture(organisme, List.of(dossier(compte, 20_000)));
        viderLeCache();
        ResponseReglementDTO r1 = services.reglementFactureModeAllService.doReglement(parametreTotal(premiere, 10_000));
        ResponseReglementDTO r2 = services.reglementFactureModeAllService.doReglement(parametreTotal(seconde, 20_000));
        viderLeCache();

        services.reglementDataService.deleteReglement(Set.of(r1.id(), r2.id()));
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'InvoicePayment'"));
        assertEquals(InvoiceStatut.NOT_PAID, em.find(FactureTiersPayant.class, premiere.getId()).getStatut());
        assertEquals(InvoiceStatut.NOT_PAID, em.find(FactureTiersPayant.class, seconde.getId()).getStatut());
    }

    @Test
    @DisplayName("Les règlements se relisent par période et par organisme")
    void relectureParPeriodeEtOrganisme() throws Exception {
        TiersPayant recherche = tiersPayant("CNAM RECHERCHE");
        TiersPayant autre = tiersPayant("MUGEF RECHERCHE");
        FactureTiersPayant factureRecherchee = facture(recherche, List.of(dossier(compte(recherche), 11_000)));
        FactureTiersPayant factureAutre = facture(autre, List.of(dossier(compte(autre), 22_000)));
        viderLeCache();
        services.reglementFactureModeAllService.doReglement(parametreTotal(factureRecherchee, 11_000));
        services.reglementFactureModeAllService.doReglement(parametreTotal(factureAutre, 22_000));
        viderLeCache();

        List<InvoicePaymentDTO> tous = services.reglementDataService.fetchInvoicesPayments(
            new InvoicePaymentParam(null, null, LocalDate.now(), LocalDate.now(), false)
        );
        List<InvoicePaymentDTO> filtres = services.reglementDataService.fetchInvoicesPayments(
            new InvoicePaymentParam(null, recherche.getId(), LocalDate.now(), LocalDate.now(), false)
        );

        assertEquals(2, tous.size());
        assertEquals(1, filtres.size());
        assertEquals(recherche.getName(), filtres.getFirst().getOrganisme());
        assertEquals(factureRecherchee.getNumFacture(), filtres.getFirst().getCodeFacture());
    }

    @Test
    @DisplayName("La recherche par nom d'organisme n'attrape que le sien")
    void relectureParRecherche() throws Exception {
        TiersPayant organisme = tiersPayant("ZENITH");
        TiersPayant autre = tiersPayant("ALPHA");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 9_000)));
        facture(autre, List.of(dossier(compte(autre), 8_000)));
        viderLeCache();
        services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 9_000));
        viderLeCache();

        List<InvoicePaymentDTO> resultats = services.reglementDataService.fetchInvoicesPayments(
            new InvoicePaymentParam("ZENITH", null, LocalDate.now(), LocalDate.now(), false)
        );

        assertEquals(1, resultats.size());
        assertTrue(resultats.getFirst().getOrganisme().startsWith("ZENITH"));
    }

    @Test
    @DisplayName("Les règlements d'une facture se relisent par son identifiant composite")
    void relectureParFacture() throws Exception {
        TiersPayant organisme = tiersPayant("PAR FACTURE");
        ClientTiersPayant compte = compte(organisme);
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte, 40_000)));
        facture(organisme, List.of(dossier(compte, 15_000)));
        viderLeCache();
        services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 40_000));
        viderLeCache();

        List<InvoicePaymentDTO> reglements = services.reglementDataService.findByInvoice(facture.getId());

        assertEquals(1, reglements.size());
        assertEquals(facture.getNumFacture(), reglements.getFirst().getCodeFacture());
    }

    @Test
    @DisplayName("Le détail d'un règlement rend un élément par dossier réglé")
    void detailDUnReglement() throws Exception {
        TiersPayant organisme = tiersPayant("DETAIL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine premier = dossier(compte, 6_000);
        ThirdPartySaleLine second = dossier(compte, 4_000);
        FactureTiersPayant facture = facture(organisme, List.of(premier, second));
        viderLeCache();
        ResponseReglementDTO reglement = services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 10_000));
        viderLeCache();

        List<InvoicePaymentItemDTO> details = services.reglementDataService.getInvoicePaymentsItems(reglement.id());

        assertEquals(2, details.size());
        assertTrue(
            details.stream().anyMatch(d -> d.getNumBon().equals(premier.getNumBon())),
            "chaque détail porte le numéro de bon de son dossier"
        );
    }

    @Test
    @DisplayName("Le détail d'un règlement groupé rend les paiements de chaque facture fille")
    void detailDUnReglementGroupe() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE DETAIL");
        FactureTiersPayant premiere = factureDe(groupe, "ADHERENT L", 12_000);
        FactureTiersPayant seconde = factureDe(groupe, "ADHERENT M", 18_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(premiere, seconde));
        viderLeCache();
        ResponseReglementDTO reglement = services.reglementGroupeFactureService.doReglement(
            parametreTotal(factureGroupe, 30_000).setMode(ModeEditionReglement.GROUPE_TOTAL)
        );
        viderLeCache();

        List<InvoicePaymentDTO> fils = services.reglementDataService.getInvoicePaymentsGroupItems(reglement.id());

        assertEquals(2, fils.size());
        assertEquals(30_000, fils.stream().mapToInt(InvoicePaymentDTO::getTotalAmount).sum());
    }

    private FactureTiersPayant factureDe(GroupeTiersPayant groupe, String nom, int montant) {
        TiersPayant organisme = tiersPayant(nom, groupe);
        return facture(organisme, List.of(dossier(compte(organisme), montant)));
    }

    private ReglementParam parametreTotal(FactureTiersPayant facture, int montant) {
        return new ReglementParam()
            .setId(facture.getId())
            .setMode(ModeEditionReglement.FACTURE_TOTAL)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setAmount(montant)
            .setTotalAmount(montant)
            .setMontantFacture(montant);
    }
}
