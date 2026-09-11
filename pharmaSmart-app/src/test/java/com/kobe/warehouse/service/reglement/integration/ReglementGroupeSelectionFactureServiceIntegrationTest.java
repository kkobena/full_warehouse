package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reglement.dto.LigneSelectionnesDTO;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.reglement.service.ReglementGroupeSelectionFactureService} sur un
 * vrai PostgreSQL.
 *
 * <p>Variante partielle du règlement de groupe : le groupe verse une somme et désigne, facture par
 * facture, ce qu'il entend couvrir. Le service impute dans l'ordre de la sélection et s'arrête dès
 * que le versement est épuisé — les factures suivantes ne doivent alors porter aucune trace du
 * règlement, ce que seule la relecture en base établit.
 */
@DisplayName("ReglementGroupeSelectionFactureService — règlement partiel d'un groupe sur PostgreSQL")
class ReglementGroupeSelectionFactureServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Le versement s'impute facture par facture dans l'ordre de la sélection")
    void impuationParFacture() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE PARTIEL");
        FactureTiersPayant soldee = factureDe(groupe, "ADHERENT E", 40_000);
        FactureTiersPayant entamee = factureDe(groupe, "ADHERENT F", 60_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(soldee, entamee));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementGroupeSelectionFactureService.doReglement(
            parametre(factureGroupe, 70_000, 100_000).setLigneSelectionnes(
                List.of(ligne(soldee, 40_000), ligne(entamee, 60_000))
            )
        );
        viderLeCache();

        assertEquals(InvoiceStatut.PAID, em.find(FactureTiersPayant.class, soldee.getId()).getStatut());
        FactureTiersPayant relueEntamee = em.find(FactureTiersPayant.class, entamee.getId());
        assertEquals(30_000, relueEntamee.getMontantRegle(), "les 70 000 versés couvrent 40 000 puis 30 000");
        assertEquals(InvoiceStatut.PARTIALLY_PAID, relueEntamee.getStatut());

        FactureTiersPayant groupeRelu = em.find(FactureTiersPayant.class, factureGroupe.getId());
        assertEquals(70_000, groupeRelu.getMontantRegle());
        assertEquals(InvoiceStatut.PARTIALLY_PAID, groupeRelu.getStatut());
        assertFalse(reponse.total());

        InvoicePayment porteur = em.find(InvoicePayment.class, reponse.id());
        assertTrue(porteur.isGrouped());
        assertEquals(100_000, porteur.getExpectedAmount());
        assertEquals(70_000, porteur.getPaidAmount());
        assertEquals(2, compter("SELECT count(*) FROM payment_transaction WHERE parent_id = " + reponse.id().getId()));
    }

    @Test
    @DisplayName("Une fois le versement épuisé, les factures suivantes ne sont pas touchées")
    void versementEpuise() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE EPUISE");
        FactureTiersPayant servie = factureDe(groupe, "ADHERENT G", 20_000);
        TiersPayant organismeIntact = tiersPayant("ADHERENT H", groupe);
        ThirdPartySaleLine dossierIntact = dossier(compte(organismeIntact), 35_000);
        FactureTiersPayant intacte = facture(organismeIntact, List.of(dossierIntact));
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(servie, intacte));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementGroupeSelectionFactureService.doReglement(
            parametre(factureGroupe, 20_000, 55_000).setLigneSelectionnes(
                List.of(ligne(servie, 20_000), ligne(intacte, 35_000))
            )
        );
        viderLeCache();

        assertEquals(InvoiceStatut.PAID, em.find(FactureTiersPayant.class, servie.getId()).getStatut());
        FactureTiersPayant relueIntacte = em.find(FactureTiersPayant.class, intacte.getId());
        assertEquals(0, relueIntacte.getMontantRegle());
        assertEquals(InvoiceStatut.NOT_PAID, relueIntacte.getStatut());
        assertEquals(ThirdPartySaleStatut.ACTIF, em.find(ThirdPartySaleLine.class, dossierIntact.getId()).getStatut());
        assertEquals(
            1,
            compter("SELECT count(*) FROM payment_transaction WHERE parent_id = " + reponse.id().getId()),
            "aucun paiement fils n'est créé pour la facture non servie"
        );
    }

    @Test
    @DisplayName("Sans ligne sélectionnée, le règlement est refusé avant toute écriture")
    void aucuneLigneSelectionnee() {
        GroupeTiersPayant groupe = groupe("GROUPE VIDE");
        FactureTiersPayant fille = factureDe(groupe, "ADHERENT I", 10_000);
        FactureTiersPayant factureGroupe = factureGroupe(groupe, List.of(fille));
        viderLeCache();

        ReglementParam param = parametre(factureGroupe, 10_000, 10_000);

        assertThrows(GenericError.class, () -> services.reglementGroupeSelectionFactureService.doReglement(param));
        assertEquals(0, compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'InvoicePayment'"));
    }

    private FactureTiersPayant factureDe(GroupeTiersPayant groupe, String nom, int montant) {
        TiersPayant organisme = tiersPayant(nom, groupe);
        return facture(organisme, List.of(dossier(compte(organisme), montant)));
    }

    private LigneSelectionnesDTO ligne(FactureTiersPayant facture, int montant) {
        return new LigneSelectionnesDTO()
            .setId(facture.getId())
            .setMontantVerse(montant)
            .setMontantAttendu(montant)
            .setMontantFacture(montant);
    }

    private ReglementParam parametre(FactureTiersPayant factureGroupe, int montantVerse, int montantFacture) {
        return new ReglementParam()
            .setId(factureGroupe.getId())
            .setMode(ModeEditionReglement.GROUPE_PARTIEL)
            .setModePaimentCode(ModePaimentCode.VIREMENT)
            .setAmount(montantVerse)
            .setTotalAmount(montantFacture)
            .setMontantFacture(montantFacture);
    }
}
