package com.kobe.warehouse.service.referentiel;

import java.util.List;

/**
 * Des DCI viennent d'être ajoutées au catalogue : à relier au référentiel et à rapprocher des
 * produits. Publié dans la transaction de l'ajout, traité après son commit.
 */
public record DcisAjouteesEvent(List<Integer> dciIds) {}
