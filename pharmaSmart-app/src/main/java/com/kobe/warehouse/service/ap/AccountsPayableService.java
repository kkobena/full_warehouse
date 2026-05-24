package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.domain.enumeration.StatutLigneFournisseurAP;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import com.kobe.warehouse.service.dto.ReglementBLDTO;
import com.kobe.warehouse.service.dto.ReglementFournisseurAPCommand;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountsPayableService {

    List<CompteFournisseurAPDTO> getComptes(LocalDate fromDate, LocalDate toDate);

    default List<CompteFournisseurAPDTO> getComptes() {
        return getComptes(null, null);
    }

    FournisseurAPSummaryDTO getSummary();

    Page<LigneFournisseurAPDTO> getLignes(Integer fournisseurId, StatutLigneFournisseurAP statut, Pageable pageable);

    List<ReglementBLDTO> getReglementsBl(Integer fournisseurId, Integer commandeId);

    void enregistrerReglement(Integer fournisseurId, ReglementFournisseurAPCommand command);

    byte[] exportComptesAsPdf(LocalDate fromDate, LocalDate toDate);

    byte[] exportFournisseurAsPdf(Integer fournisseurId);

    long countOverdue();
}
