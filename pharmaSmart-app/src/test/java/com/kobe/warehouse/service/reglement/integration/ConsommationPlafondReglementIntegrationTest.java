package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plafond absolu : le règlement réduit la consommation, son annulation la rend. */
@DisplayName("Consommation face au plafond — règlement")
class ConsommationPlafondReglementIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Plafond absolu : le règlement réduit la consommation de l'adhérent et de l'organisme")
    void reglementReduit() throws Exception {
        TiersPayant organisme = organisme("REGL ABS", true);
        ClientTiersPayant compte = compteAvecConso(organisme);
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte, 45_000)));
        viderLeCache();

        services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 45_000));
        viderLeCache();

        assertEquals(5_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(5_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    @Test
    @DisplayName("Annuler le règlement rend la consommation")
    void annulationRend() throws Exception {
        TiersPayant organisme = organisme("REGL ANN", true);
        ClientTiersPayant compte = compteAvecConso(organisme);
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte, 45_000)));
        viderLeCache();
        ResponseReglementDTO reglement = services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 45_000));
        viderLeCache();

        services.reglementDataService.deleteReglement(reglement.id());
        viderLeCache();

        assertEquals(50_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(50_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    @Test
    @DisplayName("Plafond non absolu : le règlement ne touche pas à la consommation")
    void nonAbsoluSansEffet() throws Exception {
        TiersPayant organisme = organisme("REGL NA", false);
        ClientTiersPayant compte = compteAvecConso(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 45_000);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        viderLeCache();

        services.reglementFactureModeAllService.doReglement(parametreTotal(facture, 45_000));
        viderLeCache();

        assertEquals(50_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(50_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    private TiersPayant organisme(String nom, boolean absolu) {
        TiersPayant organisme = tiersPayant(nom);
        organisme.setPlafondAbsolu(absolu);
        organisme.setPlafondAbsoluClient(absolu);
        organisme.setConsoMensuelle(50_000L);
        em.flush();
        return organisme;
    }

    private ClientTiersPayant compteAvecConso(TiersPayant organisme) {
        ClientTiersPayant compte = compte(organisme);
        compte.setConsoMensuelle(50_000L);
        em.flush();
        return compte;
    }

    private static ReglementParam parametreTotal(FactureTiersPayant facture, int montant) {
        return new ReglementParam()
            .setId(facture.getId())
            .setMode(ModeEditionReglement.FACTURE_TOTAL)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setAmount(montant)
            .setTotalAmount(montant)
            .setMontantFacture(montant);
    }
}
