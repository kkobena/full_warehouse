package com.kobe.warehouse.service.semois;

import com.kobe.warehouse.service.classification.ClassificationCriticiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Coordinateur de démarrage SEMOIS.
 *
 * <p>Séquence au démarrage (asynchrone, ne bloque pas l'application) :
 * <ol>
 *   <li>Classification mensuelle — si déjà exécutée ce mois, skippée immédiatement.</li>
 *   <li>Si la classification s'est effectivement exécutée, elle déclenche elle-même le
 *       recalcul SEMOIS via {@link SemoisCalculationService#recalculateAfterClassification()}.
 *   </li>
 *   <li>Ensuite, {@link SemoisCalculationService#recalculateAllConfigurations()} est appelé
 *       inconditionnellement : sa garde idempotente (APP_LAST_DAY_SEMOIS_CALCULATION) évite
 *       un double recalcul si SEMOIS vient d'être exécuté à l'étape 2 ; sinon il s'exécute
 *       (cas : classification déjà faite ce mois mais SEMOIS pas encore fait aujourd'hui).
 *   </li>
 * </ol>
 *
 * <p>Les schedulers horaires ({@code 0 0 8-19 * * *}) de chaque service constituent un
 * filet de sécurité pour les machines qui ne tournent pas 24/24 ; leurs gardes idempotentes
 * empêchent les doubles exécutions.
 */
@Component
public class SemoisStartupCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisStartupCoordinator.class);

    private final ClassificationCriticiteService classificationService;
    private final SemoisCalculationService semoisCalculationService;

    public SemoisStartupCoordinator(
        ClassificationCriticiteService classificationService,
        SemoisCalculationService semoisCalculationService
    ) {
        this.classificationService = classificationService;
        this.semoisCalculationService = semoisCalculationService;
    }

    /**
     * Déclenché dès que l'application est prête.
     * Exécuté dans le pool {@code taskExecutor} (cf. AsyncConfiguration) pour ne pas
     * bloquer le thread principal ni retarder la disponibilité des endpoints HTTP.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        LOG.info("Démarrage séquence classification → SEMOIS (async)...");
        try {
            // 1. Classification (mensuelle)
            //    → si elle s'exécute, elle appelle recalculateAfterClassification() en fin
            //    → si déjà faite ce mois, retour immédiat sans toucher SEMOIS
            classificationService.reclassifierTousProduits();

            // 2. SEMOIS (quotidien)
            //    → guard idempotente : skip si déjà calculé aujourd'hui
            //      (cas normal après recalculateAfterClassification())
            //    → s'exécute si classification était déjà faite ce mois mais SEMOIS pas fait aujourd'hui
            semoisCalculationService.recalculateAllConfigurations();

        } catch (Exception e) {
            LOG.error("Erreur lors de la séquence classification → SEMOIS au démarrage", e);
        }
    }
}
