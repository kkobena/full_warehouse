package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.AvoirFournisseurLine;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.errors.GenericError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AvoirFournisseurServiceImpl implements AvoirFournisseurService {

    private final AvoirFournisseurRepository avoirFournisseurRepository;
    private final RetourBonRepository retourBonRepository;
    private final RetourBonItemRepository retourBonItemRepository;
    private final UserService userService;

    public AvoirFournisseurServiceImpl(
        AvoirFournisseurRepository avoirFournisseurRepository,
        RetourBonRepository retourBonRepository,
        RetourBonItemRepository retourBonItemRepository,
        UserService userService
    ) {
        this.avoirFournisseurRepository = avoirFournisseurRepository;
        this.retourBonRepository = retourBonRepository;
        this.retourBonItemRepository = retourBonItemRepository;
        this.userService = userService;
    }

    @Override
    public AvoirFournisseurDTO create(AvoirFournisseurCommand command) {
        RetourBon retourBon = retourBonRepository.findById(command.retourBonId())
            .orElseThrow(() -> new GenericError("RetourBon non trouvé: " + command.retourBonId()));

        if (retourBon.getStatut() != RetourStatut.VALIDATED && retourBon.getStatut() != RetourStatut.PROCESSING) {
            throw new GenericError("Ce retour est déjà traité");
        }

        return createFromRetourBon(retourBon, command.lignes(), command.commentaire());
    }

    @Override
    public AvoirFournisseurDTO createFromRetourBon(RetourBon retourBon, List<AvoirFournisseurCommand.AvoirLigneCommand> lignes, String commentaire) {
        Fournisseur fournisseur = resolveFournisseur(retourBon);

        AvoirFournisseur avoir = new AvoirFournisseur();
        avoir.setDateMtv(LocalDateTime.now());
        avoir.setStatut(AvoirFournisseurStatut.EN_ATTENTE);
        avoir.setUser(userService.getUser());
        avoir.setRetourBon(retourBon);
        avoir.setFournisseur(fournisseur);
        avoir.setCommentaire(commentaire);

        boolean allAccepted = true;
        if (lignes != null && !lignes.isEmpty()) {
            for (AvoirFournisseurCommand.AvoirLigneCommand ligneCmd : lignes) {
                boolean accepted = buildLine(avoir, ligneCmd);
                if (!accepted) {
                    allAccepted = false;
                }
            }
        }

        long montant = avoir.getLignes().stream()
            .mapToLong(l -> l.getPrixAchat() * l.getQtyMvt())
            .sum();
        avoir.setMontant(montant);

        avoir = avoirFournisseurRepository.save(avoir);

        String reference = "AV-" + Year.now().getValue() + "-" + String.format("%04d", avoir.getId());
        avoir.setReference(reference);
        avoir = avoirFournisseurRepository.save(avoir);

        retourBon.setStatut(allAccepted ? RetourStatut.CLOSED : RetourStatut.PARTIALLY_ACCEPTED);
        retourBonRepository.save(retourBon);

        return new AvoirFournisseurDTO(avoir);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvoirFournisseurDTO> findAll(AvoirFournisseurStatut statut, Integer fournisseurId,
                                              LocalDate dtStart, LocalDate dtEnd, Pageable pageable) {
        LocalDateTime start = dtStart != null ? dtStart.atStartOfDay() : null;
        LocalDateTime end = dtEnd != null ? dtEnd.atTime(LocalTime.MAX) : null;
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
        if (avoir.getStatut() == AvoirFournisseurStatut.ANNULE) {
            throw new GenericError("Impossible de modifier un avoir annulé");
        }
        avoir.setStatut(statut);
        return new AvoirFournisseurDTO(avoirFournisseurRepository.save(avoir));
    }

    @Override
    public AvoirFournisseurDTO annuler(Integer id, String motif) {
        AvoirFournisseur avoir = avoirFournisseurRepository.findById(id)
            .orElseThrow(() -> new GenericError("Avoir fournisseur non trouvé"));
        if (avoir.getStatut() == AvoirFournisseurStatut.IMPUTE) {
            throw new GenericError("Impossible d'annuler un avoir déjà imputé");
        }
        if (avoir.getStatut() == AvoirFournisseurStatut.ANNULE) {
            throw new GenericError("L'avoir est déjà annulé");
        }
        avoir.setStatut(AvoirFournisseurStatut.ANNULE);
        if (motif != null && !motif.isBlank()) {
            avoir.setCommentaire(motif);
        }
        return new AvoirFournisseurDTO(avoirFournisseurRepository.save(avoir));
    }

    private boolean buildLine(AvoirFournisseur avoir, AvoirFournisseurCommand.AvoirLigneCommand cmd) {
        RetourBonItem retourBonItem = retourBonItemRepository.findById(cmd.retourBonItemId())
            .orElseThrow(() -> new GenericError("RetourBonItem non trouvé: " + cmd.retourBonItemId()));

        long prix = Objects.requireNonNullElse(cmd.prixAchat(), retourBonItem.getPrixAchat()).longValue();

        AvoirFournisseurLine line = new AvoirFournisseurLine();
        line.setAvoirFournisseur(avoir);
        line.setRetourBonItem(retourBonItem);
        line.setQtyMvt(cmd.qtyAcceptee());
        line.setPrixAchat(prix);
        avoir.getLignes().add(line);

        retourBonItem.setAcceptedQty(cmd.qtyAcceptee());
        retourBonItemRepository.save(retourBonItem);

        return Objects.equals(cmd.qtyAcceptee(), retourBonItem.getQtyMvt());
    }

    private Fournisseur resolveFournisseur(RetourBon retourBon) {
        if (retourBon.getCommande() != null && retourBon.getCommande().getFournisseur() != null) {
            return retourBon.getCommande().getFournisseur();
        }
        if (retourBon.getFournisseur() != null) {
            return retourBon.getFournisseur();
        }
        throw new GenericError("Aucun fournisseur associé au retour");
    }
}
