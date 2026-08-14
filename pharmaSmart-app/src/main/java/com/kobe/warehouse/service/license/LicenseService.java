package com.kobe.warehouse.service.license;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.LicenseAudit;
import com.kobe.warehouse.domain.LicenseState;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.LicenseAuditEventType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.license.HardwareFingerprintProvider;
import com.kobe.warehouse.license.LicenseInfo;
import com.kobe.warehouse.license.LicensePayload;
import com.kobe.warehouse.license.LicenseProperties;
import com.kobe.warehouse.license.LicenseStatus;
import com.kobe.warehouse.license.LicenseTextNormalizer;
import com.kobe.warehouse.license.LicenseType;
import com.kobe.warehouse.license.LicenseVerificationException;
import com.kobe.warehouse.license.LicenseVerifier;
import com.kobe.warehouse.license.SupportContacts;
import com.kobe.warehouse.repository.LicenseAuditRepository;
import com.kobe.warehouse.repository.LicenseStateRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.security.SecurityUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Point d'autorité unique sur l'état de la licence : c'est ce service que consultent l'aspect
 * d'enforcement, les contrôleurs REST et, à terme, les jobs batch.
 *
 * <p><strong>Le statut est calculé rarement et lu très souvent.</strong> L'évaluation touche le
 * disque, la base et l'introspection réseau ; la faire à chaque écriture placerait ces coûts sur le
 * chemin critique d'une vente. Le résultat est donc conservé dans un {@link AtomicReference} et
 * recalculé au démarrage, toutes les 15 minutes, au changement de jour et après toute activation.
 *
 */
