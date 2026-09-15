package com.kobe.warehouse.service.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Catalogue des mises à jour destinées aux <b>postes clients</b> de l'officine.
 *
 * <p>Les postes clients ne sont pas installés : on y dépose {@code pharmasmart.exe}. Le poste
 * serveur sert de relais — il héberge l'exécutable client de sa propre version, déposé par
 * l'installeur NSIS dans {@code %PROGRAMDATA%/PharmaSmart/updates}. Les postes interrogent
 * {@code /api/updates/**}, téléchargent le binaire et le remplacent (cf.
 * {@code src-tauri/src/self_update.rs}), sans que l'éditeur ait à héberger quoi que ce soit.
 *
 * <p><b>Cohérence front / back.</b> L'exécutable présent ici a été déposé par la même installation
 * que le JAR en cours d'exécution : un poste client ne peut donc pas se retrouver sur une version
 * d'Angular en décalage avec le backend qu'il interroge.
 *
 * <p><b>Dégradation volontaire.</b> Tout est optionnel. Répertoire absent ou vide : le service ne
 * propose rien et les postes restent mis à jour par dépôt manuel du fichier, exactement comme
 * avant l'introduction de ce mécanisme.
 */
@Service
public class ClientUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientUpdateService.class);

    /**
     * L'artefact publié est l'exécutable client lui-même, nommé
     * {@code pharmasmart-<version>.exe} par {@code scripts/prepare-client-update.js}.
     *
     * <p>Le poste client n'est pas installé : on y dépose {@code pharmasmart.exe}. Mettre à jour
     * revient donc à remplacer ce fichier, pas à exécuter un installeur — le binaire ne portant
     * aucun numéro de version dans ses métadonnées exploitables ici, <b>c'est le nom de fichier qui
     * porte la version</b>. Le motif reste volontairement large pour accepter aussi un installeur
     * nommé à la façon de Tauri ({@code PharmaSmart_1.4.2_x64-setup.exe}).
     */
    private static final Pattern VERSIONED_EXECUTABLE = Pattern.compile("^.*?(\\d+\\.\\d+\\.\\d+).*\\.exe$");

    private final Path updatesDirectory;

    public ClientUpdateService(@Value("${pharma-smart.updates.directory:}") String configuredDirectory) {
        this.updatesDirectory = resolveDirectory(configuredDirectory);
        LOG.info("Catalogue des mises à jour client : {}", this.updatesDirectory);
    }

    /**
     * Répertoire du catalogue. À défaut de propriété explicite, on retombe sur l'emplacement où
     * l'installeur NSIS dépose les artefacts pour une installation « tous utilisateurs ».
     */
    private static Path resolveDirectory(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        String programData = System.getenv("PROGRAMDATA");
        Path base = (programData == null || programData.isBlank())
            ? Paths.get(System.getProperty("java.io.tmpdir"))
            : Paths.get(programData);
        return base.resolve("PharmaSmart").resolve("updates").toAbsolutePath().normalize();
    }

    /**
     * Retourne l'exécutable client publiable le plus récent, ou {@link Optional#empty()} s'il n'y a
     * rien à proposer.
     */
    public Optional<ClientUpdateArtifact> findLatest() {
        if (!Files.isDirectory(updatesDirectory)) {
            LOG.debug("Aucun catalogue de mise à jour : {} n'existe pas", updatesDirectory);
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(updatesDirectory)) {
            List<ClientUpdateArtifact> candidates = files
                .filter(Files::isRegularFile)
                .map(this::toArtifact)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ClientUpdateArtifact::version))
                .toList();

            return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(candidates.size() - 1));
        } catch (IOException e) {
            LOG.warn("Lecture du catalogue {} impossible : {}", updatesDirectory, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrouve un artefact par son nom de fichier, en n'acceptant que ce que le catalogue publie
     * réellement.
     *
     * <p>C'est la protection contre la traversée de répertoire : le nom reçu n'est jamais concaténé
     * au chemin du catalogue, il est comparé aux noms effectivement listés. Un {@code ..} ou un
     * chemin absolu ne correspond à aucune entrée et ressort en {@link Optional#empty()}.
     */
    public Optional<ClientUpdateArtifact> findByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        if (!Files.isDirectory(updatesDirectory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(updatesDirectory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals(fileName))
                .map(this::toArtifact)
                .flatMap(Optional::stream)
                .findFirst();
        } catch (IOException e) {
            LOG.warn("Lecture du catalogue {} impossible : {}", updatesDirectory, e.getMessage());
            return Optional.empty();
        }
    }

    /** Construit l'artefact si le nom de fichier porte bien un numéro de version. */
    private Optional<ClientUpdateArtifact> toArtifact(Path installer) {
        String name = installer.getFileName().toString();
        Matcher matcher = VERSIONED_EXECUTABLE.matcher(name);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        Instant publishedAt;
        try {
            publishedAt = Files.getLastModifiedTime(installer).toInstant();
        } catch (IOException e) {
            LOG.warn("Artefact {} illisible : {}", name, e.getMessage());
            return Optional.empty();
        }

        return Optional.of(new ClientUpdateArtifact(matcher.group(1), name, installer, publishedAt));
    }
}
