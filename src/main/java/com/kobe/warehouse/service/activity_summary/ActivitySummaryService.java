package com.kobe.warehouse.service.activity_summary;

import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivitySummaryService {
    ChiffreAffaireDTO getChiffreAffaire(LocalDate fromDate, LocalDate toDate);

    List<Recette> findRecettes(LocalDate fromDate, LocalDate toDate);

    Page<AchatTiersPayant> fetchAchatTiersPayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable);

    Page<ReglementTiersPayants> findReglementTierspayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable);

    Page<GroupeFournisseurAchat> fetchAchats(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<MouvementCaisse> findMouvementsCaisse(LocalDate fromDate, LocalDate toDate);
}
