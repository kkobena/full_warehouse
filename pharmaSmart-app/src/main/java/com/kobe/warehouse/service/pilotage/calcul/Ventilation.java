package com.kobe.warehouse.service.pilotage.calcul;

import java.util.List;

/** Éléments rapprochés et leur total (sans membre). */
public record Ventilation(List<MesuresComparees> elements, MesuresComparees total) {}
