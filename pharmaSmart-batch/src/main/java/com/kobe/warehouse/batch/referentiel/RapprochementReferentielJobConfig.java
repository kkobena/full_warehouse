package com.kobe.warehouse.batch.referentiel;

import com.kobe.warehouse.service.referentiel.RapprochementProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Rapprochement des produits du catalogue avec le référentiel médicament (BDPM).
 *
 * <p>Deux étapes, deux jobs, chacun une simple tasklet : le calcul est une requête ensembliste
 * PostgreSQL ({@code ref_rapprocher_produits}, V2.1.20). Un découpage en chunks n'apporterait rien,
 * sinon un aller-retour par produit.
 *
 * <ul>
 *   <li>{@code rapprocherReferentielStep} — produits sans proposition. Étape du pipeline de nuit :
 *       coût négligeable, car seuls les produits nouveaux sont calculés.
 *   <li>{@code recalculerReferentielStep} — recalcule aussi les propositions EN_ATTENTE, à lancer
 *       (job {@code recalculReferentielJob}) après le rechargement du référentiel. Hors pipeline de
 *       nuit : tous les NON_TROUVE y repasseraient chaque nuit.
 * </ul>
 */
@Configuration
public class RapprochementReferentielJobConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RapprochementReferentielJobConfig.class);

    @Bean
    public Step rapprocherReferentielStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        RapprochementProduitService rapprochementProduitService
    ) {
        return new StepBuilder("rapprocherReferentielStep", jobRepository)
            .tasklet(
                (contribution, ctx) -> {
                    LOG.info("[REFERENTIEL] {} produit(s) rapproché(s)", rapprochementProduitService.rapprocherNouveauxProduits());
                    return RepeatStatus.FINISHED;
                },
                txManager
            )
            .build();
    }

    @Bean
    public Step recalculerReferentielStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        RapprochementProduitService rapprochementProduitService
    ) {
        return new StepBuilder("recalculerReferentielStep", jobRepository)
            .tasklet(
                (_, _) -> {
                    LOG.info("[REFERENTIEL] {} proposition(s) recalculée(s)", rapprochementProduitService.recalculerPropositions());
                    return RepeatStatus.FINISHED;
                },
                txManager
            )
            .build();
    }

    @Bean
    public Job rapprochementReferentielJob(JobRepository jobRepository, Step rapprocherReferentielStep) {
        return new JobBuilder("rapprochementReferentielJob", jobRepository).start(rapprocherReferentielStep).build();
    }

    @Bean
    public Job recalculReferentielJob(JobRepository jobRepository, Step recalculerReferentielStep) {
        return new JobBuilder("recalculReferentielJob", jobRepository).start(recalculerReferentielStep).build();
    }
}
