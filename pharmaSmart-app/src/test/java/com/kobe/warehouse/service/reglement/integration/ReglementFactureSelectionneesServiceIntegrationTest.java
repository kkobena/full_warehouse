package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.reglement.service.ReglementFactureSelectionneesService} sur un
 * vrai PostgreSQL.
 *
 * <p>Ici l'organisme ne règle qu'une partie des dossiers d'une facture, et le montant versé
 * s'impute dossier par dossier jusqu'à épuisement. Deux choses ne s'observent qu'en base : la
 * sélection elle-même, qui passe par une spécification Criteria sur les identifiants des dossiers,
 * et le fait que les dossiers <em>non</em> sélectionnés restent intacts — c'est la garantie que
 * l'imputation n'a pas débordé.
 */
@DisplayName("ReglementFactureSelectionneesService — règlement partiel par dossiers sur PostgreSQL")
class ReglementFactureSelectionneesServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Seuls les dossiers sélectionnés sont réglés, les autres restent intacts")
    void seulsLesDossiersSelectionnes() throws Exception {
        TiersPayant organisme = tiersPayant("CNAM SEL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine premier = dossier(compte, 30_000);
        ThirdPartySaleLine second = dossier(compte, 20_000);
        ThirdPartySaleLine horsSelection = dossier(compte, 50_000);
        FactureTiersPayant facture = facture(organisme, List.of(premier, second, horsSelection));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureSelectionneesService.doReglement(
            parametre(facture, 50_000, 50_000, 100_000).setDossierIds(List.of(premier.getId().getId(), second.getId().getId()))
        );
        viderLeCache();

        assertEquals(ThirdPartySaleStatut.PAID, em.find(ThirdPartySaleLine.class, premier.getId()).getStatut());
        assertEquals(ThirdPartySaleStatut.PAID, em.find(ThirdPartySaleLine.class, second.getId()).getStatut());
        ThirdPartySaleLine intact = em.find(ThirdPartySaleLine.class, horsSelection.getId());
        assertEquals(ThirdPartySaleStatut.ACTIF, intact.getStatut());
        assertEquals(0, intact.getMontantRegle(), "le dossier non sélectionné n'a rien encaissé");

        FactureTiersPayant relue = em.find(FactureTiersPayant.class, facture.getId());
        assertEquals(50_000, relue.getMontantRegle());
        assertEquals(InvoiceStatut.PARTIALLY_PAID, relue.getStatut(), "il reste 50 000 à facturer");
        assertFalse(reponse.total());
    }

    @Test
    @DisplayName("Un versement inférieur au dû laisse le dossier à moitié réglé")
    void versementPartielSurUnDossier() throws Exception {
        TiersPayant organisme = tiersPayant("MUGEF SEL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 40_000);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureSelectionneesService.doReglement(
            parametre(facture, 15_000, 40_000, 40_000).setDossierIds(List.of(dossier.getId().getId()))
        );
        viderLeCache();

        ThirdPartySaleLine relu = em.find(ThirdPartySaleLine.class, dossier.getId());
        assertEquals(15_000, relu.getMontantRegle());
        assertEquals(ThirdPartySaleStatut.HALF_PAID, relu.getStatut());
        assertEquals(InvoiceStatut.PARTIALLY_PAID, em.find(FactureTiersPayant.class, facture.getId()).getStatut());

        InvoicePayment paiement = em.find(InvoicePayment.class, reponse.id());
        assertEquals(15_000, paiement.getPaidAmount());
        assertEquals(40_000, paiement.getExpectedAmount(), "le montant attendu reste celui des dossiers sélectionnés");
    }

    @Test
    @DisplayName("Le trop-versé n'est pas imputé au-delà du dû des dossiers sélectionnés")
    void tropVerse() throws Exception {
        TiersPayant organisme = tiersPayant("CNPS SEL");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 10_000);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureSelectionneesService.doReglement(
            parametre(facture, 18_000, 10_000, 10_000).setDossierIds(List.of(dossier.getId().getId()))
        );
        viderLeCache();

        assertEquals(10_000, em.find(ThirdPartySaleLine.class, dossier.getId()).getMontantRegle());
        InvoicePayment paiement = em.find(InvoicePayment.class, reponse.id());
        assertEquals(18_000, paiement.getMontantVerse(), "le versé est conservé tel quel");
        assertEquals(10_000, paiement.getPaidAmount(), "seul le dû est imputé");
        assertTrue(reponse.total());
    }

    @Test
    @DisplayName("Sans dossier sélectionné, le règlement est refusé avant toute écriture")
    void aucunDossierSelectionne() {
        TiersPayant organisme = tiersPayant("VIDE");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 5_000)));
        viderLeCache();

        ReglementParam param = parametre(facture, 5_000, 5_000, 5_000);

        assertThrows(GenericError.class, () -> services.reglementFactureSelectionneesService.doReglement(param));
        assertEquals(0, compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'InvoicePayment'"));
    }

    /**
     * {@code totalAttendu} est la somme due par les dossiers sélectionnés, {@code montantFacture}
     * celle de la facture entière : c'est leur écart qui décide du statut laissé à la facture.
     */
    private ReglementParam parametre(FactureTiersPayant facture, int montantVerse, int totalAttendu, int montantFacture) {
        return new ReglementParam()
            .setId(facture.getId())
            .setMode(ModeEditionReglement.FACTURE_PARTIEL)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setAmount(montantVerse)
            .setTotalAmount(totalAttendu)
            .setMontantFacture(montantFacture);
    }
}
