package com.kobe.warehouse.config;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFileAttributeView;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fournit la paire de clés RSA qui signe les JWT, en la <b>persistant sur disque</b>.
 *
 * <p>Sans persistance, une nouvelle paire est générée à chaque démarrage et tous les
 * tokens émis — access comme refresh — deviennent invalides : chaque redémarrage
 * déconnecte l'ensemble des utilisateurs, y compris en plein comptage d'inventaire.
 *
 * <p>La clé est stockée en PKCS#8 chiffré (mot de passe basé PBE). La clé publique
 * n'est pas stockée : elle est reconstruite depuis la clé privée RSA, qui porte déjà
 * le module et l'exposant public.
 *
 * <p><b>Le mot de passe n'est pas la frontière de sécurité</b> : il protège le fichier
 * contre une copie opportuniste (sauvegarde, poste partagé), mais la protection réelle
 * vient des droits d'accès du fichier, restreints au propriétaire à la création.
 */
@Component
public class JwtSigningKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JwtSigningKeyProvider.class);

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String PBE_ALGORITHM = "PBEWithHmacSHA256AndAES_256";
    private static final int PBE_ITERATIONS = 100_000;
    private static final int SALT_LENGTH = 16;
    private static final String DEFAULT_FILE_NAME = "jwt-signing-key.p8";

    private final String configuredPath;
    private final char[] password;

    public JwtSigningKeyProvider(
        @Value("${pharma-smart.security.jwt-key-path:}") String configuredPath,
        @Value("${pharma-smart.security.jwt-key-password:changeit}") String password
    ) {
        this.configuredPath = configuredPath;
        this.password = password.toCharArray();
    }

    /**
     * Charge la paire de clés depuis le disque, ou la crée au premier démarrage.
     *
     * <p>En cas d'échec (droits insuffisants, fichier corrompu), on retombe sur une clé
     * éphémère : l'application démarre — refuser de démarrer bloquerait le comptoir —
     * mais le comportement dégradé est journalisé en WARN car les sessions ne
     * survivront alors pas au prochain redémarrage.
     */
    public KeyPair getOrCreateKeyPair() {
        Path path = resolveKeyPath();
        try {
            if (Files.exists(path)) {
                KeyPair keyPair = load(path);
                LOG.info("Clé de signature JWT chargée depuis {}", path);
                return keyPair;
            }
            KeyPair keyPair = generate();
            store(keyPair, path);
            LOG.info("Clé de signature JWT générée et enregistrée dans {}", path);
            return keyPair;
        } catch (Exception e) {
            LOG.warn(
                "Impossible de lire ou d'écrire la clé de signature JWT ({}) — repli sur une clé "
                    + "éphémère. Les sessions seront invalidées au prochain redémarrage.",
                path,
                e
            );
            return generate();
        }
    }

    /**
     * Emplacement du fichier de clé.
     *
     * <p>Défaut Windows : {@code %PROGRAMDATA%\PharmaSmart\} — répertoire déjà utilisé par
     * l'application (miroir de config.json), au niveau machine, et qui <b>survit aux mises
     * à jour</b> contrairement au répertoire d'installation que l'installeur écrase.
     * Ailleurs (développement, Linux) : {@code ~/.pharmasmart/}.
     */
    private Path resolveKeyPath() {
        if (StringUtils.hasText(configuredPath)) {
            return Paths.get(configuredPath);
        }
        String programData = System.getenv("PROGRAMDATA");
        Path baseDir = StringUtils.hasText(programData)
            ? Paths.get(programData, "PharmaSmart")
            : Paths.get(System.getProperty("user.home"), ".pharmasmart");
        return baseDir.resolve(DEFAULT_FILE_NAME);
    }

    private KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Échec de génération de la paire de clés RSA", e);
        }
    }

    private KeyPair load(Path path) throws Exception {
        EncryptedPrivateKeyInfo encryptedInfo = new EncryptedPrivateKeyInfo(Files.readAllBytes(path));

        SecretKey pbeKey = SecretKeyFactory
            .getInstance(encryptedInfo.getAlgName())
            .generateSecret(new PBEKeySpec(password));

        Cipher cipher = Cipher.getInstance(encryptedInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedInfo.getAlgParameters());

        PKCS8EncodedKeySpec keySpec = encryptedInfo.getKeySpec(cipher);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return new KeyPair(derivePublicKey(privateKey, keyFactory), privateKey);
    }

    private void store(KeyPair keyPair, Path path) throws Exception {
        Files.createDirectories(path.getParent());

        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(salt);

        SecretKey pbeKey = SecretKeyFactory
            .getInstance(PBE_ALGORITHM)
            .generateSecret(new PBEKeySpec(password));

        Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, pbeKey, new PBEParameterSpec(salt, PBE_ITERATIONS));
        byte[] encrypted = cipher.doFinal(keyPair.getPrivate().getEncoded());

        AlgorithmParameters params = cipher.getParameters();
        byte[] content = new EncryptedPrivateKeyInfo(params, encrypted).getEncoded();

        Files.write(path, content);
        restrictToOwner(path);
    }

    /**
     * La clé privée RSA porte le module et l'exposant public : la clé publique s'en
     * déduit, il est inutile de la stocker séparément.
     */
    private PublicKey derivePublicKey(PrivateKey privateKey, KeyFactory keyFactory) throws Exception {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException(
                "Clé privée RSA sans paramètres CRT : clé publique non dérivable"
            );
        }
        BigInteger modulus = crtKey.getModulus();
        BigInteger publicExponent = crtKey.getPublicExponent();
        return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
    }

    /**
     * Restreint l'accès au seul propriétaire du fichier — c'est là que se joue
     * réellement la protection de la clé.
     */
    private void restrictToOwner(Path path) {
        try {
            PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (posix != null) {
                posix.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                return;
            }
            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (acl != null) {
                AclEntry ownerOnly = AclEntry
                    .newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path))
                    .setPermissions(
                        AclEntryPermission.READ_DATA,
                        AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.APPEND_DATA,
                        AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.WRITE_ATTRIBUTES,
                        AclEntryPermission.READ_ACL,
                        AclEntryPermission.WRITE_ACL,
                        AclEntryPermission.DELETE
                    )
                    .build();
                acl.setAcl(List.of(ownerOnly));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOG.warn(
                "Droits d'accès de la clé de signature JWT non restreints ({}) : {}",
                path,
                e.getMessage()
            );
        }
    }
}
