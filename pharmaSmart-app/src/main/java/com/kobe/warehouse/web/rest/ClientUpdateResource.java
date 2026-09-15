package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.update.ClientUpdateArtifact;
import com.kobe.warehouse.service.update.ClientUpdateService;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Relais de mise à jour pour les postes clients de l'officine.
 *
 * <p>Contrat : répondre {@code 204 No Content} quand il n'y a rien à installer, sinon {@code 200}
 * avec un manifeste JSON {@code {version, pub_date, url, notes}} que le poste client consomme
 * (cf. {@code src-tauri/src/self_update.rs}).
 *
 * <p><b>Accès non authentifié, et c'est nécessaire :</b> l'updater interroge cet endpoint au
 * démarrage du poste, avant toute session utilisateur, et n'a aucun jeton à présenter. Ce que l'on
 * expose en échange reste limité : le numéro de version et l'exécutable client, sur le seul réseau
 * local de l'officine — soit exactement ce qu'un opérateur y copie aujourd'hui à la main.
 *
 * <p>Ces routes sont en lecture seule : l'aspect de licence n'intercepte que les verbes mutants, et
 * n'a donc pas à être exempté ici.
 */
@RestController
@RequestMapping("/api/updates")
public class ClientUpdateResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClientUpdateResource.class);

    private final ClientUpdateService clientUpdateService;

    public ClientUpdateResource(ClientUpdateService clientUpdateService) {
        this.clientUpdateService = clientUpdateService;
    }

    /**
     * Point d'entrée interrogé par l'updater du poste client.
     *
     * @param target plateforme demandée ({@code windows}, {@code darwin}, {@code linux})
     * @param arch architecture demandée ({@code x86_64}…)
     * @param currentVersion version actuellement installée sur le poste client
     */
    @GetMapping("/{target}/{arch}/{currentVersion}")
    public ResponseEntity<Map<String, String>> checkForUpdate(
        @PathVariable(name = "target") String target,
        @PathVariable(name = "arch") String arch,
        @PathVariable(name = "currentVersion") String currentVersion
    ) {
        // Le poste client est un livrable Windows ; rien à proposer ailleurs.
        if (!target.toLowerCase().startsWith("windows")) {
            LOG.debug("Demande de mise à jour ignorée pour la cible {}/{}", target, arch);
            return ResponseEntity.noContent().build();
        }

        Optional<ClientUpdateArtifact> latest = clientUpdateService.findLatest();
        if (latest.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        ClientUpdateArtifact artifact = latest.get();
        if (artifact.version().equals(currentVersion)) {
            return ResponseEntity.noContent().build();
        }

        // URL construite à partir de la requête entrante : le poste client reçoit donc l'adresse
        // par laquelle il vient lui-même de joindre le serveur, sans qu'aucune IP soit à configurer.
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/updates/download/")
            .path(artifact.fileName())
            .toUriString();

        LOG.info("Mise à jour proposée à un poste client : {} → {}", currentVersion, artifact.version());

        return ResponseEntity.ok(
            Map.of(
                "version", artifact.version(),
                "pub_date", artifact.publishedAt().toString(),
                "url", downloadUrl,
                "notes", "Mise à jour PharmaSmart " + artifact.version()
            )
        );
    }

    /** Sert l'installeur. Seuls les fichiers réellement publiés par le catalogue sont accessibles. */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable(name = "fileName") String fileName) throws IOException {
        Optional<ClientUpdateArtifact> artifact = clientUpdateService.findByFileName(fileName);
        if (artifact.isEmpty()) {
            LOG.warn("Téléchargement refusé pour un artefact non publié : {}", fileName);
            return ResponseEntity.notFound().build();
        }

        ClientUpdateArtifact found = artifact.get();
        String contentType = Optional.ofNullable(URLConnection.guessContentTypeFromName(found.fileName()))
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + found.fileName() + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(Files.size(found.path()))
            .body(new FileSystemResource(found.path()));
    }
}
