package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.BedDTO;
import com.kobe.warehouse.service.dto.BedImportLigneDTO;
import com.kobe.warehouse.service.dto.BedLigneDTO;
import com.kobe.warehouse.service.dto.BedSummaryDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BedService {

    BedDTO createBed(BedDTO bedDTO);

    BedDTO findById(Integer bedId, LocalDate orderDate);

    Page<BedSummaryDTO> findAll(
        String search,
        MotifBed motifBed,
        OrderStatut orderStatus,
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
    );

    BedDTO addLigne(Integer bedId, LocalDate orderDate, BedLigneDTO ligne);

    BedDTO updateLigne(Integer bedId, LocalDate orderDate, Integer ligneId, LocalDate ligneDate, BedLigneDTO ligne);

    void removeLigne(Integer bedId, LocalDate orderDate, Integer ligneId, LocalDate ligneDate);

    BedDTO validateBed(Integer bedId, LocalDate orderDate, MotifBed motif, Integer fournisseurId, String commentaire);

    void deleteBed(Integer bedId, LocalDate orderDate);

    /**
     * Crée un BED directement à l'état CLOSED à partir d'une importation CSV.
     * Le stock est déjà crédité par l'importation — aucun updateTotalStock n'est appelé.
     *
     * @return la référence du BED créé (ex. "BED-20260418-001")
     */
    String createBedFromImport(MotifBed motif, Integer fournisseurId, List<BedImportLigneDTO> lignes);
}
