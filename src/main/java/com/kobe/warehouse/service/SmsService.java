package com.kobe.warehouse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi de SMS.
 * Implémentation stub — brancher un provider réel (Twilio, Orange, etc.) ici.
 */
@Service
public class SmsService {

    private static final Logger LOG = LoggerFactory.getLogger(SmsService.class);

    @Async
    public void sendSms(String phoneNumber, String message) {
        // TODO: intégrer un provider SMS (Africa's Talking, OrangeAPIs, Infobip…)
        LOG.info("SMS [non envoyé — provider non configuré] → {} : {}", phoneNumber, message);
    }
}
