package com.kobe.warehouse.service.mobile;

import com.google.firebase.messaging.*;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.UserDevice;
import com.kobe.warehouse.domain.enumeration.AuthorityEnum;
import com.kobe.warehouse.repository.UserDeviceRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.dto.mobile.DailyDigestDTO;
import com.kobe.warehouse.service.dto.mobile.UserPerformanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending push notifications to mobile devices.
 * Supports role-based, contextual, and batched notifications.
 */
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
@Transactional
public class MobilePushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MobilePushNotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final MobileReportService mobileReportService;

    public MobilePushNotificationService(
        FirebaseMessaging firebaseMessaging,
        UserDeviceRepository userDeviceRepository,
        UserRepository userRepository,
        MobileReportService mobileReportService
    ) {
        this.firebaseMessaging = firebaseMessaging;
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.mobileReportService = mobileReportService;
    }

    /**
     * Send stock alert notification (immediate - high priority).
     */
    public void sendStockAlert(Long productId, String productName, int stockLevel) {
        log.info("Sending stock alert for product: {} (stock: {})", productName, stockLevel);

        Map<String, String> data = new HashMap<>();
        data.put("type", "STOCK_RUPTURE");
        data.put("productId", productId.toString());
        data.put("message", stockLevel == 0
            ? productName + " - Stock épuisé"
            : productName + " - Stock faible (" + stockLevel + " unités)");

        String title = stockLevel == 0 ? "🔴 Rupture de stock" : "🟠 Stock bas";
        String body = data.get("message");

        // Send to managers only
        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.HIGH);
    }

    /**
     * Send expiry alert notification (scheduled - default priority).
     */
    public void sendExpiryAlert(int expiringProductsCount) {
        log.info("Sending expiry alert for {} products", expiringProductsCount);

        Map<String, String> data = new HashMap<>();
        data.put("type", "EXPIRY");
        data.put("count", String.valueOf(expiringProductsCount));
        data.put("message", expiringProductsCount + " produits expirent dans moins de 30 jours");

        String title = "⚠️ Produits proches de péremption";
        String body = data.get("message");

        // Send to managers
        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.NORMAL);
    }

    /**
     * Send daily digest notification (scheduled at end of day).
     * Personalized by role: managers get full stats, sellers get their performance.
     */
    @Scheduled(cron = "0 0 18 * * *") // Every day at 6 PM
    public void sendDailyDigest() {
        log.info("Sending daily digest notifications");

        LocalDate today = LocalDate.now();

        // Managers: Full daily digest
        sendManagerDailyDigest(today);

        // Sellers: Individual performance
        sendSellerPerformanceDigest(today);
    }

    private void sendManagerDailyDigest(LocalDate date) {
        DailyDigestDTO digest = mobileReportService.generateDailyDigest(date);

        Map<String, String> data = new HashMap<>();
        data.put("type", "DAILY_DIGEST");
        data.put("date", date.toString());
        data.put("ca", digest.getTotalCA().toString());
        data.put("variation", String.format("%.1f", digest.getVariation()));

        String title = "📊 Résumé quotidien";
        String body = String.format(
            "CA: %,d FCFA (%+.1f%%) | %d ventes | %d alertes",
            digest.getTotalCA(),
            digest.getVariation(),
            digest.getTransactionCount(),
            digest.getAlertsCount()
        );

        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.NORMAL);
    }

    private void sendSellerPerformanceDigest(LocalDate date) {
        // Get all sellers with active devices
        List<AppUser> sellers = userRepository.findByAuthority(AuthorityEnum.ROLE_CAISSIER);

        for (AppUser seller : sellers) {
            UserPerformanceDTO perf = mobileReportService.getUserPerformance(seller.getId(), date);

            if (perf.getSalesCount() > 0) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "DAILY_DIGEST");
                data.put("date", date.toString());
                data.put("ca", perf.getTotalCA().toString());

                String title = "💪 Votre performance du jour";
                String body = String.format(
                    "CA: %,d FCFA | %d ventes | Panier moyen: %,d FCFA",
                    perf.getTotalCA(),
                    perf.getSalesCount(),
                    perf.getAverageBasket()
                );

                sendToUser(seller.getId(), title, body, data, AndroidConfig.Priority.NORMAL);
            }
        }
    }

    /**
     * Send target reached notification (contextual event).
     */
    public void sendTargetReachedNotification(Long dailyCA, Long target) {
        log.info("Target reached: {} / {}", dailyCA, target);

        Map<String, String> data = new HashMap<>();
        data.put("type", "TARGET_REACHED");
        data.put("ca", dailyCA.toString());
        data.put("target", target.toString());

        String title = "🎯 Objectif atteint!";
        String body = String.format(
            "Bravo! L'objectif de CA quotidien a été dépassé (%,d FCFA)",
            dailyCA
        );

        // Send to all users
        sendToAllUsers(title, body, data, AndroidConfig.Priority.HIGH);
    }

    /**
     * Send high value sale notification (contextual event).
     */
    public void sendHighValueSaleNotification(Long saleId, Long amount, String customerName) {
        if (amount < 500_000) return; // Only for sales > 500,000 FCFA

        log.info("High value sale: {} FCFA for customer {}", amount, customerName);

        Map<String, String> data = new HashMap<>();
        data.put("type", "HIGH_VALUE_SALE");
        data.put("saleId", saleId.toString());
        data.put("amount", amount.toString());

        String title = "💰 Grosse vente!";
        String body = String.format(
            "Vente de %,d FCFA enregistrée%s",
            amount,
            customerName != null ? " pour " + customerName : ""
        );

        // Send to managers
        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.NORMAL);
    }

    /**
     * Send cash discrepancy alert.
     */
    public void sendCashDiscrepancyAlert(Long cashRegisterId, Long discrepancy) {
        log.info("Cash discrepancy detected: {} FCFA", discrepancy);

        Map<String, String> data = new HashMap<>();
        data.put("type", "CASH_DISCREPANCY");
        data.put("cashRegisterId", cashRegisterId.toString());
        data.put("discrepancy", discrepancy.toString());

        String title = "🔴 Écart de caisse";
        String body = String.format("Écart détecté: %+,d FCFA", discrepancy);

        // Send to managers
        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.HIGH);
    }

    /**
     * Send invoice overdue notification.
     */
    public void sendInvoiceOverdueAlert(Long invoiceId, String clientName, int daysOverdue) {
        log.info("Invoice overdue: {} days for {}", daysOverdue, clientName);

        Map<String, String> data = new HashMap<>();
        data.put("type", "INVOICE_OVERDUE");
        data.put("invoiceId", invoiceId.toString());
        data.put("daysOverdue", String.valueOf(daysOverdue));

        String title = "💳 Facture impayée";
        String body = String.format(
            "%s - Facture impayée depuis %d jours",
            clientName,
            daysOverdue
        );

        // Send to managers
        sendToRole(AuthorityEnum.ROLE_ADMIN, title, body, data, AndroidConfig.Priority.NORMAL);
    }

    /**
     * Send notification to all users with a specific role.
     */
    private void sendToRole(
        AuthorityEnum role,
        String title,
        String body,
        Map<String, String> data,
        AndroidConfig.Priority priority
    ) {
        List<UserDevice> devices = userDeviceRepository.findByUserAuthorityAndNotificationsEnabled(
            role.name(),
            true
        );

        log.debug("Sending notification to {} devices with role {}", devices.size(), role);

        for (UserDevice device : devices) {
            sendNotification(device.getFcmToken(), title, body, data, priority);
        }
    }

    /**
     * Send notification to a specific user.
     */
    private void sendToUser(
        Integer userId,
        String title,
        String body,
        Map<String, String> data,
        AndroidConfig.Priority priority
    ) {
        List<UserDevice> devices = userDeviceRepository.findByUserIdAndNotificationsEnabled(
            userId,
            true
        );

        for (UserDevice device : devices) {
            sendNotification(device.getFcmToken(), title, body, data, priority);
        }
    }

    /**
     * Send notification to all users.
     */
    private void sendToAllUsers(
        String title,
        String body,
        Map<String, String> data,
        AndroidConfig.Priority priority
    ) {
        List<UserDevice> devices = userDeviceRepository.findByNotificationsEnabled(true);

        log.debug("Sending notification to {} devices (all users)", devices.size());

        for (UserDevice device : devices) {
            sendNotification(device.getFcmToken(), title, body, data, priority);
        }
    }

    /**
     * Send notification to a specific FCM token.
     */
    private void sendNotification(
        String fcmToken,
        String title,
        String body,
        Map<String, String> data,
        AndroidConfig.Priority priority
    ) {
        try {
            // Build Android config
            AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(priority)
                .setNotification(AndroidNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setChannelId(priority == AndroidConfig.Priority.HIGH
                        ? "pharma_alerts"
                        : "pharma_daily")
                    .build())
                .build();

            // Build message
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .setAndroidConfig(androidConfig)
                .build();

            // Send
            String response = firebaseMessaging.send(message);
            log.debug("Successfully sent message: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", fcmToken, e);

            // If token is invalid, mark device for cleanup
            if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                userDeviceRepository.deleteByFcmToken(fcmToken);
            }
        }
    }
}
