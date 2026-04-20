package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.ReponseRetourBon;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AvoirFournisseurService {

    AvoirFournisseurDTO createFromReponseRetourBon(ReponseRetourBon reponseRetourBon);

    Page<AvoirFournisseurDTO> findAll(AvoirFournisseurStatut statut, Integer fournisseurId,
                                      LocalDate dtStart, LocalDate dtEnd, Pageable pageable);

    List<AvoirEncoursFournisseurDTO> getEncoursParFournisseur();

    AvoirFournisseurDTO updateStatut(Integer id, AvoirFournisseurStatut statut);
}
