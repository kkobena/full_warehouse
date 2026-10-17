package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitCodeAlias;
import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.ProduitCodeAliasRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.dto.ProductResolveResult;
import com.kobe.warehouse.service.dto.ProductResolveResult.MatchedBy;
import com.kobe.warehouse.service.dto.ProduitCodeAliasDTO;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.ProductCodeResolverService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductCodeResolverServiceImpl implements ProductCodeResolverService {

    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final ProduitRepository produitRepository;
    private final ProduitCodeAliasRepository aliasRepository;
    private final DataMatrixParserService dataMatrixParserService;

    public ProductCodeResolverServiceImpl(
        FournisseurProduitRepository fournisseurProduitRepository,
        ProduitRepository produitRepository,
        ProduitCodeAliasRepository aliasRepository,
        DataMatrixParserService dataMatrixParserService
    ) {
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.produitRepository = produitRepository;
        this.aliasRepository = aliasRepository;
        this.dataMatrixParserService = dataMatrixParserService;
    }

    @Override
    public List<String> listerCodesCandidats(DataMatrixInfo parsed) {
        Set<String> candidats = new LinkedHashSet<>();
        ajouterCandidat(candidats, parsed.cip13());
        ajouterCandidat(candidats, parsed.ean13());
        ajouterCandidat(candidats, parsed.gtin());
        // En dernier : un code hors nomenclature, tel que le code interne d'un grossiste.
        ajouterCandidat(candidats, parsed.rawCode());
        return List.copyOf(candidats);
    }

    @Override
    public List<ProductResolveResult> resoudre(DataMatrixInfo parsed) {
        for (String code : listerCodesCandidats(parsed)) {
            List<ProductResolveResult> trouves = resoudreCode(code);
            if (!trouves.isEmpty()) {
                return trouves;
            }
        }
        return List.of();
    }

    @Override
    public List<ProductResolveResult> resoudreCode(String code) {
        if (!StringUtils.hasText(code)) {
            return List.of();
        }
        List<ProductResolveResult> resultats = new ArrayList<>();
        Set<Integer> produitsVus = new LinkedHashSet<>();

        for (FournisseurProduit fournisseurProduit : fournisseurProduitRepository.findAllByCodeCip(code)) {
            ajouterResultat(resultats, produitsVus, fournisseurProduit.getProduit(), code, MatchedBy.CODE_CIP);
        }
        for (FournisseurProduit fournisseurProduit : fournisseurProduitRepository.findAllByCodeEan(code)) {
            ajouterResultat(resultats, produitsVus, fournisseurProduit.getProduit(), code, MatchedBy.CODE_EAN);
        }
        for (Produit produit : produitRepository.findAllByCodeEanLaboratoire(code)) {
            ajouterResultat(resultats, produitsVus, produit, code, MatchedBy.CODE_EAN_LABO);
        }
        for (ProduitCodeAlias alias : aliasRepository.findAllByCode(code)) {
            ajouterResultat(resultats, produitsVus, alias.getProduit(), code, MatchedBy.ALIAS);
        }
        return resultats;
    }

    @Override
    @Transactional
    public ProduitCodeAliasDTO enregistrerAlias(Integer produitId, String code, CodeAliasType type, CodeAliasSource source) {
        String normalise = code.trim();
        return aliasRepository
            .findFirstByProduitIdAndCode(produitId, normalise)
            .map(ProduitCodeAliasDTO::fromEntity)
            .orElseGet(() -> creerAlias(produitId, normalise, type, source));
    }

    @Override
    @Transactional
    public void supprimerAlias(Integer aliasId) {
        aliasRepository.deleteById(aliasId);
    }

    @Override
    public List<ProduitCodeAliasDTO> listerAlias(Integer produitId) {
        List<ProduitCodeAliasDTO> alias = new ArrayList<>();
        for (ProduitCodeAlias entite : aliasRepository.findAllByProduitIdOrderByCreatedAtDesc(produitId)) {
            alias.add(ProduitCodeAliasDTO.fromEntity(entite));
        }
        return alias;
    }

    @Override
    public CodeAliasType deduireType(String code) {
        return switch (dataMatrixParserService.detectBarcodeType(code)) {
            case GTIN_14 -> CodeAliasType.GTIN_14;
            case EAN_13 -> CodeAliasType.EAN_13;
            case EAN_8 -> CodeAliasType.EAN_8;
            case CIP_13 -> CodeAliasType.CIP_13;
            case CIP_7 -> CodeAliasType.CIP_7;
            // Un DataMatrix complet n'est pas un code produit : seul son contenu en est un,
            // et l'appelant mémorise ce contenu, pas la trame.
            case DATAMATRIX, UNKNOWN -> CodeAliasType.CUSTOM;
        };
    }

    private ProduitCodeAliasDTO creerAlias(Integer produitId, String code, CodeAliasType type, CodeAliasSource source) {
        ProduitCodeAlias alias = new ProduitCodeAlias()
            .setProduit(produitRepository.getReferenceById(produitId))
            .setCode(code)
            .setCodeType(type)
            .setSource(source)
            .setCreatedBy(SecurityUtils.getCurrentUserLogin().orElse(null));
        return ProduitCodeAliasDTO.fromEntity(aliasRepository.saveAndFlush(alias));
    }

    private void ajouterCandidat(Set<String> candidats, String code) {
        if (StringUtils.hasText(code)) {
            candidats.add(code.trim());
        }
    }

    /** Un produit déjà retenu par une source plus sûre n'est pas réinscrit. */
    private void ajouterResultat(
        List<ProductResolveResult> resultats,
        Set<Integer> produitsVus,
        Produit produit,
        String code,
        MatchedBy matchedBy
    ) {
        if (produit == null || !produitsVus.add(produit.getId())) {
            return;
        }
        resultats.add(new ProductResolveResult(produit.getId(), produit.getLibelle(), code, matchedBy));
    }
}
