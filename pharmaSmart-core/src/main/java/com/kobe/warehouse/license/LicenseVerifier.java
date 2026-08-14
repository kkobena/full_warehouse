package com.kobe.warehouse.license;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vérifie l'authenticité d'un fichier de licence, au format <strong>JWS compact</strong>
 * ({@code header.payload.signature}) signé en <strong>Ed25519</strong> (cf. PLAN-GESTION-LICENCE §3.1).
 *
 * <p>Ed25519 plutôt que RSA : signature de 64 octets, vérification très rapide et algorithme
 * disponible nativement sur la JVM — <em>aucune dépendance supplémentaire</em>, ce qui compte pour
 * un composant embarqué chez le client.
 *
 * <p>La classe accepte <strong>plusieurs clés publiques</strong> : c'est ce qui rend possible une
 * rotation de la clé de l'éditeur sans invalider d'un coup toutes les licences déjà déployées
 * (cf. §9, « perte de la clé privée »).
 */
public class LicenseVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseVerifier.class);

    private static final String ALGORITHM = "Ed25519";
    private static final String EXPECTED_ALG_HEADER = "EdDSA";

    private final List<PublicKey> publicKeys;

    /**
     * @param publicKeys clés acceptées. Une liste vide est tolérée à la construction — elle ne fait
     *     échouer que la vérification elle-même. Refuser de démarrer serait ici contre-productif :
     *     en profil {@code dev} ou dans les tests, le contrôle de licence est désactivé et la clé
     *     publique n'a pas à être présente.
     */
    public LicenseVerifier(List<PublicKey> publicKeys) {
        this.publicKeys = publicKeys == null ? List.of() : List.copyOf(publicKeys);
    }

    /**
     * Charge les clés publiques depuis des ressources PEM du classpath.
     *
     * @param resourcePaths chemins classpath, la première étant la clé courante
     */
    public static LicenseVerifier fromClasspath(List<String> resourcePaths) {
        List<PublicKey> keys = new ArrayList<>();
        for (String path : resourcePaths) {
            try (InputStream in = LicenseVerifier.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    LOG.warn("Clé publique de licence introuvable dans le classpath : {}", path);
                    continue;
                }
                keys.add(parsePublicKey(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException | GeneralSecurityException e) {
                LOG.error("Clé publique de licence illisible : {}", path, e);
            }
        }
        return new LicenseVerifier(keys);
    }

    /** Décode une clé publique Ed25519 au format PEM (SubjectPublicKeyInfo / X.509). */
    public static PublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "").replaceAll("-----END (.*)-----", "").replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
    }

    /**
     * Vérifie la signature puis désérialise les claims.
     *
     * <p>Ne contrôle <em>que</em> l'intégrité et la structure : les règles métier (échéance,
     * officine, empreinte) relèvent du service, qui seul connaît l'installation.
     *
     * @throws LicenseVerificationException si le jeton est malformé ou si aucune clé publique connue
     *     ne valide la signature
     */
    public LicensePayload verify(String token) throws LicenseVerificationException {
        if (publicKeys.isEmpty()) {
            throw new LicenseVerificationException(
                "Aucune clé publique de licence n'est embarquée dans cette installation : la licence ne peut pas être vérifiée.",
                "license.invalid.noPublicKey"
            );
        }
        if (token == null || token.isBlank()) {
            throw new LicenseVerificationException("Fichier de licence vide", "license.invalid.empty");
        }
        String compact = token.trim();
        String[] parts = compact.split("\\.");
        if (parts.length != 3) {
            throw new LicenseVerificationException("Format de licence invalide (JWS compact attendu)", "license.invalid.format");
        }

        assertHeader(parts[0]);

        byte[] signingInput = (parts[0] + '.' + parts[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] signature;
        try {
            signature = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new LicenseVerificationException("Signature de licence illisible", "license.invalid.signature", e);
        }

        if (!isSignatureValid(signingInput, signature)) {
            throw new LicenseVerificationException(
                "La signature de la licence est invalide : le fichier a été modifié ou provient d'une autre source.",
                "license.invalid.signature"
            );
        }

        return readClaims(parts[1]);
    }

    private void assertHeader(String encodedHeader) throws LicenseVerificationException {
        try {
            var header = LicenseJson.mapper().readTree(Base64.getUrlDecoder().decode(encodedHeader));
            var alg = header.get("alg");
            if (alg == null || !EXPECTED_ALG_HEADER.equals(alg.asText())) {
                throw new LicenseVerificationException(
                    "Algorithme de signature non supporté : " + (alg == null ? "absent" : alg.asText()),
                    "license.invalid.algorithm"
                );
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new LicenseVerificationException("En-tête de licence illisible", "license.invalid.format", e);
        }
    }

    private boolean isSignatureValid(byte[] signingInput, byte[] signature) {
        for (PublicKey key : publicKeys) {
            try {
                Signature verifier = Signature.getInstance(ALGORITHM);
                verifier.initVerify(key);
                verifier.update(signingInput);
                if (verifier.verify(signature)) {
                    return true;
                }
            } catch (GeneralSecurityException e) {
                LOG.debug("Échec de vérification avec une des clés publiques", e);
            }
        }
        return false;
    }

    private LicensePayload readClaims(String encodedPayload) throws LicenseVerificationException {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedPayload);
            return LicenseJson.mapper().readValue(json, LicensePayload.class);
        } catch (IOException | IllegalArgumentException e) {
            throw new LicenseVerificationException("Contenu de la licence illisible", "license.invalid.payload", e);
        }
    }
}
