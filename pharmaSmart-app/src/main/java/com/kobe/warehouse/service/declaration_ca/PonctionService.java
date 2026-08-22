package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.service.declaration_ca.dto.PonctionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParametresDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionSimulationDTO;
import java.time.LocalDate;
import java.util.List;

/** Ponction du chiffre d'affaires à déclarer sur une période. */
public interface PonctionService {
    /**
     * Calcule sans rien écrire. Une simulation n'occupe pas la période : deux peuvent coexister sur
     * les mêmes dates tant qu'aucune n'est validée.
     */
    PonctionSimulationDTO simuler(PonctionParamDTO param);

    /**
     * Applique la ponction.
     *
     * @throws com.kobe.warehouse.service.errors.GenericError si l'objectif dépasse le montant
     *     ponctionnable, ou si la période chevauche une ponction déjà validée
     */
    PonctionDTO valider(PonctionParamDTO param);

    /**
     * Rétablit les montants d'origine et libère la période.
     *
     * <p>Possible tant que le délai configuré n'est pas écoulé — un jour par défaut. Passé ce terme
     * la ponction est définitive : ses chiffres ont eu le temps d'être lus, imprimés ou transmis, et
     * les défaire rendrait un état déjà sorti impossible à reproduire.
     */
    /** Les valeurs d'officine : plafond par defaut et delai d'annulation. */
    PonctionParametresDTO getParametres();

    PonctionDTO annuler(Integer id);

    /**
     * Défait la ponction qui avait touché cette vente, parce qu'on s'apprête à annuler la vente.
     *
     * <p>L'annulation d'une vente prime : elle ne doit jamais être refusée au motif qu'une ponction
     * l'a touchée. C'est donc la ponction qui cède — entièrement, car son montant a été réparti sur
     * l'ensemble de ses ventes et n'est pas décomposable vente par vente sans refaire le calcul.
     *
     * <p>Ni délai ni contrôle de magasin ici : ce n'est pas un geste d'utilisateur, c'est la
     * conséquence mécanique d'un autre.
     */
    void annulerPourVente(Long saleId, LocalDate saleDate, Integer ponctionId);


    List<PonctionDTO> getHistorique();

    /** Le détail d'une ponction : ce qui a été retiré, vente par vente. */
    List<PonctionLigneDTO> getDetail(Integer id);

    /** Justificatif PDF : la cascade d'assiette, le plafond appliqué, puis le détail des ventes. */
    byte[] exportToPdf(Integer id);
}
