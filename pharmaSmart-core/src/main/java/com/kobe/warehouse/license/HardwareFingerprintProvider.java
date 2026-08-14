package com.kobe.warehouse.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calcule l'empreinte matérielle du poste serveur, utilisée par la couche 2 de la protection
 * anti-partage (cf. PLAN-GESTION-LICENCE §3.4).
 *
 * <p><strong>Composition retenue</strong> : nom d'hôte + adresses MAC physiques triées.
 * Le plan mentionnait également le numéro de série de la carte mère ; il a été écarté car son
 * obtention impose de lancer un processus système (WMI sous Windows, {@code dmidecode} sous Linux —
 * ce dernier exigeant les droits root). Une valeur qui n'est parfois lisible qu'en fonction des
 * droits d'exécution rendrait l'empreinte <em>non déterministe</em> et provoquerait des divergences
 * fantômes chez des clients pourtant légitimes. Le tri des adresses garantit à l'inverse une valeur
 * stable d'un démarrage à l'autre.
 *
 * <p>L'empreinte reste volontairement approximative : elle n'a pas vocation à être inviolable mais
 * à rendre le partage d'un même fichier entre officines coûteux. La tolérance graduée de 14 jours
 * (§3.4, couche 2) absorbe les changements matériels légitimes.
 */
public class HardwareFingerprintProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HardwareFingerprintProvider.class);
    private static final String PREFIX = "sha256:";

    /** Le calcul parcourt toutes les interfaces réseau : on le fait une fois pour la durée du process. */
    private volatile String cached;

    /**
     * @return empreinte de la forme {@code sha256:<hex>}, jamais {@code null} — en cas d'échec total
     *     de l'introspection réseau, une empreinte dégradée basée sur le seul nom d'hôte est
     *     renvoyée plutôt que de faire échouer le démarrage.
     */
    public String fingerprint() {
        String local = cached;
        if (local == null) {
            synchronized (this) {
                if (cached == null) {
                    cached = compute();
                }
                local = cached;
            }
        }
        return local;
    }

    private String compute() {
        String hostname = hostname();
        List<String> macs = macAddresses();
        String raw = hostname + '|' + String.join(",", macs);
        LOG.debug("Empreinte matérielle calculée à partir de {} interface(s) réseau", macs.size());
        return PREFIX + sha256(raw);
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName().toLowerCase(Locale.ROOT);
        } catch (UnknownHostException e) {
            LOG.warn("Nom d'hôte indisponible pour le calcul de l'empreinte matérielle", e);
            return "unknown-host";
        }
    }

    private List<String> macAddresses() {
        List<String> macs = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                // On écarte loopback et sous-interfaces virtuelles : elles n'identifient pas la machine.
                if (nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                byte[] mac = nic.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    macs.add(HexFormat.of().formatHex(mac));
                }
            }
        } catch (SocketException e) {
            LOG.warn("Interfaces réseau illisibles : empreinte matérielle dégradée", e);
        }
        Collections.sort(macs);
        return macs;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM", e);
        }
    }
}
