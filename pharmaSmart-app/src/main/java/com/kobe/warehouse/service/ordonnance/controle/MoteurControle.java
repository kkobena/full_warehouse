package com.kobe.warehouse.service.ordonnance.controle;

import com.kobe.warehouse.domain.pharmacovigilance.ContreIndication;
import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import com.kobe.warehouse.domain.pharmacovigilance.NiveauInteraction;
import com.kobe.warehouse.service.dto.controle.AlerteControleDTO;
import com.kobe.warehouse.service.dto.controle.ControleResultatDTO;
import com.kobe.warehouse.service.dto.controle.EntreeControleDTO;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import com.kobe.warehouse.service.dto.controle.ProduitNonControleDTO;
import com.kobe.warehouse.service.dto.controle.ProfilPatientDTO;
import com.kobe.warehouse.service.dto.controle.TraitementEnCoursDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Règles du contrôle, sans accès aux données : mêmes entrées, mêmes alertes. Le service charge, le
 * moteur décide, et chaque alerte dit pourquoi elle est levée.
 */
public final class MoteurControle {

    private MoteurControle() {}

    public static ControleResultatDTO evaluer(EntreeControleDTO e) {
        List<ProduitNonControleDTO> nonControles = new ArrayList<>();
        e.panier().forEach((id, libelle) -> {
            if (e.moleculesPanier().getOrDefault(id, List.of()).isEmpty()) {
                nonControles.add(new ProduitNonControleDTO(id, libelle));
            }
        });
        List<AlerteControleDTO> alertes = new ArrayList<>();
        alertes.addAll(interactions(e));
        alertes.addAll(redondances(e));
        alertes.addAll(contreIndications(e));
        alertes.sort(Comparator.comparing(AlerteControleDTO::niveau).thenComparing(AlerteControleDTO::type));
        return new ControleResultatDTO(alertes, nonControles, !e.sources().isEmpty(), ControleResultatDTO.LIMITE);
    }

    // ── Interactions ──

    private static List<AlerteControleDTO> interactions(EntreeControleDTO e) {
        Map<String, AlerteControleDTO> parCouple = new LinkedHashMap<>();
        for (Interaction i : e.interactions()) {
            Set<Integer> cotesA = membres(e, i.getARefDciId(), i.getAClasseId());
            Set<Integer> cotesB = membres(e, i.getBRefDciId(), i.getBClasseId());
            for (int x : cotesA) {
                for (int y : cotesB) {
                    if (x == y || deuxMoleculesDuMemeProduit(e, x, y)) {
                        continue;
                    }
                    List<Integer> produitsX = produitsPanier(e, x);
                    List<Integer> produitsY = produitsPanier(e, y);
                    // Deux traitements déjà en cours ne regardent pas la vente du jour.
                    if (produitsX.isEmpty() && produitsY.isEmpty()) {
                        continue;
                    }
                    String cle = Math.min(x, y) + "-" + Math.max(x, y);
                    AlerteControleDTO existante = parCouple.get(cle);
                    if (existante != null && existante.niveau().compareTo(i.getNiveau()) <= 0) {
                        continue;
                    }
                    List<Integer> concernes = new ArrayList<>(produitsX);
                    produitsY.stream().filter(p -> !concernes.contains(p)).forEach(concernes::add);
                    parCouple.put(
                        cle,
                        new AlerteControleDTO(
                            i.getNiveau(),
                            "INTERACTION",
                            origine(e, x, produitsX) + " et " + origine(e, y, produitsY) + " : " + i.getNiveau(),
                            i.getConduite(),
                            e.sources().get(i.getVersionId()),
                            concernes
                        )
                    );
                }
            }
        }
        return new ArrayList<>(parCouple.values());
    }

    /** Molécules présentes (panier ou en cours) que vise un côté : la molécule elle-même, ou les membres de la classe. */
    private static Set<Integer> membres(EntreeControleDTO e, Integer refDciId, Integer classeId) {
        Set<Integer> resultat = new LinkedHashSet<>();
        for (int dci : presentes(e)) {
            if (
                (refDciId != null && refDciId == dci) ||
                (classeId != null && e.classesParDci().getOrDefault(dci, Set.of()).contains(classeId))
            ) {
                resultat.add(dci);
            }
        }
        return resultat;
    }

    private static Set<Integer> presentes(EntreeControleDTO e) {
        Set<Integer> presentes = new LinkedHashSet<>();
        e.moleculesPanier().values().forEach(l -> l.forEach(m -> presentes.add(m.refDciId())));
        e.enCours().forEach(c -> presentes.add(c.molecule().refDciId()));
        return presentes;
    }

    private static boolean deuxMoleculesDuMemeProduit(EntreeControleDTO e, int x, int y) {
        return e
            .moleculesPanier()
            .values()
            .stream()
            .anyMatch(l -> l.stream().anyMatch(m -> m.refDciId() == x) && l.stream().anyMatch(m -> m.refDciId() == y));
    }

    private static List<Integer> produitsPanier(EntreeControleDTO e, int refDciId) {
        return e
            .moleculesPanier()
            .entrySet()
            .stream()
            .filter(en -> en.getValue().stream().anyMatch(m -> m.refDciId() == refDciId))
            .map(Map.Entry::getKey)
            .toList();
    }

    private static String libelle(EntreeControleDTO e, int refDciId) {
        return e
            .moleculesPanier()
            .values()
            .stream()
            .flatMap(List::stream)
            .filter(m -> m.refDciId() == refDciId)
            .map(MoleculeDTO::libelle)
            .findFirst()
            .orElseGet(() ->
                e
                    .enCours()
                    .stream()
                    .filter(c -> c.molecule().refDciId() == refDciId)
                    .map(c -> c.molecule().libelle())
                    .findFirst()
                    .orElse("?")
            );
    }

