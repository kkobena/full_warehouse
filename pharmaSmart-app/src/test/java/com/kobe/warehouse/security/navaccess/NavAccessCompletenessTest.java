package com.kobe.warehouse.security.navaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garde-fou de complétude (docs/PLAN-SECURISATION-ENDPOINTS.md § 3.2) : tout endpoint de
 * {@code /api/**} déclare son droit ({@link RequiresNavAccess}), son exemption
 * ({@link NavAccessExempt}) ou un contrôle Spring Security. Un nouvel endpoint ne peut plus être
 * oublié.
 */
@DisplayName("Sécurisation des endpoints — complétude")
class NavAccessCompletenessTest {

    /**
     * Contrôleurs pas encore classés, tolérés pendant la période d'audit. La liste doit se vider
     * lot après lot ; vide, le test devient strict (Lot 6).
     */
    private static final Set<String> LISTE_D_ATTENTE = Set.of();

    @Test
    @DisplayName("chaque endpoint de /api/** déclare son droit ou son exemption")
    void toutEndpointEstClasse() {
        List<String> nonClasses = NavAccessHandlers.scan()
            .stream()
            .filter(h -> !h.coveredByFilterChain())
            .filter(h -> h.rule() instanceof NavAccessRules.Unclassified)
            .filter(h -> !LISTE_D_ATTENTE.contains(h.type().getSimpleName()))
            .map(h -> h.name() + " " + h.paths())
            .sorted()
            .toList();
        assertThat(nonClasses).as("endpoints sans @RequiresNavAccess ni @NavAccessExempt").isEmpty();
    }

    @Test
    @DisplayName("la liste d'attente ne garde que des contrôleurs encore à traiter")
    void listeDAttenteAJour() {
        Set<String> encoreATraiter = NavAccessHandlers.scan()
            .stream()
            .filter(h -> !h.coveredByFilterChain())
            .filter(h -> h.rule() instanceof NavAccessRules.Unclassified)
            .map(h -> h.type().getSimpleName())
            .collect(Collectors.toSet());
        assertThat(encoreATraiter).as("contrôleurs traités à retirer de la liste d'attente").containsAll(LISTE_D_ATTENTE);
    }

    @Test
    @DisplayName("une exemption porte une justification, une exigence au moins un code")
    void declarationsCompletes() {
        List<String> incompletes = NavAccessHandlers.scan()
            .stream()
            .filter(h ->
                switch (h.rule()) {
                    case NavAccessRules.Exempt e -> e.justification().isBlank();
                    case NavAccessRules.Requires r -> r.codes().length == 0 || List.of(r.codes()).stream().anyMatch(String::isBlank);
                    default -> false;
                }
            )
            .map(NavAccessHandlers.Handler::name)
            .toList();
        assertThat(incompletes).isEmpty();
    }
}
