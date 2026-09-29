package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plafond non absolu : la facture définitive remet la consommation à zéro, son annulation la rend. */
@DisplayName("Consommation face au plafond — facturation")
class ConsommationPlafondFacturationIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("Plafond non absolu : la facture définitive remet à zéro l'adhérent et l'organisme")
    void definitiveRemetAZero() throws Exception {
        TiersPayant organisme = organisme("CONSO NA", false);
        ClientTiersPayant compte = compteAvecConso(organisme, 30_000L);
        dossier(compte, 30_000);
        viderLeCache();

        services.editionAllService.createFactureEdition(parametres(false));
        viderLeCache();

        assertEquals(0L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(0L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    @Test
    @DisplayName("Une facture provisoire ne touche pas à la consommation")
    void provisoireSansEffet() throws Exception {
        TiersPayant organisme = organisme("CONSO PROV", false);
        ClientTiersPayant compte = compteAvecConso(organisme, 30_000L);
        dossier(compte, 30_000);
        viderLeCache();

        services.editionAllService.createFactureEdition(parametres(true));
        viderLeCache();

        assertEquals(30_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(30_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    @Test
    @DisplayName("Plafond absolu : la facture définitive laisse la consommation, seul le règlement la réduit")
    void absoluNonRemisAZero() throws Exception {
        TiersPayant organisme = organisme("CONSO ABS", true);
        ClientTiersPayant compte = compteAvecConso(organisme, 30_000L);
        dossier(compte, 30_000);
        viderLeCache();

        services.editionAllService.createFactureEdition(parametres(false));
        viderLeCache();

        assertEquals(30_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(30_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
    }

    @Test
    @DisplayName("Annuler la facture rend la consommation qu'elle avait effacée")
    void annulationRendLaConsommation() throws Exception {
        TiersPayant organisme = organisme("CONSO ANN", false);
        ClientTiersPayant compte = compteAvecConso(organisme, 30_000L);
        dossier(compte, 30_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(false));
        viderLeCache();
        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        services.editionDataService.deleteFacture(facture.getId());
        viderLeCache();

        assertEquals(30_000L, em.find(ClientTiersPayant.class, compte.getId()).getConsoMensuelle());
        assertEquals(30_000L, em.find(TiersPayant.class, organisme.getId()).getConsoMensuelle());
        assertEquals(0, compter("SELECT count(*) FROM reinitialisation_consommation"), "la trace est consommée");
    }

    private TiersPayant organisme(String nom, boolean absolu) {
        TiersPayant organisme = tiersPayant(nom);
        organisme.setPlafondAbsolu(absolu);
        organisme.setPlafondAbsoluClient(absolu);
        organisme.setConsoMensuelle(30_000L);
        em.flush();
        return organisme;
    }

    private ClientTiersPayant compteAvecConso(TiersPayant organisme, long conso) {
        ClientTiersPayant compte = compte(organisme);
        compte.setConsoMensuelle(conso);
        em.flush();
        return compte;
    }

    private static EditionSearchParams parametres(boolean provisoire) {
        return new EditionSearchParams(
            null,
            ModeEditionEnum.ALL,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Set.of(),
            Set.of(),
            Set.of(),
            true,
            Set.of(),
            provisoire,
            OrigineGeneration.MANUELLE
        );
    }
}
