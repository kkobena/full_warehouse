package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.UserDevice;
import com.kobe.warehouse.domain.enumeration.AuthorityEnum;
import com.kobe.warehouse.repository.UserDeviceRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.dto.mobile.DailyDigestDTO;
import com.kobe.warehouse.service.dto.mobile.UserPerformanceDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Les notifications poussées partent sans que personne les demande et arrivent sur un écran
 * verrouillé : leur destinataire ne peut ni les recouper ni les redemander.
 *
 * <p>Trois choses s'y jouent donc. Le <b>ciblage</b> : une alerte de gestion ne va qu'aux titulaires,
 * un bilan de vente ne va qu'à son auteur — une notification mal adressée expose le chiffre
 * d'affaires à toute l'équipe. Le <b>seuil</b> : ce qui part trop souvent n'est plus lu, d'où le
 * plancher sur les grosses ventes et le silence pour un vendeur qui n'a rien vendu. Et la
 * <b>survie de la boucle</b> : un jeton périmé sur un téléphone perdu ne doit pas empêcher les
 * autres appareils de recevoir, et doit se nettoyer de lui-même.
 */
@DisplayName("MobilePushNotificationService — notifications poussées")
class MobilePushNotificationServiceTest {

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
    private final UserDeviceRepository userDeviceRepository = mock(UserDeviceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MobileReportService mobileReportService = mock(MobileReportService.class);
    private final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);

    private final MobilePushNotificationService service = new MobilePushNotificationService(
        firebaseMessaging,
        userDeviceRepository,
        userRepository,
        mobileReportService,
        appConfigurationService
    );

    @BeforeEach
    void unSeulAppareil() throws Exception {
        when(userDeviceRepository.findByUserAuthorityAndNotificationsEnabled(anyString(), any()))
            .thenReturn(List.of(appareil("jeton-admin")));
        when(userDeviceRepository.findByUserIdAndNotificationsEnabled(anyInt(), any()))
            .thenReturn(List.of(appareil("jeton-vendeur")));
        when(userDeviceRepository.findByNotificationsEnabled(any())).thenReturn(List.of(appareil("jeton-admin")));
        when(userRepository.findByAuthority(any())).thenReturn(List.of());
        when(appConfigurationService.getDevise()).thenReturn("FCFA");
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");
    }

    // ===== ciblage =====

    @Nested
    @DisplayName("Ciblage")
    class Ciblage {

        @Test
        @DisplayName("une alerte de gestion ne part qu'aux appareils des titulaires")
        void alerteAuxTitulaires() throws Exception {
            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(userDeviceRepository).findByUserAuthorityAndNotificationsEnabled(AuthorityEnum.ROLE_ADMIN.name(), true);
            verify(firebaseMessaging).send(any(Message.class));
        }

        @Test
        @DisplayName("un objectif atteint se réjouit auprès de tout le monde")
        void objectifATous() throws Exception {
            service.sendTargetReachedNotification(1_500_000L, 1_000_000L);

            verify(userDeviceRepository).findByNotificationsEnabled(true);
            verify(userDeviceRepository, never()).findByUserAuthorityAndNotificationsEnabled(anyString(), any());
        }

        @Test
        @DisplayName("chaque appareil enregistré reçoit sa copie")
        void unEnvoiParAppareil() throws Exception {
            when(userDeviceRepository.findByUserAuthorityAndNotificationsEnabled(anyString(), any()))
                .thenReturn(List.of(appareil("jeton-1"), appareil("jeton-2"), appareil("jeton-3")));

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(firebaseMessaging, times(3)).send(any(Message.class));
        }

        /** Personne n'a installé l'application : il n'y a rien à envoyer, et rien ne doit échouer. */
        @Test
        @DisplayName("aucun appareil enregistré n'envoie rien")
        void aucunAppareil() throws Exception {
            when(userDeviceRepository.findByUserAuthorityAndNotificationsEnabled(anyString(), any()))
                .thenReturn(List.of());

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verifyNoInteractions(firebaseMessaging);
        }
    }

    // ===== contenu =====

    @Nested
    @DisplayName("Contenu des messages")
    class ContenuDesMessages {

