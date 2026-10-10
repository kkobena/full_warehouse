package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.pilotage.AgregatsPilotageService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tient les agrégats du pilotage à jour pendant que l'application tourne : les postes d'officine sont éteints la nuit,
 * une tâche nocturne ne passerait pas. Toute modification se fait application allumée, d'où la règle : recalculer les
 * jours modifiés récemment, et rattraper au démarrage.
 */
@Component
public class AgregatsPilotageScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AgregatsPilotageScheduler.class);

    /** Le double de l'intervalle par défaut : un passage manqué est rattrapé par le suivant. */
    private static final Duration FENETRE_ACTUALISATION = Duration.ofMinutes(30);
    private static final Duration FENETRE_RATTRAPAGE = Duration.ofDays(7);

    private final AgregatsPilotageService agregatsPilotageService;

    public AgregatsPilotageScheduler(AgregatsPilotageService agregatsPilotageService) {
        this.agregatsPilotageService = agregatsPilotageService;
    }

    @Async
    @Scheduled(cron = "${pharma-smart.pilotage.actualisation-cron:0 */15 * * * *}")
    public void actualiser() {
        actualiserDepuis(LocalDateTime.now().minus(FENETRE_ACTUALISATION));
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void rattraperAuDemarrage() {
        actualiserDepuis(LocalDateTime.now().minus(FENETRE_RATTRAPAGE));
    }

    private void actualiserDepuis(LocalDateTime depuis) {
        try {
            int jours = agregatsPilotageService.actualiserJoursModifiesDepuis(depuis);
            agregatsPilotageService.photographierStock(LocalDate.now());
            LOG.debug("Agrégats du pilotage : {} jour(s) recalculé(s), stock photographié", jours);
        } catch (RuntimeException e) {
            LOG.warn("Actualisation des agrégats du pilotage en échec", e);
        }
    }
}
