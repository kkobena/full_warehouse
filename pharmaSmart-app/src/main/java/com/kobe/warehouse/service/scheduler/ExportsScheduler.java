package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.exports.ExportsFichiersService;
import com.kobe.warehouse.service.exports.ModelesExportService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Exports programmés et expiration des fichiers. Les postes sont éteints la nuit : un modèle dû pendant l'arrêt part au premier
 * passage qui suit le démarrage (sa prochaine exécution est dans le passé).
 */
@Component
public class ExportsScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ExportsScheduler.class);

    private final ModelesExportService modelesExportService;
    private final ExportsFichiersService exportsFichiersService;

    public ExportsScheduler(ModelesExportService modelesExportService, ExportsFichiersService exportsFichiersService) {
        this.modelesExportService = modelesExportService;
        this.exportsFichiersService = exportsFichiersService;
    }

    @Scheduled(cron = "${pharma-smart.exports.programmation-cron:0 */15 * * * *}")
    public void executerEtPurger() {
        LocalDateTime maintenant = LocalDateTime.now();
        try {
            modelesExportService.executerProgrammes(maintenant);
            exportsFichiersService.purgerExpires(maintenant);
        } catch (RuntimeException e) {
            LOG.warn("Exports programmés ou purge en échec", e);
        }
    }
}