        @Test
        @DisplayName("une rupture franche se distingue d'un stock simplement bas")
        void ruptureOuStockBas() throws Exception {
            service.sendStockAlert(42L, "DOLIPRANE", 0);
            assertThat(dernierMessage()).contains("Rupture de stock").contains("DOLIPRANE - Stock épuisé");

            service.sendStockAlert(42L, "DOLIPRANE", 3);
            assertThat(dernierMessage()).contains("Stock bas").contains("DOLIPRANE - Stock faible (3 unités)");
        }

        @Test
        @DisplayName("l'alerte porte le type et l'identifiant du produit, pour l'ouvrir d'un geste")
        void donneesDAction() throws Exception {
            service.sendStockAlert(42L, "DOLIPRANE", 0);

            assertThat(dernierMessage()).contains("STOCK_RUPTURE").contains("\"productId\":\"42\"");
        }

        @Test
        @DisplayName("l'alerte de péremption annonce le nombre de produits concernés")
        void peremption() throws Exception {
            service.sendExpiryAlert(12);

            assertThat(dernierMessage()).contains("EXPIRY").contains("12 produits expirent dans moins de 30 jours");
        }

        @Test
        @DisplayName("l'écart de caisse est annoncé avec son signe")
        void ecartSigne() throws Exception {
            service.sendCashDiscrepancyAlert(1L, -7_500L);

            assertThat(dernierMessage()).contains("CASH_DISCREPANCY").containsPattern("Écart détecté: -7.500 FCFA");
        }

        @Test
        @DisplayName("l'impayé nomme le client et son ancienneté")
        void impaye() throws Exception {
            service.sendInvoiceOverdueAlert(77L, "CNAM", 120);

            assertThat(dernierMessage()).contains("INVOICE_OVERDUE").contains("CNAM - Facture impayée depuis 120 jours");
        }

        /** La devise est configurable : une officine hors zone franc ne doit pas lire « FCFA ». */
        @Test
        @DisplayName("les montants portent la devise configurée")
        void deviseConfiguree() throws Exception {
            when(appConfigurationService.getDevise()).thenReturn("EUR");

            service.sendTargetReachedNotification(1_500_000L, 1_000_000L);

            assertThat(dernierMessage()).contains("EUR").doesNotContain("FCFA");
        }
    }

    // ===== seuils =====

    @Nested
    @DisplayName("Seuils d'envoi")
    class SeuilsDEnvoi {

        /** Sans plancher, chaque vente notifierait : la notification cesserait d'être lue. */
        @Test
        @DisplayName("une vente sous le plancher ne notifie personne")
        void venteOrdinaire() throws Exception {
            service.sendHighValueSaleNotification(1L, 499_999L, "Client");

            verifyNoInteractions(firebaseMessaging, userDeviceRepository);
        }

        @Test
        @DisplayName("une vente au plancher notifie")
        void venteAuPlancher() throws Exception {
            service.sendHighValueSaleNotification(1L, 500_000L, "Awa Koné");

            assertThat(dernierMessage()).contains("HIGH_VALUE_SALE").contains("pour Awa Koné");
        }

        @Test
        @DisplayName("une vente sans client nommé n'invente pas de nom")
        void venteSansClient() throws Exception {
            service.sendHighValueSaleNotification(1L, 800_000L, null);

            assertThat(dernierMessage()).containsPattern("Vente de 800.000 FCFA enregistrée").doesNotContain("pour ");
        }
    }

    // ===== résumé quotidien =====

    @Nested
    @DisplayName("Résumé quotidien")
    class ResumeQuotidien {

        @BeforeEach
        void officineActive() {
            when(mobileReportService.generateDailyDigest(any())).thenReturn(
                new DailyDigestDTO(1_200_000L, 20.0, 42, 3, 1_000_000L, 20.0, 38, 28_571L)
            );
        }

        @Test
        @DisplayName("le titulaire reçoit le chiffre du jour, sa variation et le nombre d'alertes")
        void resumeDuTitulaire() throws Exception {
            service.sendDailyDigest();

            assertThat(dernierMessage())
                .contains("DAILY_DIGEST")
                .containsPattern("CA: 1.200.000 FCFA \\(\\+20[.,]0%\\) \\| 42 ventes \\| 3 alertes");
        }

