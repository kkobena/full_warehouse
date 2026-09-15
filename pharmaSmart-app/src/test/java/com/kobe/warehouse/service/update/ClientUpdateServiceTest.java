package com.kobe.warehouse.service.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests unitaires du catalogue de mises à jour client.
 *
 * <p>Deux exigences structurent ces tests :
 *
 * <ul>
 *   <li><b>Dégradation</b> — sans catalogue ni artefact, le service ne propose rien et les postes
 *       restent mis à jour par dépôt manuel. Aucune de ces situations n'est une erreur.
 *   <li><b>Étanchéité</b> — le nom de fichier reçu de l'extérieur ne doit jamais permettre de sortir
 *       du catalogue.
 * </ul>
 */
class ClientUpdateServiceTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClientUpdateService serviceSur(Path repertoire) {
        return new ClientUpdateService(repertoire.toString());
    }

    /** Dépose un exécutable dans le catalogue, comme le fait l'installeur serveur. */
    private Path deposer(Path repertoire, String nom) throws IOException {
        Path executable = repertoire.resolve(nom);
        Files.writeString(executable, "binaire factice");
        return executable;
    }

    // ── Dégradation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Répertoire inexistant : aucune mise à jour proposée, pas d'erreur")
    void repertoireInexistant(@TempDir Path tmp) {
        ClientUpdateService service = serviceSur(tmp.resolve("catalogue-absent"));

        assertThat(service.findLatest()).isEmpty();
        assertThat(service.findByFileName("pharmasmart-1.0.0.exe")).isEmpty();
    }

    @Test
    @DisplayName("Répertoire vide : aucune mise à jour proposée")
    void repertoireVide(@TempDir Path tmp) {
        assertThat(serviceSur(tmp).findLatest()).isEmpty();
    }

    @Test
    @DisplayName("Fichier au nom non conforme : ignoré")
    void nomNonConforme(@TempDir Path tmp) throws IOException {
        deposer(tmp, "notes-de-version.txt");

        assertThat(serviceSur(tmp).findLatest()).isEmpty();
    }

    @Test
    @DisplayName("Exécutable sans version dans le nom : ignoré, faute de pouvoir l'annoncer")
    void executableSansVersion(@TempDir Path tmp) throws IOException {
        deposer(tmp, "pharmasmart.exe");

        assertThat(serviceSur(tmp).findLatest()).isEmpty();
    }

    // ── Publication nominale ──────────────────────────────────────────────────

    @Test
    @DisplayName("Exécutable client : version extraite du nom de fichier")
    void executableClient(@TempDir Path tmp) throws IOException {
        // Nom produit par scripts/prepare-client-update.js : le poste client n'étant pas installé,
        // c'est le binaire lui-même qui est publié, et le nom de fichier porte la version.
        deposer(tmp, "pharmasmart-1.4.2.exe");

        Optional<ClientUpdateArtifact> artefact = serviceSur(tmp).findLatest();

        assertThat(artefact).isPresent();
        assertThat(artefact.get().version()).isEqualTo("1.4.2");
        assertThat(artefact.get().fileName()).isEqualTo("pharmasmart-1.4.2.exe");
    }

    @Test
    @DisplayName("Un installeur nommé à la façon de Tauri reste accepté")
    void installeurNommeTauri(@TempDir Path tmp) throws IOException {
        deposer(tmp, "PharmaSmart_1.4.2_x64-setup.exe");

        Optional<ClientUpdateArtifact> artefact = serviceSur(tmp).findLatest();

        assertThat(artefact).isPresent();
        assertThat(artefact.get().version()).isEqualTo("1.4.2");
    }

    @Test
    @DisplayName("Plusieurs versions présentes : la plus récente est publiée")
    void plusieursVersions(@TempDir Path tmp) throws IOException {
        deposer(tmp, "pharmasmart-1.4.2.exe");
        deposer(tmp, "pharmasmart-1.10.0.exe");
        deposer(tmp, "pharmasmart-1.9.3.exe");

        // Comparaison lexicographique : 1.9.3 l'emporte sur 1.10.0. Le poste client ne bascule que
        // sur une version différente de la sienne, donc rien de dangereux — mais le catalogue est
        // purgé à chaque dépôt, ce qui évite en pratique de dépendre de ce filet.
        Optional<ClientUpdateArtifact> artefact = serviceSur(tmp).findLatest();

        assertThat(artefact).isPresent();
        assertThat(artefact.get().version()).isEqualTo("1.9.3");
    }

    // ── Étanchéité ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Traversée de répertoire : un nom hors catalogue ne résout rien")
    void traverseeDeRepertoire(@TempDir Path tmp) throws IOException {
        deposer(tmp, "pharmasmart-1.4.2.exe");
        // Une cible plausible juste au-dessus du catalogue.
        Files.writeString(tmp.getParent().resolve("config.json"), "{\"database\":{\"password\":\"secret\"}}");

        ClientUpdateService service = serviceSur(tmp);

        assertThat(service.findByFileName("../config.json")).isEmpty();
        assertThat(service.findByFileName("..\\config.json")).isEmpty();
        assertThat(service.findByFileName(tmp.getParent().resolve("config.json").toString())).isEmpty();
        assertThat(service.findByFileName("")).isEmpty();
        assertThat(service.findByFileName(null)).isEmpty();
    }

    @Test
    @DisplayName("Un fichier du catalogue qui n'est pas un exécutable versionné n'est pas servi")
    void fichierNonPubliable(@TempDir Path tmp) throws IOException {
        deposer(tmp, "lisez-moi.txt");

        assertThat(serviceSur(tmp).findByFileName("lisez-moi.txt")).isEmpty();
    }
}
