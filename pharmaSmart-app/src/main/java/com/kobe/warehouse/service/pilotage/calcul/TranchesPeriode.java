package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/** TranchesPeriode de la période, retrouvées par leur premier jour ; le libellé suit le découpage. */
public record TranchesPeriode(NavigableMap<LocalDate, MembreAnalyseDTO> parDebut) {
    public static TranchesPeriode decouper(PeriodeDTO periode, Granularite granularite) {
        NavigableMap<LocalDate, MembreAnalyseDTO> parDebut = new TreeMap<>();
        List<PeriodeDTO> decoupage = DecoupagePeriode.decouper(periode, granularite);
        for (int rang = 0; rang < decoupage.size(); rang++) {
            PeriodeDTO tranche = decoupage.get(rang);
            parDebut.put(tranche.du(), new MembreAnalyseDTO(String.valueOf(rang), LibellesPilotage.libellerTranche(tranche, granularite)));
        }
        return new TranchesPeriode(parDebut);
    }

    public MembreAnalyseDTO lireMembre(LocalDate jour) {
        return parDebut.floorEntry(jour).getValue();
    }
}
