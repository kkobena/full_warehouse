package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.MailService;
import com.kobe.warehouse.service.SmsService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class AvoirClientNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(AvoirClientNotificationService.class);

    private final MailService mailService;
    private final SmsService smsService;
    private final AppConfigurationService appConfigurationService;
    private final SpringTemplateEngine templateEngine;

    public AvoirClientNotificationService(
        MailService mailService,
        SmsService smsService,
        AppConfigurationService appConfigurationService,
        SpringTemplateEngine templateEngine
    ) {
        this.mailService = mailService;
        this.smsService = smsService;
        this.appConfigurationService = appConfigurationService;
        this.templateEngine = templateEngine;
    }

    /**
     * Notifie le client que ses produits en avoir sont disponibles au comptoir.
     * Appelé uniquement lors d'une clôture en mode RETOUR_PRODUIT.
     * Exécuté de façon asynchrone pour ne pas bloquer la réponse HTTP.
     */
    @Async
    public void notifierProduitsDisponibles(AvoirClient avoir) {
        Customer customer = avoir.getCustomer();
        if (customer == null) {
            return;
        }
        Magasin magasin = appConfigurationService.getMagasin();

        if (appConfigurationService.isNotifAvoirEmailEnabled() && StringUtils.hasText(customer.getEmail())) {
            sendEmail(avoir, customer, magasin);// Brevo (ex-Sendinblue)
        }
        if (appConfigurationService.isNotifAvoirSmsEnabled() && StringUtils.hasText(customer.getPhone())) {
            sendSms(avoir, customer, magasin);
        }
    }

    private void sendEmail(AvoirClient avoir, Customer customer, Magasin magasin) {
        try {
            String customerName = (customer.getFirstName() + " "
                + Objects.requireNonNullElse(customer.getLastName(), "")).strip();
            Context context = new Context(Locale.FRENCH);
            context.setVariable("customerName", customerName);
            context.setVariable("avoir", avoir);
            context.setVariable("magasinName", magasin.getName());
            context.setVariable("magasinPhone", magasin.getPhone());
            String content = templateEngine.process("mail/avoir-produits-prets", context);
            mailService.sendEmail(
                customer.getEmail(),
                "Vos produits sont disponibles — " + magasin.getName(),
                content, false, true
            );
            LOG.debug("Email notification avoir {} envoyé à {}", avoir.getReference(), customer.getEmail());
        } catch (Exception e) {
            LOG.warn("Erreur envoi email notification avoir {} : {}", avoir.getReference(), e.getMessage());
        }
    }

    private void sendSms(AvoirClient avoir, Customer customer, Magasin magasin) {
        String message = "Vos produits (avoir " + avoir.getReference()
            + ") sont disponibles à " + magasin.getName()
            + ". Montant : " + avoir.getMontant() + " CFA.";
        smsService.sendSms(customer.getPhone(), message);
    }
}
