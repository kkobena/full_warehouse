package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.repository.AvoirClientRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AvoirExpirationJob {

    private static final Logger LOG = LoggerFactory.getLogger(AvoirExpirationJob.class);

    private final AvoirClientRepository avoirClientRepository;

    public AvoirExpirationJob(AvoirClientRepository avoirClientRepository) {
        this.avoirClientRepository = avoirClientRepository;
    }

    @Transactional
    public void expireAvoirsEchus() {
        List<AvoirClient> expires = avoirClientRepository
            .findByStatutAndDateExpirationLessThanEqual(AvoirClientStatut.OUVERT, LocalDate.now());
        if (expires.isEmpty()) {
            return;
        }
        expires.forEach(a -> a.setStatut(AvoirClientStatut.EXPIRE));
        avoirClientRepository.saveAll(expires);
        LOG.info("[AVOIR-EXPIRATION] {} avoir(s) expiré(s)", expires.size());
    }
}
