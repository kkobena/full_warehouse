package com.kobe.warehouse.license;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramétrage du contrôle de licence.
 *
 * <p>Les seuils sont externalisés pour être ajustables sans recompilation : une officine bloquée un
 * jour d'affluence est un incident commercial, et la marge de manœuvre doit rester dans les mains
 * du support (cf. PLAN-GESTION-LICENCE §3.2 et §9).
 */
@ConfigurationProperties(prefix = "pharma-smart.license")
public class LicenseProperties {

    private static final String DEFAULT_FILE_NAME = "license.lic";

    /**
     * Active le contrôle de licence. Positionné à {@code false} en profil {@code dev} et dans les
     * tests afin de ne pas altérer la CI existante.
     */
    private boolean enabled = true;

    /**
     * Emplacement du fichier {@code license.lic}. Vide ⇒ emplacement résolu par
     * {@link #resolveFilePath()}.
     */
    private String filePath;

    /** Ressources classpath des clés publiques acceptées ; la première est la clé courante. */
    private List<String> publicKeys = List.of("license/pharmasmart-public.pem");

    /** Seuil de déclenchement du toast à la connexion (B2). */
    private int warningThresholdDays = 30;

    /** Seuil de déclenchement de la bannière permanente (B3). */
    private int criticalThresholdDays = 14;

    /** Délai de régularisation après un changement de matériel avant blocage (§3.4, couche 2). */
    private int fingerprintMismatchToleranceDays = 14;

    /** Recul d'horloge toléré avant de conclure à une manipulation (§3.3). */
    private int clockToleranceHours = 24;

    /** Taille maximale acceptée pour un fichier de licence déposé, en octets. */
    private int maxUploadBytes = 16 * 1024;

    /**
     * Coordonnées de l'éditeur, affichées dans l'écran de licence pour transmettre une demande
     * d'activation.
     *
     * <p>Distinctes des contacts portés par la licence ({@code SupportContacts}) : ceux-là
     * identifient le <em>revendeur</em> et n'existent qu'une fois la licence valide. Ceux-ci sont
     * dans la configuration, donc disponibles à la toute première installation — précisément le
     * moment où le client n'a encore rien et doit joindre quelqu'un.
     */
    private Publisher publisher = new Publisher();

    /** Coordonnées de l'éditeur du logiciel. */
    public static class Publisher {

        /** Raison sociale affichée. */
        private String name;

        /** Adresses de réception des demandes d'activation ; la première sert de destinataire. */
        private List<String> emails = List.of();

        /** Numéros de téléphone affichés au client. */
        private List<String> phones = List.of();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getEmails() {
            return emails;
        }

        public void setEmails(List<String> emails) {
            this.emails = emails == null ? List.of() : List.copyOf(emails);
        }

        public List<String> getPhones() {
            return phones;
        }

        public void setPhones(List<String> phones) {
            this.phones = phones == null ? List.of() : List.copyOf(phones);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Emplacement effectif du fichier de licence.
     *
     * <p><strong>Ne jamais se rabattre sur {@code user.home} sous Windows.</strong> Le backend y
     * tourne en service {@code LocalSystem} : {@code user.home} y vaut
     * {@code C:\Windows\System32\config\systemprofile}, un répertoire que le pharmacien ne peut ni
     * ouvrir, ni sauvegarder, ni inspecter quand il appelle le support — il chercherait son fichier
     * dans son propre profil et le croirait perdu. On vise donc le même répertoire de données que
     * le reste de l'application ({@code %PROGRAMDATA%\PharmaSmart}) : au niveau machine, quel que
     * soit le compte, et il survit aux mises à jour. Même raisonnement que
     * {@code JwtSigningKeyProvider.resolveKeyPath()}.
     *
     * <p>Le repli {@code ~/.pharmasmart} ne sert qu'au développement et à Linux, où le processus
     * tourne sous un vrai compte utilisateur.
     */
    public Path resolveFilePath() {
        if (filePath != null && !filePath.isBlank()) {
            return Path.of(filePath.trim());
        }
        String programData = System.getenv("PROGRAMDATA");
        Path baseDir = programData != null && !programData.isBlank()
            ? Path.of(programData, "PharmaSmart")
            : Path.of(System.getProperty("user.home"), ".pharmasmart");
        return baseDir.resolve(DEFAULT_FILE_NAME);
    }

    public List<String> getPublicKeys() {
        return publicKeys;
    }

    public void setPublicKeys(List<String> publicKeys) {
        this.publicKeys = publicKeys;
    }

    public int getWarningThresholdDays() {
        return warningThresholdDays;
    }

    public void setWarningThresholdDays(int warningThresholdDays) {
        this.warningThresholdDays = warningThresholdDays;
    }

    public int getCriticalThresholdDays() {
        return criticalThresholdDays;
    }

    public void setCriticalThresholdDays(int criticalThresholdDays) {
        this.criticalThresholdDays = criticalThresholdDays;
    }

    public int getFingerprintMismatchToleranceDays() {
        return fingerprintMismatchToleranceDays;
    }

    public void setFingerprintMismatchToleranceDays(int fingerprintMismatchToleranceDays) {
        this.fingerprintMismatchToleranceDays = fingerprintMismatchToleranceDays;
    }

    public int getClockToleranceHours() {
        return clockToleranceHours;
    }

    public void setClockToleranceHours(int clockToleranceHours) {
        this.clockToleranceHours = clockToleranceHours;
    }

    public int getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(int maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher == null ? new Publisher() : publisher;
    }
}
