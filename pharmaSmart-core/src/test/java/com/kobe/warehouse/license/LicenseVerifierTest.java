package com.kobe.warehouse.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Vérifie le socle cryptographique : une licence authentique est acceptée, toute altération —
 * fût-elle d'un seul octet — est rejetée, et une clé étrangère ne signe rien d'exploitable.
 */
class LicenseVerifierTest {

    private static KeyPair editorKeys;
    private static KeyPair foreignKeys;
    private static LicenseVerifier verifier;

    @BeforeAll
    static void setUp() throws Exception {
        editorKeys = LicenseSigner.generateKeyPair();
        foreignKeys = LicenseSigner.generateKeyPair();
        verifier = new LicenseVerifier(List.of(editorKeys.getPublic()));
    }

    private static LicensePayload samplePayload() {
        return new LicensePayload(
            "b3c1f2e0-0000-0000-0000-000000000001",
            LicenseType.SUBSCRIPTION,
            "CI-ABJ-0042",
            "PHARMACIE DE LA PAIX",
            "PHARMACIE DE LA PAIX SARL",
            "1234567A",
            "STANDARD",
            Instant.parse("2026-01-15T10:00:00Z"),
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2027, 1, 15),
            7,
            10,
            null,
            null,
            EnumSet.of(Feature.CAISSE, Feature.FACTURATION),
            "sha256:9f2c",
            BindingPolicy.MAGASIN_AND_HARDWARE,
            new SupportContacts("PharmaSmart CI", List.of("+225 07 00 00 00 00"), List.of("support@pharmasmart.ci"), null, null)
        );
    }

    private static String sign(KeyPair keys, LicensePayload payload) throws Exception {
        return new LicenseSigner(keys.getPrivate()).sign(payload);
    }

    @Test
    void accepteUneLicenceAuthentiqueEtRestitueTousLesClaims() throws Exception {
        LicensePayload verified = verifier.verify(sign(editorKeys, samplePayload()));

        assertThat(verified.licenseId()).isEqualTo("b3c1f2e0-0000-0000-0000-000000000001");
        assertThat(verified.licenseType()).isEqualTo(LicenseType.SUBSCRIPTION);
        assertThat(verified.magasinName()).isEqualTo("PHARMACIE DE LA PAIX");
        assertThat(verified.taxId()).isEqualTo("1234567A");
        assertThat(verified.expiresAt()).isEqualTo(LocalDate.of(2027, 1, 15));
        assertThat(verified.gracePeriodDays()).isEqualTo(7);
        assertThat(verified.features()).containsExactlyInAnyOrder(Feature.CAISSE, Feature.FACTURATION);
        assertThat(verified.bindingPolicy()).isEqualTo(BindingPolicy.MAGASIN_AND_HARDWARE);
        assertThat(verified.support().primaryPhone()).isEqualTo("+225 07 00 00 00 00");
    }

    @Test
    void rejetteUneLicenceDontUnSeulCaractereDesClaimsAEteModifie() throws Exception {
        String token = sign(editorKeys, samplePayload());
        String[] parts = token.split("\\.");
        // On altère un caractère au milieu du payload sans toucher à la signature :
        // c'est exactement le scénario « le client édite son fichier .lic ».
        char[] claims = parts[1].toCharArray();
        int index = claims.length / 2;
        claims[index] = claims[index] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + '.' + new String(claims) + '.' + parts[2];

        assertThatThrownBy(() -> verifier.verify(tampered))
            .isInstanceOf(LicenseVerificationException.class)
            .hasMessageContaining("signature");
    }

    @Test
    void rejetteUneLicenceSigneeParUneCleEtrangere() throws Exception {
        String token = sign(foreignKeys, samplePayload());

        assertThatThrownBy(() -> verifier.verify(token))
            .isInstanceOf(LicenseVerificationException.class)
            .extracting(e -> ((LicenseVerificationException) e).getErrorKey())
            .isEqualTo("license.invalid.signature");
    }

    @Test
    void accepteUneLicenceSigneeParUneCleDeRotationEncoreReconnue() throws Exception {
        // Deux clés publiques acceptées : c'est ce qui rend une rotation possible sans invalider
        // d'un coup toutes les licences déjà déployées (cf. plan §9).
        LicenseVerifier rotating = new LicenseVerifier(List.of(foreignKeys.getPublic(), editorKeys.getPublic()));

        assertThat(rotating.verify(sign(editorKeys, samplePayload())).licenseId()).isNotNull();
    }

    @Test
    void rejetteUnFichierQuiNestPasUnJws() {
        assertThatThrownBy(() -> verifier.verify("ceci n'est pas une licence"))
            .isInstanceOf(LicenseVerificationException.class)
            .extracting(e -> ((LicenseVerificationException) e).getErrorKey())
            .isEqualTo("license.invalid.format");
    }

    @Test
    void rejetteUnFichierVide() {
        assertThatThrownBy(() -> verifier.verify("   "))
            .isInstanceOf(LicenseVerificationException.class)
            .extracting(e -> ((LicenseVerificationException) e).getErrorKey())
            .isEqualTo("license.invalid.empty");
    }

    @Test
    void uneLicenceExpireLeSoirDeSaDateDecheanceEtNonLaVeille() throws Exception {
        LicensePayload payload = verifier.verify(sign(editorKeys, samplePayload()));

        // Le 15/01 à 23h00 UTC, la licence « expirant le 15/01 » doit encore être valide :
        // c'est l'interprétation commerciale attendue par le client.
        assertThat(payload.expiryInstant()).isAfter(Instant.parse("2027-01-15T23:00:00Z"));
        assertThat(payload.expiryInstant()).isEqualTo(Instant.parse("2027-01-16T00:00:00Z"));
        assertThat(payload.graceEndInstant()).isEqualTo(Instant.parse("2027-01-23T00:00:00Z"));
    }

    @Test
    void uneLicenceSansFeaturesConserveLexistantSansOuvrirDoption() throws Exception {
        LicensePayload sansFeatures = new LicensePayload(
            "id",
            LicenseType.SUBSCRIPTION,
            null,
            "PHARMACIE",
            null,
            null,
            null,
            Instant.now(),
            null,
            LocalDate.of(2030, 1, 1),
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LicensePayload verified = verifier.verify(sign(editorKeys, sansFeatures));

        assertThat(verified.features()).isEmpty();
        for (Feature feature : Feature.values()) {
            // Périmètre couvert d'office accordé, options fermées : une licence émise avant
            // l'introduction des options ne doit ni bloquer un client, ni lui offrir un module payant.
            assertThat(verified.hasFeature(feature)).as("feature %s", feature).isEqualTo(!feature.isOptional());
        }
    }
}
