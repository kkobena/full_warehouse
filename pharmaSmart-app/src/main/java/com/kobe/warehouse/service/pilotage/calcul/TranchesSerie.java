package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.util.List;

public record TranchesSerie(List<PeriodeDTO> decoupage, List<MesuresPilotage> mesures, MesuresPilotage total) {
    public static final TranchesSerie AUCUNE = new TranchesSerie(List.of(), List.of(), MesuresPilotage.AUCUNE);

    public boolean estVide() {
        return decoupage.isEmpty();
    }
}
