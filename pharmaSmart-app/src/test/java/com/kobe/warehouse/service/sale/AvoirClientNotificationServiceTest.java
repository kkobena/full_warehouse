package com.kobe.warehouse.service.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.MailService;
import com.kobe.warehouse.service.SmsService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class AvoirClientNotificationServiceTest {

    @Mock
    private MailService mailService;
    @Mock
    private SmsService smsService;
    @Mock
    private AppConfigurationService appConfigurationService;
    @Mock
    private SpringTemplateEngine templateEngine;

    private AvoirClientNotificationService service;
    private AvoirClient avoir;
    private Customer customer;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new AvoirClientNotificationService(
            mailService, smsService, appConfigurationService, templateEngine);

        customer = new Customer();
        customer.setFirstName("Alice");
        customer.setLastName("Kouassi");
        customer.setEmail("alice@example.test");
        customer.setPhone("0700000000");

        avoir = new AvoirClient()
            .setReference("AV-2026-001")
            .setMontant(12_500)
            .setCustomer(customer);

        magasin = new Magasin();
        magasin.setName("Pharmacie Centrale");
        magasin.setPhone("0102030405");
    }

    @Test
    void notifierProduitsDisponibles_SansClient_NeFaitRien() {
        avoir.setCustomer(null);

        service.notifierProduitsDisponibles(avoir);

        verify(appConfigurationService, never()).getMagasin();
        verify(mailService, never()).sendEmail(any(), any(), any(), anyBoolean(), anyBoolean());
        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void notifierProduitsDisponibles_EmailEtSmsActifs_EnvoieLesDeuxNotifications() {
        when(appConfigurationService.getMagasin()).thenReturn(magasin);
        when(appConfigurationService.isNotifAvoirEmailEnabled()).thenReturn(true);
        when(appConfigurationService.isNotifAvoirSmsEnabled()).thenReturn(true);
        when(templateEngine.process(eq("mail/avoir-produits-prets"), any(Context.class)))
            .thenReturn("<html>notification</html>");

        service.notifierProduitsDisponibles(avoir);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/avoir-produits-prets"), contextCaptor.capture());
        assertEquals("Alice Kouassi", contextCaptor.getValue().getVariable("customerName"));
        assertEquals(avoir, contextCaptor.getValue().getVariable("avoir"));
        assertEquals("Pharmacie Centrale", contextCaptor.getValue().getVariable("magasinName"));
        verify(mailService).sendEmail(
            "alice@example.test",
            "Vos produits sont disponibles — Pharmacie Centrale",
            "<html>notification</html>", false, true);
        verify(smsService).sendSms(
            eq("0700000000"),
            contains("avoir AV-2026-001"));
        verify(smsService).sendSms(
            eq("0700000000"),
            contains("12500 CFA"));
    }

    @Test
    void notifierProduitsDisponibles_CanauxDesactives_NEnvoieRien() {
        when(appConfigurationService.getMagasin()).thenReturn(magasin);
        when(appConfigurationService.isNotifAvoirEmailEnabled()).thenReturn(false);
        when(appConfigurationService.isNotifAvoirSmsEnabled()).thenReturn(false);

        service.notifierProduitsDisponibles(avoir);

        verify(templateEngine, never()).process(any(String.class), any(Context.class));
        verify(mailService, never()).sendEmail(any(), any(), any(), anyBoolean(), anyBoolean());
        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void notifierProduitsDisponibles_EchecEmail_NeBloquePasLeSms() {
        when(appConfigurationService.getMagasin()).thenReturn(magasin);
        when(appConfigurationService.isNotifAvoirEmailEnabled()).thenReturn(true);
        when(appConfigurationService.isNotifAvoirSmsEnabled()).thenReturn(true);
        when(templateEngine.process(eq("mail/avoir-produits-prets"), any(Context.class)))
            .thenReturn("contenu");
        doThrow(new IllegalStateException("SMTP indisponible"))
            .when(mailService).sendEmail(any(), any(), any(), eq(false), eq(true));

        service.notifierProduitsDisponibles(avoir);

        verify(smsService).sendSms(eq("0700000000"), contains("Pharmacie Centrale"));
    }
}

