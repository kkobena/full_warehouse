package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.service.dto.Consommation;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.impl.ConsommationService} sur un vrai PostgreSQL.
 *
 * <p>La consommation d'un assuré n'est pas une table : c'est un ensemble d'objets rangé dans une
 * colonne {@code jsonb}, un cumul par mois. Le service décide du cumul, mais c'est le passage par
 * Postgres qui dit si l'ensemble revient entier — identifiant, mois, année, montant — et si le
 * cumul du mois suivant s'ajoute sans écraser le précédent.
 */
@DisplayName("ConsommationService — cumuls mensuels en jsonb sur PostgreSQL")
class ConsommationServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Le premier montant crée le cumul du mois et le relit tel quel")
    void premierCumul() {
        ClientTiersPayant compte = compte("CNAM CONSO A", 80, PrioriteTiersPayant.R0);
        LocalDateTime maintenant = LocalDateTime.now();

        services.consommationService.updateConsommation(compte, 12_000, maintenant, c -> services.clientTiersPayantRepository.save(c));
        viderLeCache();

        ClientTiersPayant relu = em.find(ClientTiersPayant.class, compte.getId());
        assertEquals(1, relu.getConsommations().size());
        Consommation cumul = relu.getConsommations().iterator().next();
        assertEquals(12_000L, cumul.getConsommation());
        assertEquals(maintenant.getMonthValue(), cumul.getMonth());
        assertEquals(maintenant.getYear(), cumul.getYear());
        assertEquals(12_000, relu.getConsoMensuelle(), "le raccourci mensuel suit le cumul");
    }

    @Test
    @DisplayName("Un second montant du même mois s'ajoute au cumul existant")
    void cumulDuMemeMois() {
        ClientTiersPayant compte = compte("CNAM CONSO B", 80, PrioriteTiersPayant.R0);
        LocalDateTime maintenant = LocalDateTime.now();

        services.consommationService.updateConsommation(compte, 5_000, maintenant, c -> services.clientTiersPayantRepository.save(c));
        viderLeCache();
        services.consommationService.updateConsommation(
            em.find(ClientTiersPayant.class, compte.getId()),
            3_000,
            maintenant,
            c -> services.clientTiersPayantRepository.save(c)
        );
        viderLeCache();

        ClientTiersPayant relu = em.find(ClientTiersPayant.class, compte.getId());
        assertEquals(1, relu.getConsommations().size(), "un seul cumul par mois");
        assertEquals(8_000L, relu.getConsommations().iterator().next().getConsommation());
        assertEquals(8_000, relu.getConsoMensuelle());
    }

    @Test
    @DisplayName("Un montant d'un autre mois ouvre un second cumul sans effacer le premier")
    void cumulDunAutreMois() {
        ClientTiersPayant compte = compte("CNAM CONSO C", 80, PrioriteTiersPayant.R0);
        LocalDateTime maintenant = LocalDateTime.now();

        services.consommationService.updateConsommation(compte, 4_000, maintenant, c -> services.clientTiersPayantRepository.save(c));
        viderLeCache();
        services.consommationService.updateConsommation(
            em.find(ClientTiersPayant.class, compte.getId()),
            6_000,
            maintenant.minusMonths(1),
            c -> services.clientTiersPayantRepository.save(c)
        );
        viderLeCache();

        ClientTiersPayant relu = em.find(ClientTiersPayant.class, compte.getId());
        assertEquals(2, relu.getConsommations().size(), "chaque mois garde son propre cumul");
        assertTrue(relu.getConsommations().stream().anyMatch(c -> c.getConsommation() == 4_000L));
        assertTrue(relu.getConsommations().stream().anyMatch(c -> c.getConsommation() == 6_000L));
        assertEquals(10_000, relu.getConsoMensuelle(), "le raccourci, lui, additionne tout ce qui est passé");
    }

    @Test
    @DisplayName("Un montant nul ne touche ni au cumul ni à la ligne en base")
    void montantNul() {
        ClientTiersPayant compte = compte("CNAM CONSO D", 80, PrioriteTiersPayant.R0);

        services.consommationService.updateConsommation(compte, null, LocalDateTime.now(), c ->
            services.clientTiersPayantRepository.save(c)
        );
        viderLeCache();

        assertTrue(em.find(ClientTiersPayant.class, compte.getId()).getConsommations().isEmpty());
    }

    @Test
    @DisplayName("L'organisme cumule sur la même mécanique que le compte de l'assuré")
    void cumulDeLorganisme() {
        TiersPayant organisme = tiersPayant("CNAM CONSO ORG");
        LocalDateTime maintenant = LocalDateTime.now();

        services.consommationService.updateConsommation(organisme, 20_000, maintenant, t -> services.tiersPayantRepository.save(t));
        viderLeCache();

        TiersPayant relu = em.find(TiersPayant.class, organisme.getId());
        assertEquals(1, relu.getConsommations().size());
        assertEquals(20_000L, relu.getConsommations().iterator().next().getConsommation());
    }
}
