package com.kobe.warehouse.service.dci.service;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.repository.DciRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.dci.dto.DciDTO;
import com.kobe.warehouse.service.dci.dto.DciProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class DciServiceImpl implements DciService {

    /**
     * Longueur maximale de la colonne dci.code.
     */
    private static final int CODE_MAX_LENGTH = 20;

    /**
     * Marge réservée au suffixe de désambiguïsation lorsqu'un code dérivé est déjà pris.
     */
    private static final int CODE_BASE_LENGTH = 16;

    private static final Logger LOG = LoggerFactory.getLogger(DciServiceImpl.class);

    private final DciRepository dciRepository;
    private final ProduitRepository produitRepository;

    public DciServiceImpl(DciRepository dciRepository, ProduitRepository produitRepository) {
        this.dciRepository = dciRepository;
        this.produitRepository = produitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DciDTO> findAll(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return this.dciRepository.findAllByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCaseOrderByLibelleAsc(
                search,
                search,
                pageable
            ).map(DciDTO::new);
        }
        return this.dciRepository.findAllByOrderByLibelleAsc(pageable).map(DciDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DciDTO> findOne(Integer id) {
        return this.dciRepository.findById(id).map(DciDTO::new);
    }

    @Override
    public DciDTO create(DciDTO dto) {
        Dci dci = new Dci();
        dci.setLibelle(exigerLibelle(dto));
        verifierUnicite(dci.getLibelle(), dto.getCode(), null);
        dci.setCode(resolveCode(dto.getCode(), dci.getLibelle(), null));
        return new DciDTO(this.dciRepository.save(dci));
    }

    @Override
    public DciDTO update(DciDTO dto) {
        int id = (int) dto.getId();
        Dci dci = this.dciRepository.findById(id).orElseThrow(() ->
            new GenericError("Cette DCI n'existe plus.", "dciIntrouvable")
        );
        String libelle = exigerLibelle(dto);
        verifierUnicite(libelle, dto.getCode(), id);
        dci.setLibelle(libelle);
        // À la modification, un code vidé est régénéré depuis le nouveau libellé.
        dci.setCode(resolveCode(dto.getCode(), libelle, id));
        return new DciDTO(this.dciRepository.save(dci));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DciProduitDTO> findProduits(Integer dciId) {
        return this.produitRepository.findAllByDciIdForDetail(dciId)
            .stream()
            .map(p ->
                new DciProduitDTO(
                    p.getId(),
                    p.getLibelle(),
                    // Le CIP vit sur fournisseur_produit, pas sur produit : il est donc
                    // absent tant qu'aucun fournisseur principal n'est désigné.
                    p.getFournisseurProduitPrincipal() != null
                        ? p.getFournisseurProduitPrincipal().getCodeCip()
                        : null,
                    p.getFamille() != null ? p.getFamille().getLibelle() : null,
                    p.getRegularUnitPrice(),
                    p.getStatus() != null ? p.getStatus().name() : null
                )
            )
            .toList();
    }

    @Override
    public void delete(Integer id) {

        long rattaches = this.produitRepository.countByDciId(id);
        if (rattaches > 0) {
            throw new GenericError(
                "Suppression impossible : %d produit(s) sont rattachés à cette DCI.".formatted(
                    rattaches),
                "dciRattacheeAProduits"
            );
        }
        this.dciRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger importes = new AtomicInteger(0);
        AtomicInteger rejetes = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(0);

        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(br);

            for (CSVRecord record : records) {
                total.incrementAndGet();
                try {
                    // Colonne 0 : code (facultatif). Colonne 1 : libellé (obligatoire).
                    String code = record.size() > 0 ? record.get(0) : null;
                    String libelle = record.size() > 1 ? record.get(1) : null;

                    if (!StringUtils.hasText(libelle)) {
                        rejetes.incrementAndGet();
                        continue;
                    }

                    // Le libellé est unique en base : un doublon n'est pas une erreur
                    // d'import, c'est une ligne déjà connue.
                    if (this.dciRepository.existsByLibelleIgnoreCase(libelle)) {
                        rejetes.incrementAndGet();
                        continue;
                    }

                    if (StringUtils.hasText(code)
                        && this.dciRepository.existsByCodeIgnoreCase(code.trim().toUpperCase())) {
                        LOG.warn("Ligne {} rejetée : le code « {} » est déjà utilisé.", total.get(),
                            code);
                        rejetes.incrementAndGet();
                        continue;
                    }

                    Dci dci = new Dci();
                    dci.setLibelle(libelle);
                    dci.setCode(resolveCode(code, libelle, null));
                   
                    this.dciRepository.saveAndFlush(dci);
                    importes.incrementAndGet();
                } catch (Exception e) {
                    LOG.warn("Ligne {} rejetée à l'import DCI : {}", total.get(), e.getMessage());
                    rejetes.incrementAndGet();
                }
            }
        } catch (IOException e) {
            LOG.error("Lecture du fichier d'import DCI impossible", e);
            return new ResponseDTO().success(false).message("Fichier illisible.");
        }

        return new ResponseDTO()
            .success(true)
            .setCompleted(true)
            .size(importes.get())
            .totalSize(total.get())
            .setErrorSize(rejetes.get())
            .message(
                "%d ligne(s) importée(s) sur %d, %d ignorée(s).".formatted(importes.get(),
                    total.get(), rejetes.get())
            );
    }

    /**
     * Libellé obligatoire — la contrainte est aussi portée par la base, autant la dire ici.
     */
    private String exigerLibelle(DciDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getLibelle())) {
            throw new GenericError("Le libellé est obligatoire.", "dciLibelleObligatoire");
        }
        return dto.getLibelle().trim();
    }

    /**
     * Refuse un libellé ou un code déjà pris.
     *
     * <p>Les deux colonnes sont uniques en base : sans ce contrôle, l'utilisateur recevrait une
     * violation de contrainte, qui ne dit ni quel champ est en cause ni par quoi il est occupé.
     *
     * @param idAExclure identifiant de la ligne en cours de modification, {@code null} en création
     */
    private void verifierUnicite(String libelle, String codeFourni, Integer idAExclure) {
        boolean libellePris = idAExclure == null
            ? this.dciRepository.existsByLibelleIgnoreCase(libelle)
            : this.dciRepository.existsByLibelleIgnoreCaseAndIdNot(libelle, idAExclure);
        if (libellePris) {
            throw new GenericError(
                "Une DCI porte déjà le libellé « %s ».".formatted(libelle),
                "dciLibelleDuplique"
            );
        }

        // Un code absent sera dérivé, et la dérivation garantit déjà son unicité :
        // il n'y a rien à contrôler dans ce cas.
        if (!StringUtils.hasText(codeFourni)) {
            return;
        }
        String code = codeFourni.trim().toUpperCase();
        boolean codePris = idAExclure == null
            ? this.dciRepository.existsByCodeIgnoreCase(code)
            : this.dciRepository.existsByCodeIgnoreCaseAndIdNot(code, idAExclure);
        if (codePris) {
            throw new GenericError(
                "Le code « %s » est déjà utilisé par une autre DCI.".formatted(code),
                "dciCodeDuplique"
            );
        }
    }

    /**
     * Retient le code fourni, ou en dérive un du libellé lorsqu'il est absent.
     */
    private String resolveCode(String codeFourni, String libelle, Integer idAExclure) {
        if (StringUtils.hasText(codeFourni)) {
            String code = codeFourni.trim().toUpperCase();
            return code.length() > CODE_MAX_LENGTH ? code.substring(0, CODE_MAX_LENGTH) : code;
        }
        return genererCode(libelle, idAExclure);
    }

    /**
     * Dérive un code du libellé : accents retirés, caractères non alphanumériques supprimés, mise
     * en majuscules et troncature.
     *
     * <p>La colonne étant unique, un suffixe numérique est ajouté tant que le code obtenu est déjà
     * pris. Sans lui, deux libellés proches — « ACIDE FOLIQUE » et « ACIDE FOLINIQUE » — se
     * réduiraient au même code et le second import échouerait sur la contrainte.
     */
    private String genererCode(String libelle, Integer idAExclure) {
        String base = Normalizer.normalize(libelle, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase();

        if (base.isEmpty()) {
            base = "DCI";
        }
        if (base.length() > CODE_BASE_LENGTH) {
            base = base.substring(0, CODE_BASE_LENGTH);
        }

        if (!codeExiste(base, idAExclure)) {
            return base;
        }
        for (int suffixe = 2; suffixe < 10000; suffixe++) {
            String candidat = base + suffixe;
            if (candidat.length() > CODE_MAX_LENGTH) {
                candidat =
                    base.substring(0, CODE_MAX_LENGTH - String.valueOf(suffixe).length()) + suffixe;
            }
            if (!codeExiste(candidat, idAExclure)) {
                return candidat;
            }
        }
        throw new GenericError(
            "Impossible de dériver un code unique pour « %s » : renseignez-le manuellement.".formatted(
                libelle),
            "dciCodeIndicible"
        );
    }

    /**
     * Le code est-il déjà pris, en ignorant la ligne en cours de modification ?
     *
     * <p>Sans cette exclusion, régénérer le code d'une DCI existante le trouverait pris par
     * elle-même et lui accolerait un suffixe à chaque enregistrement.
     */
    private boolean codeExiste(String code, Integer idAExclure) {
        return idAExclure == null
            ? this.dciRepository.existsByCodeIgnoreCase(code)
            : this.dciRepository.existsByCodeIgnoreCaseAndIdNot(code, idAExclure);
    }
}
