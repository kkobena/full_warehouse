package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.repository.ObjectifPilotageRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Objectifs du pilotage : la table de V2.1.38 se lit avec l'auteur de la modification. */
@DisplayName("Pilotage — objectifs sur la base")
class ObjectifsPilotageIntegrationTest extends AbstractReportIntegrationTest {

    @Test
    @DisplayName("un objectif enregistré se relit par année, avec son auteur")
    void enregistrerEtRelire() {
        ObjectifPilotageRepository repository = IntegrationPostgresDatabase.bean(ObjectifPilotageRepository.class);
        repository.save(
            new ObjectifPilotage()
                .setIndicateur(IndicateurPilotage.CA_TTC)
                .setAnnee(2091)
                .setMois(3)
                .setValeur(1_500_000)
                .setModifiePar(utilisateur)
                .setModifieLe(LocalDateTime.of(2091, 1, 5, 9, 0))
        );
        em.flush();
        em.clear();

        assertThat(repository.listerParAnnee(2091)).singleElement().satisfies(objectif -> {
            assertThat(objectif.getMois()).isEqualTo(3);
            assertThat(objectif.getValeur()).isEqualTo(1_500_000);
            assertThat(objectif.getModifiePar().getId()).isEqualTo(utilisateur.getId());
        });
        assertThat(repository.findAllByAnneeAndIndicateur(2091, IndicateurPilotage.TAUX_REMISE)).isEmpty();
    }
}
