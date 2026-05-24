package com.kobe.warehouse.batch.classification;

import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class ClassificationStepListener implements StepExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClassificationStepListener.class);
    private static final String APP_LAST_DAY_CLASSIFICATION = "APP_LAST_DAY_CLASSIFICATION";

    private final AppConfigurationService appConfigurationService;

    public ClassificationStepListener(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            appConfigurationService.findOneById(APP_LAST_DAY_CLASSIFICATION)
                .ifPresent(cfg -> {
                    cfg.setValue(LocalDate.now().toString());
                    appConfigurationService.update(cfg);
                });
            LOG.info("[CLASSIFICATION] Garde mensuelle mise à jour : {}", LocalDate.now());
        }
        return stepExecution.getExitStatus();
    }
}
