package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.ReponseRetourBon;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.errors.GenericError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@Transactional
public class AvoirFournisseurServiceImpl implements AvoirFournisseurService {

    private final AvoirFournisseurRepository avoirFournisseurRepository;
    private final UserService userService;

    public AvoirFournisseurServiceImpl(AvoirFournisseurRepository avoirFournisseurRepository,
                                       UserService userService) {
        this.avoirFournisseurRepository = avoirFournisseurRepository;
        this.userService = userService;
    }

    @Override
    public AvoirFournisseurDTO createFromReponseRetourBon(ReponseRetourBon reponseRetourBon) {
        long montant = reponseRetourBon.getReponseRetourBonItems().stream()
            .mapToLong(item -> (long) (item.getPrixAchat() != null ? item.getPrixAchat() : 0)
                               * (item.getQtyMvt() != null ? item.getQtyMvt() : 0))
            .sum();

        Fournisseur fournisseur = resolveFournisseur(reponseRetourBon);

        AvoirFournisseur avoir = new AvoirFournisseur();
        avoir.setDateMtv(LocalDateTime.now());
        avoir.setMontant(montant);
        avoir.setStatut(AvoirFournisseurStatut.EN_ATTENTE);
        avoir.setUser(userService.getUser());
        avoir.setReponseRetourBon(reponseRetourBon);
        avoir.setFournisseur(fournisseur);
        avoir = avoirFournisseurRepository.save(avoir);

        String reference = "AV-" + Year.now().getValue() + "-" + String.format("%04d", avoir.getId());
        avoir.setReference(reference);
        avoir = avoirFournisseurRepository.save(avoir);

        return new AvoirFournisseurDTO(avoir);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvoirFournisseurDTO> findAll(AvoirFournisseurStatut statut, Integer fournisseurId,
                                              LocalDate dtStart, LocalDate dtEnd, Pageable pageable) {
        LocalDateTime start = dtStart != null ? dtStart.atStartOfDay() : null;
        LocalDateTime end = dtEnd != null ? dtEnd.atTime(23, 59, 59) : null;
        return avoirFournisseurRepository.findAll(statut, fournisseurId, start, end, pageable)
            .map(AvoirFournisseurDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvoirEncoursFournisseurDTO> getEncoursParFournisseur() {
        return avoirFournisseurRepository.sumEncoursParFournisseur().stream()
            .map(row -> new AvoirEncoursFournisseurDTO(
                (Integer) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .toList();
    }

    @Override
    public AvoirFournisseurDTO updateStatut(Integer id, AvoirFournisseurStatut statut) {
        AvoirFournisseur avoir = avoirFournisseurRepository.findById(id)
            .orElseThrow(() -> new GenericError("Avoir fournisseur non trouvé"));
        if (avoir.getStatut() != AvoirFournisseurStatut.EN_ATTENTE) {
            throw new GenericError("Seuls les avoirs en attente peuvent être mis à jour");
        }
        avoir.setStatut(statut);
        return new AvoirFournisseurDTO(avoirFournisseurRepository.save(avoir));
    }

    private Fournisseur resolveFournisseur(ReponseRetourBon reponseRetourBon) {
        var retourBon = reponseRetourBon.getRetourBon();
        if (retourBon.getCommande() != null && retourBon.getCommande().getFournisseur() != null) {
            return retourBon.getCommande().getFournisseur();
        }
        if (retourBon.getFournisseur() != null) {
            return retourBon.getFournisseur();
        }
        throw new GenericError("Aucun fournisseur associé au retour");
    }
}
