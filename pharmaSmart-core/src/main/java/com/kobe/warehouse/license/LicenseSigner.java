package com.kobe.warehouse.license;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Émission d'un fichier de licence : sérialise les claims et les signe en Ed25519, au format JWS
 * compact attendu par {@link LicenseVerifier}.
 *
 * <p><strong>Pourquoi cette classe vit dans un module livré au client ?</strong> Parce que la
 * sécurité du dispositif repose entièrement sur le secret de la <em>clé privée</em>, jamais sur la
 * confidentialité de l'algorithme (principe de Kerckhoffs) : Ed25519 est un standard public que
 * quiconque peut réimplémenter en quelques lignes. La factoriser ici garantit en revanche que
 * l'émetteur et le vérificateur produisent et attendent exactement la même représentation JSON —
 * une divergence entre les deux invaliderait silencieusement toutes les licences émises.
 *
 * <p>La clé privée, elle, ne doit jamais quitter le coffre de l'éditeur.
 */
public final class LicenseSigner {

    private static final String ALGORITHM = "Ed25519";
    private static final String HEADER_JSON = "{\"alg\":\"EdDSA\",\"typ\":\"JWT\"}";

    private final PrivateKey privateKey;

    public LicenseSigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /** Génère une paire Ed25519 — usage éditeur uniquement. */
    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
    }

    /** Décode une clé privée Ed25519 au format PEM (PKCS#8). */
    public static PrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "").replaceAll("-----END (.*)-----", "").replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /** @return le fichier {@code .lic}, c'est-à-dire un JWS compact {@code header.payload.signature}. */
    public String sign(LicensePayload payload) throws GeneralSecurityException, JsonProcessingException {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String claims = encoder.encodeToString(LicenseJson.mapper().writeValueAsBytes(payload));

        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initSign(privateKey);
        signature.update((header + '.' + claims).getBytes(StandardCharsets.US_ASCII));

        return header + '.' + claims + '.' + encoder.encodeToString(signature.sign());
    }

    /** Encode une clé au format PEM, en lignes de 64 caractères. */
    public static String toPem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, System.lineSeparator().getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN %s-----%n%s%n-----END %s-----%n".formatted(label, body, label);
    }
}
