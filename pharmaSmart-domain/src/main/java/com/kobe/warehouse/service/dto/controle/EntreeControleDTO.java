package com.kobe.warehouse.service.dto.controle;

import com.kobe.warehouse.domain.pharmacovigilance.ContreIndication;
import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tout ce dont le moteur de contrôle a besoin : il ne va chercher aucune donnée lui-même. */
public record EntreeControleDTO(
    Map<Integer, String> panier,
    Map<Integer, List<MoleculeDTO>> moleculesPanier,
    List<TraitementEnCoursDTO> enCours,
    Map<Integer, Set<Integer>> classesParDci,
    Map<Integer, String> classes,
    List<Interaction> interactions,
    List<ContreIndication> contreIndications,
    Map<Integer, String> sources,
    ProfilPatientDTO profil
) {}