    private static String origine(EntreeControleDTO e, int refDciId, List<Integer> produitsPanier) {
        String molecule = libelle(e, refDciId);
        if (!produitsPanier.isEmpty()) {
            return molecule + " (" + e.panier().get(produitsPanier.getFirst()) + ")";
        }
        String produit = e
            .enCours()
            .stream()
            .filter(c -> c.molecule().refDciId() == refDciId)
            .map(TraitementEnCoursDTO::produitLibelle)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("traitement en cours");
        return molecule + " (en cours : " + produit + ")";
    }

    // ── Redondances ──

    private static List<AlerteControleDTO> redondances(EntreeControleDTO e) {
        List<AlerteControleDTO> alertes = new ArrayList<>();
        Set<Integer> moleculesPanier = new TreeSet<>();
        e.moleculesPanier().values().forEach(l -> l.forEach(m -> moleculesPanier.add(m.refDciId())));

        for (int dci : moleculesPanier) {
            List<Integer> produits = produitsPanier(e, dci);
            if (produits.size() > 1) {
                alertes.add(
                    new AlerteControleDTO(
                        NiveauInteraction.APEC,
                        "REDONDANCE",
                        "La molécule " + libelle(e, dci) + " figure dans plusieurs produits du panier : " + noms(e, produits) + ".",
                        null,
                        null,
                        produits
                    )
                );
            }
            // Le même produit déjà pris est un renouvellement, pas une redondance.
            List<String> autres = e
                .enCours()
                .stream()
                .filter(c -> c.molecule().refDciId() == dci && (c.produitId() == null || !e.panier().containsKey(c.produitId())))
                .map(c -> c.produitLibelle() == null ? "traitement en cours" : c.produitLibelle())
                .distinct()
                .toList();
            if (!autres.isEmpty()) {
                alertes.add(
                    new AlerteControleDTO(
                        NiveauInteraction.APEC,
                        "REDONDANCE",
                        "La molécule " + libelle(e, dci) + " est déjà prise par le client (" + String.join(", ", autres) + ").",
                        null,
                        null,
                        produits
                    )
                );
            }
        }

        // Même classe, molécules différentes, dont l'une au moins est au panier.
        Map<Integer, Set<Integer>> classeVersDcis = new LinkedHashMap<>();
        for (int dci : presentes(e)) {
            e.classesParDci().getOrDefault(dci, Set.of()).forEach(c -> classeVersDcis.computeIfAbsent(c, k -> new TreeSet<>()).add(dci));
        }
        classeVersDcis.forEach((classe, dcis) -> {
            List<Integer> produits = dcis.stream().flatMap(d -> produitsPanier(e, d).stream()).distinct().toList();
            if (dcis.size() < 2 || produits.isEmpty()) {
                return;
            }
            String molecules = dcis.stream().map(d -> libelle(e, d)).collect(Collectors.joining(", "));
            alertes.add(
                new AlerteControleDTO(
                    NiveauInteraction.APEC,
                    "REDONDANCE_CLASSE",
                    "Plusieurs molécules de la classe « " + e.classes().getOrDefault(classe, "?") + " » : " + molecules + ".",
                    null,
                    null,
                    produits
                )
            );
        });
        return alertes;
    }

    private static String noms(EntreeControleDTO e, List<Integer> produits) {
        return produits.stream().map(e.panier()::get).collect(Collectors.joining(", "));
    }

    // ── Contre-indications liées au profil ──

    private static List<AlerteControleDTO> contreIndications(EntreeControleDTO e) {
        List<AlerteControleDTO> alertes = new ArrayList<>();
        for (ContreIndication c : e.contreIndications()) {
            List<Integer> produits = produitsPanier(e, c.getRefDciId());
            if (produits.isEmpty() || !sApplique(c, e.profil())) {
                continue;
            }
            alertes.add(
                new AlerteControleDTO(
                    c.getNiveau(),
                    "CONTRE_INDICATION",
                    libelle(e, c.getRefDciId()) +
                    " (" +
                    e.panier().get(produits.getFirst()) +
                    ") : " +
                    intitule(c) +
                    (c.getMotif() == null ? "" : " — " + c.getMotif()),
                    c.getConduite(),
                    e.sources().get(c.getVersionId()),
                    produits
                )
            );
        }
        return alertes;
    }

    private static boolean sApplique(ContreIndication c, ProfilPatientDTO p) {
        BigDecimal seuil = c.getValeurNum();
        return switch (c.getCritere()) {
            case AGE_MIN -> p.age() != null && seuil != null && BigDecimal.valueOf(p.age()).compareTo(seuil) < 0;
            case AGE_MAX -> p.age() != null && seuil != null && BigDecimal.valueOf(p.age()).compareTo(seuil) > 0;
            case SEXE -> p.sexe() != null && p.sexe().equalsIgnoreCase(c.getValeurTexte());
            case GROSSESSE -> p.grossesse();
            case ALLAITEMENT -> p.allaitement();
        };
    }

    private static String intitule(ContreIndication c) {
        return switch (c.getCritere()) {
            case AGE_MIN -> "contre-indiqué avant " + c.getValeurNum().stripTrailingZeros().toPlainString() + " ans";
            case AGE_MAX -> "contre-indiqué après " + c.getValeurNum().stripTrailingZeros().toPlainString() + " ans";
            case SEXE -> "contre-indiqué selon le sexe du patient";
            case GROSSESSE -> "contre-indiqué pendant la grossesse";
            case ALLAITEMENT -> "contre-indiqué pendant l'allaitement";
        };
    }
}