@Service
public class LicenseService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);
    /**
     * Délai avant nouvelle tentative après un échec technique.
     *
     * <p>Sans lui, une base injoignable ferait payer à <em>chaque</em> écriture le délai de
     * connexion Hikari (30 s) : l'application paraîtrait figée. Une minute laisse le temps à
     * PostgreSQL de finir de démarrer sans transformer l'intercepteur en générateur d'attentes.
     */
    private static final Duration FAILURE_RETRY_DELAY = Duration.ofMinutes(1);
    /**
     * Format des dates dans les messages utilisateur.
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LicenseProperties properties;
    private final LicenseVerifier verifier;
    private final HardwareFingerprintProvider fingerprintProvider;
    private final LicenseStateRepository licenseStateRepository;
    private final LicenseAuditRepository licenseAuditRepository;
    private final MagasinRepository magasinRepository;
    private final SalesRepository salesRepository;
    private final ProduitRepository produitRepository;
    private final CacheManager cacheManager;
    /**
     * Statut courant et date UTC de son calcul — voir {@link #currentStatus()}.
     */
    private final AtomicReference<CachedStatus> cache = new AtomicReference<>();

    public LicenseService(
        LicenseProperties properties,
        LicenseVerifier verifier,
        HardwareFingerprintProvider fingerprintProvider,
        LicenseStateRepository licenseStateRepository,
        LicenseAuditRepository licenseAuditRepository,
        MagasinRepository magasinRepository,
        SalesRepository salesRepository,
        ProduitRepository produitRepository,
        CacheManager cacheManager
    ) {
        this.properties = properties;
        this.verifier = verifier;
        this.fingerprintProvider = fingerprintProvider;
        this.licenseStateRepository = licenseStateRepository;
        this.licenseAuditRepository = licenseAuditRepository;
        this.magasinRepository = magasinRepository;
        this.salesRepository = salesRepository;
        this.produitRepository = produitRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Statut courant, servi depuis le cache mémoire.
     *
     * <p>Le cache est invalidé au changement de jour UTC : sans cela, une licence expirant à
     * minuit resterait valide jusqu'au prochain rafraîchissement planifié.
     */
    public LicenseInfo currentStatus() {
        if (!properties.isEnabled()) {
            return LicenseInfo.disabled();
        }
        CachedStatus cached = cache.get();
        if (cached == null || cached.isStale(today(), Instant.now())) {
            return refresh();
        }
        return infoOf(cached);
    }

    /**
     * Projette une entrée de cache en statut affichable, <em>à l'instant présent</em>.
     *
     * <p>Tout ce qui dépend du temps — le statut de date, le nombre de jours restants, le message
     * — est calculé ici et nulle part ailleurs : c'est ce qui fait du décompte un vrai compteur
     * plutôt qu'une phrase mémorisée le jour de l'évaluation.
     */
    private LicenseInfo infoOf(CachedStatus cached) {
        if (cached.payload() == null) {
            return cached.frozenInfo();
        }
        LicensePayload payload = cached.payload();
        LicenseStatus status = cached.pinnedStatus() != null
            ? cached.pinnedStatus()
            : computeDateStatus(payload, Instant.now());
        return status(payload, status, cached.pinnedMessage());
    }

    // ------------------------------------------------------------------ API publique

    /**
     * {@code true} si les opérations d'écriture sont autorisées (exigence B4).
     */
    public boolean isWriteAllowed() {
        return !currentStatus().readOnly();
    }

    /**
     * {@code true} si le module est couvert par l'abonnement (§3.6).
     */
    public boolean hasFeature(Feature feature) {
        return currentStatus().hasFeature(feature);
    }

    /**
     * {@code true} si l'installation tourne sous une licence de démonstration (§3.5).
     */
    public boolean isDemo() {
        return currentStatus().demo();
    }

    /**
     * Empreinte matérielle du poste, à transmettre à l'éditeur <em>uniquement</em> s'il délivre une
     * licence liée au matériel ({@code MAGASIN_AND_HARDWARE}) — le cas courant s'en passe.
     */
    public String hardwareFingerprint() {
        return fingerprintProvider.fingerprint();
    }

    /**
     * Évaluation initiale au démarrage. Le résultat est journalisé de façon explicite : sur une
     * installation on-premise, les logs backend sont souvent le seul élément de diagnostic dont
     * dispose le support à distance .
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isEnabled()) {
            LOG.warn("Contrôle de licence DÉSACTIVÉ (pharma-smart.license.enabled=false)");
            return;
        }
        LicenseInfo info = refresh();
        switch (info.status()) {
            case VALID ->
                LOG.info("Licence valide jusqu'au {} ({} jour(s) restant(s))", info.expiresAt(),
                    info.daysRemaining());
            case EXPIRING_SOON, EXPIRING_CRITICAL, GRACE -> LOG.warn(
                "Licence à renouveler : {} — expiration le {} ({} jour(s))",
                info.status(),
                info.expiresAt(),
                info.daysRemaining()
            );
            // La base n'était pas joignable au démarrage : le statut réel sera établi au premier
            // rafraîchissement qui aboutit, sans que l'officine soit bridée entre-temps.
            case UNKNOWN -> LOG.warn(
                "Contrôle de licence indisponible au démarrage — nouvelle tentative dans {}",
                FAILURE_RETRY_DELAY);
            default ->
                LOG.error("LICENCE NON VALIDE ({}) : {} — l'application est en LECTURE SEULE",
                    info.status(), info.message());
        }
    }

    // ------------------------------------------------------------------ cycle de vie

    /**
     * Rafraîchit le statut et la sonde anti-recul d'horloge.
     *
     * <p>Quinze minutes est un compromis : assez fréquent pour qu'un recul d'horloge laisse une
     * trace exploitable, assez rare pour rester invisible en charge.
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void scheduledRefresh() {
        if (properties.isEnabled()) {
            refresh();
        }
    }

    /**
     * Volontairement <strong>non</strong> {@code @Transactional} : cette méthode est appelée depuis
     * {@link #currentStatus()}, donc en auto-invocation, où le proxy Spring ne s'applique pas — une
     * annotation ici serait silencieusement sans effet. Les rares écritures qu'elle déclenche
     * portent chacune leur propre transaction (méthodes de repository).
     */
    public LicenseInfo refresh() {
        try {
            CachedStatus evaluated = evaluate();
            cache.set(evaluated);
            return infoOf(evaluated);
        } catch (RuntimeException e) {
            return degraded(e);
        }
    }

    // ------------------------------------------------------------------ évaluation

    /**
     * Repli lorsque l'évaluation elle-même échoue.
     */
    private LicenseInfo degraded(RuntimeException e) {
        LOG.error("Échec de l'évaluation de la licence", e);
        Instant retryAfter = Instant.now().plus(FAILURE_RETRY_DELAY);
        if (isInfrastructureFailure(e)) {
            CachedStatus previous = cache.get();
            CachedStatus fallback = (previous != null ? previous
                : CachedStatus.frozen(LicenseInfo.unavailable(), today()))
                .retryingAfter(today(), retryAfter);
            cache.set(fallback);
            return infoOf(fallback);
        }
        LicenseInfo info = invalid(
            "Le contrôle de licence n'a pas pu aboutir. Contactez votre revendeur.",
            supportOf(null));
        cache.set(CachedStatus.frozen(info, today()).retryingAfter(today(), retryAfter));
        return info;
    }

    /**
     * Distingue « la base est injoignable » de « la licence est en cause ».
     *
     * <p>On remonte la chaîne des causes : une indisponibilité Hikari arrive enveloppée dans une
     * {@code JpaSystemException}, dont la {@link java.sql.SQLException} d'origine est le seul
     * signal fiable.
     */
    private boolean isInfrastructureFailure(Throwable e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current instanceof DataAccessException || current instanceof SQLException) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    private CachedStatus evaluate() {
        LocalDate today = today();
        Optional<LicenseState> stateOpt = licenseStateRepository.findSingleton();
        String token = loadToken(stateOpt.orElse(null));
        if (token == null) {
            return CachedStatus.frozen(LicenseInfo.missing(SupportContacts.empty()), today);
        }

        LicensePayload payload;
        try {
            payload = verifier.verify(token);
        } catch (LicenseVerificationException e) {
            audit(LicenseAuditEventType.REJECTION, e.getMessage(), null);
            return CachedStatus.frozen(invalid(e.getMessage(), SupportContacts.empty()), today);
        }

        // La sonde d'horloge d'abord : si la date système est fausse, toutes les comparaisons
        // temporelles qui suivent sont sans valeur.
        Instant now = Instant.now();
        LicenseState state = stateOpt.orElse(null);
        if (isClockTampered(state, now)) {
            audit(LicenseAuditEventType.CLOCK_TAMPER,
                "Horloge système reculée par rapport à la dernière exécution", payload.licenseId());
            return CachedStatus.pinned(payload, LicenseStatus.CLOCK_TAMPERED,
                "L'horloge du poste est incohérente avec l'historique d'utilisation.", today);
        }
        touchLastSeen(state, now);

        if (now.isBefore(payload.validFromInstant())) {
            return CachedStatus.pinned(payload, LicenseStatus.INVALID,
                "Cette licence n'est pas encore valide (à partir du " + payload.validFrom() + ").",
                today);
        }

        String bindingError = checkBinding(payload, state, now);
        if (bindingError != null) {
            return CachedStatus.pinned(payload, LicenseStatus.INVALID, bindingError, today);
        }

        String quotaError = payload.isDemo() ? checkDemoQuotas(payload) : null;
        if (quotaError != null) {
            return CachedStatus.pinned(payload, LicenseStatus.DEMO_QUOTA_REACHED, quotaError,
                today);
        }

        // Aucun contrôle n'a tranché : seule l'échéance décide, et elle se réévalue à chaque lecture.
        return CachedStatus.live(payload, today);
    }

    /**
     * Plafonne le volume exploitable d'une version de démonstration (§3.5).
     *
     * <p>Une démo doit rester confortable à présenter tout en étant inutilisable en production :
     * c'est ce que règlent ces quotas. Sans eux, une licence `DEMO` — installable partout par
     * construction — deviendrait le moyen le plus simple de faire tourner une officine sans payer.
     *
     * <p>Le comptage n'a lieu que pour une démo : une installation en production ne paie jamais
     * ces deux requêtes.
     *
     * @return le message de dépassement, ou {@code null} si les quotas sont respectés
     */
    private String checkDemoQuotas(LicensePayload payload) {
        long sales = salesRepository.count();
        if (sales >= payload.effectiveMaxSales()) {
            return "Limite de la version de démonstration atteinte (%d ventes). Souscrivez un abonnement pour continuer.".formatted(
                payload.effectiveMaxSales()
            );
        }
        long produits = produitRepository.count();
        if (produits >= payload.effectiveMaxProduits()) {
            return "Limite de la version de démonstration atteinte (%d produits). Souscrivez un abonnement pour continuer.".formatted(
                payload.effectiveMaxProduits()
            );
        }
        return null;
    }

    /**
     * Charge le jeton depuis le disque, avec repli sur la copie en base.
     */
    private String loadToken(LicenseState state) {
        Path path = properties.resolveFilePath();
        if (Files.isReadable(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    return content;
                }
            } catch (IOException e) {
                LOG.warn("Fichier de licence illisible ({}), repli sur la copie en base", path, e);
            }
        }
        String stored = state == null ? null : state.getLicenseToken();
        if (stored == null || stored.isBlank()) {
            return null;
        }

        if (!Files.exists(path)) {
            writeTokenToDisk(stored);
        }
        return stored;
    }

    // ------------------------------------------------------------------ chargement du jeton

    /**
     * Détecte un recul d'horloge.
     *
     * <p>Le client est administrateur de sa machine : reculer la date est le contournement le
     * moins coûteux qui soit. La sonde {@code last_seen_instant}, écrite à chaque cycle, rend
     * l'opération détectable sans dépendre du réseau.
     */
    private boolean isClockTampered(LicenseState state, Instant now) {
        if (state == null || state.getLastSeenInstant() == null) {
            return false;
        }
        Instant floor = state.getLastSeenInstant()
            .minus(Duration.ofHours(properties.getClockToleranceHours()));
        return now.isBefore(floor);
    }

    // ------------------------------------------------------------------ anti-fraude

    private void touchLastSeen(LicenseState state, Instant now) {
        if (state != null) {
            licenseStateRepository.touchLastSeenInstant(now);
        }
    }

    /**
     * Contrôle les liaisons de la licence à cette installation : officine, n° contribuable,
     * matériel.
     *
     * @return le message d'invalidité, ou {@code null} si la licence est utilisable ici
     */
    private String checkBinding(LicensePayload payload, LicenseState state, Instant now) {
        if (!payload.bindingPolicy().checksMagasin()) {
            return null;
        }
        Magasin magasin = magasinRepository.findFirstByTypeMagasinOrderByIdAsc(TypeMagasin.OFFICINE)
            .orElse(null);
        if (magasin == null) {
            // Aucune officine paramétrée : on ne bloque pas une installation en cours de
            // configuration, il n'y a encore rien à comparer.
            LOG.warn("Aucune officine paramétrée : le contrôle de liaison de licence est ignoré");
            return null;
        }

        if (!LicenseTextNormalizer.matches(payload.magasinName(), magasin.getName())) {
            audit(
                LicenseAuditEventType.MAGASIN_NAME_MISMATCH,
                "Licence délivrée à « %s », officine paramétrée « %s »".formatted(
                    payload.magasinName(), magasin.getName()),
                payload.licenseId()
            );
            return "Cette licence est délivrée à l'officine « %s » et ne peut pas être utilisée ici.".formatted(
                payload.magasinName());
        }

        // Renfort : ne tranche que si le n° contribuable est renseigné des deux côtés (colonne nullable).
        if (!LicenseTextNormalizer.matchesIfBothPresent(payload.taxId(),
            magasin.getCompteContribuable())) {
            audit(
                LicenseAuditEventType.TAX_ID_MISMATCH,
                "N° contribuable de la licence « %s », officine « %s »".formatted(payload.taxId(),
                    magasin.getCompteContribuable()),
                payload.licenseId()
            );
            return "Le numéro contribuable de cette officine ne correspond pas à celui de la licence.";
        }

        return payload.bindingPolicy().checksHardware() ? checkFingerprint(payload, state, now)
            : null;
    }

    /**
     * Contrôle gradué de l'empreinte matérielle .
     *
     * <p>Un changement de serveur ou une panne de carte réseau sont des événements légitimes .
     */
    private String checkFingerprint(LicensePayload payload, LicenseState state, Instant now) {
        String expected = payload.hardwareFingerprint();
        if (expected == null || expected.isBlank()) {
            return null;
        }
        if (expected.equalsIgnoreCase(fingerprintProvider.fingerprint())) {
            clearFingerprintMismatch(state);
            return null;
        }

        Instant since = markFingerprintMismatch(state, now);
        long days = ChronoUnit.DAYS.between(since, now);
        if (days < properties.getFingerprintMismatchToleranceDays()) {
            long remaining = properties.getFingerprintMismatchToleranceDays() - days;
            LOG.warn("Empreinte matérielle divergente depuis {} jour(s) — blocage dans {} jour(s)",
                days, remaining);
            return null;
        }
        audit(
            LicenseAuditEventType.FINGERPRINT_MISMATCH,
            "Empreinte divergente depuis %s (délai de régularisation dépassé)".formatted(since),
            payload.licenseId()
        );
        return "Cette licence a été délivrée pour un autre poste. Contactez votre revendeur pour la faire réémettre.";
    }

    private Instant markFingerprintMismatch(LicenseState state, Instant now) {
        if (state == null) {
            return now;
        }
        if (state.getFingerprintMismatchSince() == null) {
            audit(LicenseAuditEventType.FINGERPRINT_MISMATCH,
                "Première divergence d'empreinte matérielle constatée", state.getLicenseId());
            state.setFingerprintMismatchSince(now);
            licenseStateRepository.save(state);
            return now;
        }
        return state.getFingerprintMismatchSince();
    }

    private void clearFingerprintMismatch(LicenseState state) {
        if (state != null && state.getFingerprintMismatchSince() != null) {
            state.setFingerprintMismatchSince(null);
            licenseStateRepository.save(state);
        }
    }

    private LicenseStatus computeDateStatus(LicensePayload payload, Instant now) {
        Instant expiry = payload.expiryInstant();
        if (now.isBefore(expiry)) {
            long days = daysRemaining(payload, now);
            if (days > properties.getWarningThresholdDays()) {
                return LicenseStatus.VALID;
            }
            return days > properties.getCriticalThresholdDays() ? LicenseStatus.EXPIRING_SOON
                : LicenseStatus.EXPIRING_CRITICAL;
        }
        return now.isBefore(payload.graceEndInstant()) ? LicenseStatus.GRACE
            : LicenseStatus.EXPIRED;
    }

    // ------------------------------------------------------------------ statuts

    private long daysRemaining(LicensePayload payload, Instant now) {
        if (payload.expiresAt() == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(now.atZone(ZoneOffset.UTC).toLocalDate(),
            payload.expiresAt());
    }

    private LicenseInfo status(LicensePayload payload, LicenseStatus status,
        String overrideMessage) {
        Instant now = Instant.now();
        long days = daysRemaining(payload, now);
        String message =
            overrideMessage != null ? overrideMessage : defaultMessage(payload, status, days, now);
        boolean demo = payload.isDemo();
        return new LicenseInfo(
            status,
            payload.licenseType(),
            payload.edition(),
            payload.expiresAt(),
            days,
            payload.magasinName(),
            Set.copyOf(payload.features()),
            status.isReadOnly(),
            demo,
            // Une démo doit s'annoncer en permanence, quel que soit le nombre de jours restants :
            // un ticket de démonstration ne doit jamais pouvoir passer pour un document réel.
            status.requiresBanner() || demo,
            message,
            payload.support()
        );
    }

    /**
     * Message destiné au pharmacien.
     *
     * <p>Les échéances y sont exprimées en <strong>décompte</strong>, recalculé à chaque lecture
     * (cf. {@link CachedStatus}) : un texte qui annonce « dans 14 jours » le jour même de
     * l'expiration décrédibilise l'alerte et, pire, laisse croire qu'il reste du temps. Le décompte
     * se lit donc toujours au présent, et l'échéance est rappelée en clair pour que le client
     * puisse la noter.
     */
    private String defaultMessage(LicensePayload payload, LicenseStatus status, long days,
        Instant now) {
        return switch (status) {
            // Une licence sans échéance (partenaire) n'a pas de décompte à afficher.
            case VALID -> payload.expiresAt() == null
                ? "Licence valide, sans échéance."
                : "Licence valide jusqu'au %s (%d jours restants).".formatted(
                    date(payload.expiresAt()), days);
            case EXPIRING_SOON, EXPIRING_CRITICAL -> "Votre abonnement expire %s, le %s.".formatted(
                countdown(days),
                date(payload.expiresAt())
            );
            case GRACE -> "Votre abonnement a expiré le %s. %s".formatted(
                date(payload.expiresAt()),
                graceCountdown(graceDaysRemaining(payload, now))
            );
            case EXPIRED ->
                "Votre abonnement a expiré le %s. L'application est en lecture seule.".formatted(
                    date(payload.expiresAt())
                );
            case DEMO_QUOTA_REACHED -> "Limite de la version de démonstration atteinte.";
            default -> "Licence non valide.";
        };
    }

    /**
     * Décompte avant échéance. Le cas {@code days <= 0} n'est pas théorique : c'est le dernier
     * jour, celui où l'alerte doit être la plus claire, et « dans 0 jour(s) » ne veut rien dire.
     */
    private String countdown(long days) {
        if (days <= 0) {
            return "aujourd'hui";
        }
        return days == 1 ? "demain" : "dans %d jours".formatted(days);
    }

    /**
     * Décompte du délai de renouvellement, sur le même principe.
     */
    private String graceCountdown(long days) {
        if (days <= 0) {
            return "Dernier jour pour le renouveler avant le passage en lecture seule.";
        }
        return days == 1
            ? "Il vous reste jusqu'à demain pour le renouveler avant le passage en lecture seule."
            : "Il vous reste %d jours pour le renouveler avant le passage en lecture seule.".formatted(
                days);
    }

    /**
     * Jours restants avant la fin du délai de renouvellement.
     */
    private long graceDaysRemaining(LicensePayload payload, Instant now) {
        if (payload.expiresAt() == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(
            now.atZone(ZoneOffset.UTC).toLocalDate(),
            payload.expiresAt().plusDays(payload.gracePeriodDays())
        );
    }

    /**
     * Les dates s'affichent au format local : un {@code 2026-08-27} ISO ne parle pas à un client.
     */
    private String date(LocalDate value) {
        return value == null ? "—" : value.format(DATE_FORMAT);
    }

    private LicenseInfo invalid(String message, SupportContacts support) {
        return new LicenseInfo(
            LicenseStatus.INVALID,
            LicenseType.SUBSCRIPTION,
            "STANDARD",
            null,
            0,
            null,
            Set.of(),
            true,
            false,
            true,
            message,
            support
        );
    }

    private SupportContacts supportOf(LicensePayload payload) {
        return payload == null ? SupportContacts.empty() : payload.support();
    }

    /**
     * Active une licence déposée par un administrateur.
     *
     * <p>La licence n'est persistée <strong>que si elle est exploitable ici</strong> : accepter
     * puis afficher « invalide » laisserait l'officine sans recours, l'ancienne licence ayant été
     * écrasée.
     *
     * @throws LicenseActivationException si le fichier est refusé — le message est destiné à
     *                                    l'utilisateur, il doit dire quoi faire
     */
    @Transactional
    public LicenseInfo activate(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            throw new LicenseActivationException("Le fichier déposé est vide.",
                "license.activate.empty");
        }
        if (content.length > properties.getMaxUploadBytes()) {
            throw new LicenseActivationException(
                "Le fichier déposé n'est pas un fichier de licence (taille inattendue).",
                "license.activate.tooLarge");
        }

        String token = new String(content, StandardCharsets.UTF_8).trim();
        String login = SecurityUtils.getCurrentUserLogin().orElse("system");

        LicensePayload payload;
        try {
            payload = verifier.verify(token);
        } catch (LicenseVerificationException e) {
            audit(LicenseAuditEventType.REJECTION,
                "%s (fichier : %s)".formatted(e.getMessage(), originalFilename), null);
            throw new LicenseActivationException(e.getMessage(), e.getErrorKey());
        }

        LicenseState state = licenseStateRepository.findSingleton()
            .orElseGet(() -> new LicenseState().setId(LicenseState.SINGLETON_ID));
        Instant now = Instant.now();

        // Nouveau fichier ⇒ l'historique de divergence d'empreinte de l'ancienne licence n'a plus
        // de sens : le compteur de régularisation repart de zéro.
        state.setFingerprintMismatchSince(null);

        String bindingError = checkBinding(payload, state, now);
        if (bindingError != null) {
            audit(LicenseAuditEventType.REJECTION, bindingError, payload.licenseId());
            throw new LicenseActivationException(bindingError, "license.activate.binding");
        }
        if (now.isAfter(payload.graceEndInstant())) {
            String message = "Cette licence est déjà expirée (échéance : %s).".formatted(
                payload.expiresAt());
            audit(LicenseAuditEventType.REJECTION, message, payload.licenseId());
            throw new LicenseActivationException(message, "license.activate.expired");
        }

        state
            .setLicenseToken(token)
            .setLicenseId(payload.licenseId())
            .setExpiresAt(payload.expiresAt())
            .setActivatedAt(now)
            .setActivatedBy(login)
            .setLastSeenInstant(now)
            .setHardwareFingerprint(fingerprintProvider.fingerprint());
        licenseStateRepository.save(state);

        writeTokenToDisk(token);

        audit(
            LicenseAuditEventType.ACTIVATION,
            "Licence %s (%s) activée jusqu'au %s".formatted(payload.licenseId(),
                payload.licenseType(), payload.expiresAt()),
            payload.licenseId()
        );
        LOG.info("Licence {} activée par {} — échéance {}", payload.licenseId(), login,
            payload.expiresAt());

        // Le cache est reconstruit immédiatement : la bannière doit disparaître et l'écriture être
        // rétablie sans redémarrage du backend ni rechargement de la page (scénario de recette n°5).
        CachedStatus activated = CachedStatus.live(payload, today());
        cache.set(activated);
        evictNavigationTree();
        return infoOf(activated);
    }

    // ------------------------------------------------------------------ activation

    /**
     * Vide l'arbre de navigation mis en cache par utilisateur.
     *
     * <p>Cet arbre est filtré selon les modules souscrits (§3.6) : sans purge, souscrire un module
     * supplémentaire ne ferait apparaître son menu qu'à l'expiration du cache — plusieurs heures
     * plus tard — et le client conclurait que son achat n'a pas été pris en compte.
     */
    private void evictNavigationTree() {
        Cache navTree = cacheManager.getCache(EntityConstant.NAV_TREE_CACHE);
        if (navTree != null) {
            navTree.clear();
        }
    }

    /**
     * Recopie le jeton sur disque. Un échec n'est pas bloquant : la base fait déjà foi, et refuser
     * l'activation pour un problème de droits sur un répertoire laisserait l'officine bloquée.
     */
    private void writeTokenToDisk(String token) {
        Path path = properties.resolveFilePath();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, token, StandardCharsets.UTF_8);

            LOG.info("Fichier de licence écrit dans {}", path);
        } catch (IOException e) {
            LOG.warn(
                "Impossible d'écrire le fichier de licence dans {} — la copie en base fait foi",
                path, e);
        }
    }

    /**
     * Journalise un événement. L'audit ne doit jamais faire échouer l'opération qu'il observe.
     */
    public void audit(LicenseAuditEventType type, String detail, String licenseId) {
        try {
            licenseAuditRepository.save(new LicenseAudit(type, detail, licenseId,
                SecurityUtils.getCurrentUserLogin().orElse(null)));
        } catch (RuntimeException e) {
            LOG.warn("Échec d'écriture du journal de licence ({})", type, e);
        }
    }

    // ------------------------------------------------------------------ audit

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Entrée de cache. Elle conserve la <strong>licence</strong>, pas le texte à afficher.
     *
     * <p>C'est la distinction qui compte : « expire dans N jour(s) » est un compteur, il ne peut
     * pas être calculé une fois puis resservi. Une caisse d'officine reste ouverte des jours
     * durant, et le frontend mémorise le statut reçu à la connexion .
     *
     * @param payload      licence exploitable, ou {@code null} si l'évaluation n'en a produit
     *                     aucune (absente, signature invalide, panne technique) :
     *                     {@code frozenInfo} fait alors foi
     * @param pinnedStatus statut établi par un contrôle indépendant du temps qui passe (horloge
     *                     reculée, liaison, quota de démo). {@code null} ⇒ statut recalculé sur la
     *                     date à chaque lecture.
     * @param retryAfter   instant à partir duquel l'entrée doit être réévaluée même dans la journée
     *                     — {@code null} pour un statut normal, valorisé après un échec technique
     */
    private record CachedStatus(
        LicensePayload payload,
        LicenseStatus pinnedStatus,
        String pinnedMessage,
        LicenseInfo frozenInfo,
        LocalDate computedOn,
        Instant retryAfter
    ) {

        /**
         * Licence exploitable dont seule l'échéance tranche : tout est recalculé à la lecture.
         */
        static CachedStatus live(LicensePayload payload, LocalDate computedOn) {
            return new CachedStatus(payload, null, null, null, computedOn, null);
        }

        /**
         * Licence exploitable, mais un contrôle indépendant du temps a déjà tranché.
         */
        static CachedStatus pinned(LicensePayload payload, LicenseStatus status, String message,
            LocalDate computedOn) {
            return new CachedStatus(payload, status, message, null, computedOn, null);
        }

        /**
         * Aucune licence exploitable : il n'y a plus rien à recalculer.
         */
        static CachedStatus frozen(LicenseInfo info, LocalDate computedOn) {
            return new CachedStatus(null, null, null, info, computedOn, null);
        }

        /**
         * Repousse la prochaine évaluation. La date de calcul est réalignée sur le jour courant :
         * sans cela, une entrée héritée de la veille resterait périmée et chaque écriture paierait
         * à nouveau le délai de connexion.
         */
        CachedStatus retryingAfter(LocalDate today, Instant instant) {
            return new CachedStatus(payload, pinnedStatus, pinnedMessage, frozenInfo, today,
                instant);
        }

        boolean isStale(LocalDate today, Instant now) {
            return !computedOn.equals(today) || (retryAfter != null && !now.isBefore(retryAfter));
        }
    }
}
