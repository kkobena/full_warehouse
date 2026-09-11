package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.reglement.service.ReglementGroupeFactureService} sur un vrai
 * PostgreSQL.
 *
 * <p>Un groupe d'organismes règle d'un seul virement les factures de tous ses adhérents. L'écriture
 * qui en résulte est un arbre : un paiement porteur marqué {@code grouped}, et un paiement fils par
 * facture fille, relié au porteur par un identifiant composite {@code (parent_id,
 * parent_transaction_date)}. Ce lien-là ne se vérifie qu'en base — et sans lui, l'annulation du
 * règlement ne retrouverait pas ses fils.
 */
@DisplayName("ReglementGroupeFactureService — règlement total d'une facture de groupe sur PostgreSQL")
class ReglementGroupeFactureServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Le règlement du groupe solde chaque facture fille et relie les paiements à leur porteur")
    void reglementDuGroupe() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE SANTE");
        FactureTiersPayant premiere = factureDe(groupe, "ADHERENT A", 30_000);
        FactureTiersPayant seconde = factureDe(groupe, "ADHERENT B", 70_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(premiere, seconde));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementGroupeFactureService.doReglement(parametre(factureGroupe, 100_000));
        viderLeCache();

        InvoicePayment porteur = em.find(InvoicePayment.class, reponse.id());
        assertTrue(porteur.isGrouped(), "le paiement porteur est marqué groupé");
        assertEquals(100_000, porteur.getPaidAmount());
        assertEquals(100_000, porteur.getMontantVerse());
        assertEquals(
            2,
            compter("SELECT count(*) FROM payment_transaction WHERE parent_id = " + reponse.id().getId()),
            "un paiement fils par facture fille"
        );

        FactureTiersPayant premiereRelue = em.find(FactureTiersPayant.class, premiere.getId());
        assertEquals(30_000, premiereRelue.getMontantRegle());
        assertEquals(InvoiceStatut.PAID, premiereRelue.getStatut(), "la fille soldée change de statut, pas seulement de montant");
        FactureTiersPayant secondeRelue = em.find(FactureTiersPayant.class, seconde.getId());
        assertEquals(70_000, secondeRelue.getMontantRegle());
        assertEquals(InvoiceStatut.PAID, secondeRelue.getStatut());
        FactureTiersPayant groupeRelu = em.find(FactureTiersPayant.class, factureGroupe.getId());
        assertEquals(100_000, groupeRelu.getMontantRegle());
        assertEquals(InvoiceStatut.PAID, groupeRelu.getStatut());
        assertTrue(reponse.total());
    }

    @Test
    @DisplayName("Les dossiers des factures filles sont soldés un à un")
    void dossiersDesFilles() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE DOSSIERS");
        TiersPayant organisme = tiersPayant("ADHERENT C", groupe);
        var compte = compte(organisme);
        ThirdPartySaleLine premier = dossier(compte, 15_000);
        ThirdPartySaleLine second = dossier(compte, 5_000);
        FactureTiersPayant fille = facture(organisme, List.of(premier, second));
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(fille));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementGroupeFactureService.doReglement(parametre(factureGroupe, 20_000));
        viderLeCache();

        assertEquals(ThirdPartySaleStatut.PAID, em.find(ThirdPartySaleLine.class, premier.getId()).getStatut());
        assertEquals(ThirdPartySaleStatut.PAID, em.find(ThirdPartySaleLine.class, second.getId()).getStatut());
        assertEquals(
            2,
            compter(
                "SELECT count(*) FROM invoice_payment_item i JOIN payment_transaction p ON p.id = i.invoice_payment_id" +
                " AND p.transaction_date = i.invoice_payment_transaction_date WHERE p.parent_id = " +
                reponse.id().getId()
            ),
            "les détails sont portés par les paiements fils, pas par le porteur"
        );
        assertNotNull(em.find(InvoicePayment.class, reponse.id()).getTransactionNumber());
    }

    @Test
    @DisplayName("Une facture fille déjà entamée est soldée, pas ramenée à son seul reliquat")
    void filleDejaEntamee() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE ENTAME");
        TiersPayant organisme = tiersPayant("ADHERENT N", groupe);
        ThirdPartySaleLine dossier = dossier(compte(organisme), 50_000);
        dossier.setMontantRegle(20_000);
        dossier.setStatut(ThirdPartySaleStatut.HALF_PAID);
        FactureTiersPayant fille = facture(organisme, List.of(dossier));
        fille.setMontantRegle(20_000);
        fille.setStatut(InvoiceStatut.PARTIALLY_PAID);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(fille));
        viderLeCache();

        services.reglementGroupeFactureService.doReglement(parametre(factureGroupe, 30_000));
        viderLeCache();

        FactureTiersPayant relue = em.find(FactureTiersPayant.class, fille.getId());
        assertEquals(50_000, relue.getMontantRegle(), "20 000 déjà réglés + 30 000 de reliquat");
        // Le montant facturé se reconstitue sur le montant des dossiers, pas sur le reliquat
        // encaissé : le comparer aux 30 000 versés laisserait la facture réglée mais partielle.
        assertEquals(InvoiceStatut.PAID, relue.getStatut());
    }

    @Test
    @DisplayName("Un versement inférieur au total attendu est refusé avant toute écriture")
    void versementInsuffisant() {
        GroupeTiersPayant groupe = groupe("GROUPE COURT");
        FactureTiersPayant fille = factureDe(groupe, "ADHERENT D", 50_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(fille));
        viderLeCache();

        ReglementParam param = parametre(factureGroupe, 50_000).setAmount(30_000);

        assertThrows(PaymentAmountException.class, () -> services.reglementGroupeFactureService.doReglement(param));
        assertEquals(0, compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'InvoicePayment'"));
        assertEquals(InvoiceStatut.NOT_PAID, em.find(FactureTiersPayant.class, fille.getId()).getStatut());
    }

    private FactureTiersPayant factureDe(GroupeTiersPayant groupe, String nom, int montant) {
        TiersPayant organisme = tiersPayant(nom, groupe);
        return facture(organisme, List.of(dossier(compte(organisme), montant)));
    }

    private ReglementParam parametre(FactureTiersPayant factureGroupe, int montant) {
        return new ReglementParam()
            .setId(factureGroupe.getId())
            .setMode(ModeEditionReglement.GROUPE_TOTAL)
            .setModePaimentCode(ModePaimentCode.VIREMENT)
            .setAmount(montant)
            .setTotalAmount(montant)
            .setMontantFacture(montant);
    }
}