        @Test
        @DisplayName("chaque vendeur ayant vendu reçoit son propre bilan")
        void bilanDuVendeur() throws Exception {
            when(userRepository.findByAuthority(AuthorityEnum.ROLE_CAISSIER)).thenReturn(List.of(vendeur(7)));
            when(mobileReportService.getUserPerformance(eq(7), any())).thenReturn(performance(600_000L, 25, 24_000L));

            service.sendDailyDigest();

            verify(userDeviceRepository).findByUserIdAndNotificationsEnabled(7, true);
            assertThat(dernierMessage()).containsPattern("CA: 600.000 FCFA \\| 25 ventes \\| Panier moyen: 24.000 FCFA");
        }

        /** Un vendeur qui n'a rien vendu n'a pas besoin qu'on le lui rappelle le soir. */
        @Test
        @DisplayName("un vendeur sans vente ne reçoit rien")
        void vendeurSansVente() throws Exception {
            when(userRepository.findByAuthority(AuthorityEnum.ROLE_CAISSIER)).thenReturn(List.of(vendeur(7)));
            when(mobileReportService.getUserPerformance(eq(7), any())).thenReturn(performance(0L, 0, 0L));

            service.sendDailyDigest();

            verify(userDeviceRepository, never()).findByUserIdAndNotificationsEnabled(anyInt(), any());
        }

        @Test
        @DisplayName("le résumé porte sur la journée en cours")
        void journeeEnCours() {
            service.sendDailyDigest();

            verify(mobileReportService).generateDailyDigest(LocalDate.now());
        }
    }

    // ===== jetons périmés =====

    @Nested
    @DisplayName("Jetons périmés")
    class JetonsPerimes {

        @Test
        @DisplayName("un jeton refusé par Firebase est retiré de la base")
        void jetonRetire() throws Exception {
            FirebaseMessagingException echec = echec(MessagingErrorCode.UNREGISTERED);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(echec);

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(userDeviceRepository).deleteByFcmToken("jeton-admin");
        }

        @Test
        @DisplayName("un jeton mal formé est lui aussi retiré")
        void jetonMalForme() throws Exception {
            FirebaseMessagingException echec = echec(MessagingErrorCode.INVALID_ARGUMENT);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(echec);

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(userDeviceRepository).deleteByFcmToken("jeton-admin");
        }

        /** Une panne passagère de Firebase ne doit pas déréférencer les téléphones de l'officine. */
        @Test
        @DisplayName("une indisponibilité passagère ne retire aucun appareil")
        void pannePassagere() throws Exception {
            FirebaseMessagingException echec = echec(MessagingErrorCode.UNAVAILABLE);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(echec);

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(userDeviceRepository, never()).deleteByFcmToken(anyString());
        }

        @Test
        @DisplayName("un appareil en échec n'empêche pas les autres de recevoir")
        void echecIsole() throws Exception {
            when(userDeviceRepository.findByUserAuthorityAndNotificationsEnabled(anyString(), any()))
                .thenReturn(List.of(appareil("jeton-1"), appareil("jeton-2"), appareil("jeton-3")));
            FirebaseMessagingException echec = echec(MessagingErrorCode.UNREGISTERED);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(echec).thenReturn("message-id");

            service.sendStockAlert(42L, "DOLIPRANE", 0);

            verify(firebaseMessaging, times(3)).send(any(Message.class));
            verify(userDeviceRepository).deleteByFcmToken("jeton-1");
        }
    }

    // ===== utilitaires =====

    /**
     * Le message Firebase n'expose aucun accesseur : ses champs sont annotés pour la sérialisation
     * JSON. On le relit donc tel qu'il partira sur le réseau.
     */
    private String dernierMessage() throws Exception {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        return new com.google.api.client.json.gson.GsonFactory().toString(captor.getValue());
    }

    private static FirebaseMessagingException echec(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return exception;
    }

    private static UserDevice appareil(String jeton) {
        UserDevice appareil = new UserDevice();
        appareil.setFcmToken(jeton);
        return appareil;
    }

    private static AppUser vendeur(int id) {
        AppUser utilisateur = new AppUser();
        ReflectionTestUtils.setField(utilisateur, "id", id);
        return utilisateur;
    }

    private static UserPerformanceDTO performance(long chiffreAffaires, int ventes, long panierMoyen) {
        return new UserPerformanceDTO(7L, "Awa Koné", chiffreAffaires, ventes, panierMoyen, 0.0);
    }
}
