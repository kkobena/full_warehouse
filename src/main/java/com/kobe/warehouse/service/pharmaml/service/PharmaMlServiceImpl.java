package com.kobe.warehouse.service.pharmaml.service;

import static java.util.Objects.isNull;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rupture;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.SubstitutionProposee;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import com.kobe.warehouse.domain.enumeration.SubstitutionStatut;
import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.PharmaMlEnvoiRepository;
import com.kobe.warehouse.repository.RuptureRepository;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.repository.SubstitutionProposeeRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.FournisseurService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.DispoGrossisteResultDTO;
import com.kobe.warehouse.service.pharmaml.dto.DispoMultiRequestDTO;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.InfoProduitDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmaMlEnvoiDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;
import com.kobe.warehouse.service.pharmaml.dto.SubstitutionProposeeDTO;
import com.kobe.warehouse.service.settings.FileStorageService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service principal PharmaML qui orchestre les opérations métier. Ce service délègue la
 * construction des payloads à {@link PharmaMlPayloadBuilderService} et les communications HTTP à
 * {@link PharmaMlHttpClientService}.
 */
@Service
@Transactional
public class PharmaMlServiceImpl implements PharmaMlService {

    private static final Logger LOG = LoggerFactory.getLogger(PharmaMlServiceImpl.class);

    private final CommandeRepository commandeRepository;
    private final FournisseurService fournisseurService;
    private final RuptureRepository ruptureRepository;
    private final PharmaMlEnvoiRepository pharmaMlEnvoiRepository;
    private final SubstitutionProposeeRepository substitutionProposeeRepository;
    private final SubstitutRepository substitutRepository;
    private final FournisseurProduitService fournisseurProduitService;
    private final OrderLineService orderLineService;
    private final FileStorageService fileStorageService;
    private final SuggestionLineRepository suggestionLineRepository;

    // Services délégués
    private final PharmaMlPayloadBuilderService payloadBuilderService;
    private final PharmaMlHttpClientService httpClientService;

    public PharmaMlServiceImpl(
        CommandeRepository commandeRepository,
        FournisseurService fournisseurService,
        RuptureRepository ruptureRepository,
        PharmaMlEnvoiRepository pharmaMlEnvoiRepository,
        SubstitutionProposeeRepository substitutionProposeeRepository,
        SubstitutRepository substitutRepository,
        FournisseurProduitService fournisseurProduitService,
        OrderLineService orderLineService,
        FileStorageService fileStorageService,
        SuggestionLineRepository suggestionLineRepository,
        PharmaMlPayloadBuilderService payloadBuilderService,
        PharmaMlHttpClientService httpClientService
    ) {
        this.commandeRepository = commandeRepository;
        this.fournisseurService = fournisseurService;
        this.ruptureRepository = ruptureRepository;
        this.pharmaMlEnvoiRepository = pharmaMlEnvoiRepository;
        this.substitutionProposeeRepository = substitutionProposeeRepository;
        this.substitutRepository = substitutRepository;
        this.fournisseurProduitService = fournisseurProduitService;
        this.orderLineService = orderLineService;
        this.fileStorageService = fileStorageService;
        this.suggestionLineRepository = suggestionLineRepository;
        this.payloadBuilderService = payloadBuilderService;
        this.httpClientService = httpClientService;
    }

    @Override
    public PharmamlCommandeResponse envoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO) {
        Commande commande = commandeRepository.getReferenceById(envoiParamsDTO.getCommandeId());
        if (commande.isHasBeenSubmittedToPharmaML()) {
            throw new GenericError(
                "Cette commande a déjà été soumise via PharmaML",
                "commandeDejasoumisePharmaML"
            );
        }
        PharmaMlEnvoi envoi = null;
        try {
            Fournisseur fournisseur = isNull(envoiParamsDTO.getGrossisteId())
                ? commande.getFournisseur()
                : this.fournisseurService.findOneById(envoiParamsDTO.getGrossisteId());
            prevalidate(fournisseur);

            String refMessage = payloadBuilderService.generateRefMessage();

            envoi = new PharmaMlEnvoi()
                .setCommande(commande)
                .setFournisseur(fournisseur)
                .setRefMessage(refMessage)
                .setStatut(PharmaMlStatut.PENDING)
                .setTentatives(1)
                .setDerniereTentative(LocalDateTime.now());
            pharmaMlEnvoiRepository.save(envoi);

            CsrpEnveloppe payload = payloadBuilderService.buildCommandePayload(commande,
                envoiParamsDTO, fournisseur, refMessage);
            String fileName = httpClientService.generateFileName(commande.getOrderReference(),
                fournisseur.getLibelle());

            PharmamlCommandeResponse result = httpClientService.sendCommandeWithRetry(payload,
                commande, fournisseur, envoi, fileName);

            Path storageLocation = fileStorageService.getFilePharmamlStorageLocation();
            PharmaMlStatut statut = determineStatut(result);

            envoi.setStatut(statut)
                .setXmlRequetePath(storageLocation.resolve("C_" + fileName + ".xml").toString())
                .setXmlReponsePath(storageLocation.resolve("R_" + fileName + ".xml").toString())
                .setTotalLignes(result.totalProduit())
                .setLignesAcceptees(result.successCount())
                .setLignesRupture(result.outOfStockCount());
            pharmaMlEnvoiRepository.save(envoi);

            return result;
        } catch (Exception e) {
            if (envoi != null && PharmaMlStatut.PENDING == envoi.getStatut()) {
                envoi.setStatut(PharmaMlStatut.ERROR);
                pharmaMlEnvoiRepository.save(envoi);
            }
            LOG.error("Erreur lors de l'envoi de la commande à PharmaML", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResponseCommandeDTO lignesCommandeRetour(String commandeRef,
        String orderId) {
        Commande commande = commandeRepository
            .findByOrderReference(commandeRef)
            .orElseThrow(() -> new GenericError("Commande introuvable : " + commandeRef,
                "commandeNotFound"));

        List<VerificationResponseCommandeDTO.Item> items = new ArrayList<>();
        List<VerificationResponseCommandeDTO.Item> extraItems = new ArrayList<>();

        for (OrderLine ol : commande.getOrderLines()) {
            VerificationResponseCommandeDTO.Item item = new VerificationResponseCommandeDTO.Item()
                .setCodeCip(ol.getFournisseurProduit().getCodeCip())
                .setCodeEan(ol.getFournisseurProduit().getProduit().getCodeEanLaboratoire())
                .setProduitLibelle(ol.getFournisseurProduit().getProduit().getLibelle())
                .setQuantite(ol.getQuantityRequested())
                .setQuantitePriseEnCompte(
                    ol.getQuantityReceived() != null ? ol.getQuantityReceived() : 0);

            if (Boolean.TRUE.equals(ol.getUpdated()) && item.getQuantitePriseEnCompte() > 0) {
                items.add(item);
            } else {
                extraItems.add(item);
            }
        }
        return new VerificationResponseCommandeDTO().setItems(items).setExtraItems(extraItems);
    }

    @Override
    public void renvoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO) {
        Commande commande = commandeRepository.getReferenceById(envoiParamsDTO.getCommandeId());
        boolean dernierEnvoiEchoue = pharmaMlEnvoiRepository
            .findTopByCommandeIdAndCommandeOrderDateOrderByCreatedAtDesc(
                commande.getId().getId(), commande.getId().getOrderDate())
            .map(e -> e.getStatut() == PharmaMlStatut.ERROR || e.getStatut() == PharmaMlStatut.REJECTED)
            .orElse(false);
        if (!dernierEnvoiEchoue) {
            throw new GenericError(
                "Le renvoi n'est possible que si le dernier envoi a échoué",
                "renvoiNonAutorise"
            );
        }
        commande.setHasBeenSubmittedToPharmaML(false);
        commandeRepository.save(commande);
        envoiPharmaCommande(envoiParamsDTO);
    }

    @Override
    public List<SubstitutionProposeeDTO> getSubstitutionsEnAttente(Integer commandeId,
        LocalDate orderDate) {
        return substitutionProposeeRepository
            .findByCommandeIdAndCommandeOrderDateAndStatutOrderByCreatedAtDesc(
                commandeId, orderDate, SubstitutionStatut.EN_ATTENTE)
            .stream()
            .map(this::toSubstitutionDTO)
            .toList();
    }

    @Override
    public void accepterSubstitution(Integer substitutionId) {
        SubstitutionProposee sub = substitutionProposeeRepository.findById(substitutionId)
            .orElseThrow(() -> new GenericError("Substitution introuvable : " + substitutionId,
                "substitutionNotFound"));

        List<FournisseurProduit> fps = fournisseurProduitService.findByCodeCipOrProduitcodeEan(
            sub.getCipPropose());
        FournisseurProduit fp = fps.stream()
            .filter(p -> p.getCodeCip().equals(sub.getCipPropose()))
            .findFirst()
            .orElseGet(() -> {
                FournisseurProduit nouveau = new FournisseurProduit();
                nouveau.setCodeCip(sub.getCipPropose());
                nouveau.setFournisseur(sub.getFournisseur());
                if (!fps.isEmpty()) {
                    nouveau.setProduit(fps.getFirst().getProduit());
                }
                return fournisseurProduitService.save(nouveau);
            });

        OrderLine originalLine = sub.getOrderLine();

        OrderLineDTO dto = new OrderLineDTO();
        dto.setQuantityRequested(sub.getQuantite());
        dto.setQuantityReceived(0);
        dto.setTotalQuantity(0);
        OrderLine newLine = orderLineService.buildOrderLine(dto, fp);
        newLine.setCommande(sub.getCommande());
        sub.getCommande().getOrderLines().add(orderLineService.save(newLine));

        sub.setStatut(SubstitutionStatut.ACCEPTEE);
        substitutionProposeeRepository.save(sub);

        // Mémoriser la paire dans le référentiel local des substituts
        enregistrerSubstitutLocal(originalLine.getFournisseurProduit().getProduit(), fp.getProduit());

        // L'original est remplacé : on le supprime maintenant que SubstitutionProposee est résolue
        orderLineService.delete(originalLine);
    }

    @Override
    public void refuserSubstitution(Integer substitutionId) {
        SubstitutionProposee sub = substitutionProposeeRepository.findById(substitutionId)
            .orElseThrow(() -> new GenericError("Substitution introuvable : " + substitutionId,
                "substitutionNotFound"));
        sub.setStatut(SubstitutionStatut.REFUSEE);
        substitutionProposeeRepository.save(sub);

        // Si rien n'a été livré, l'OrderLine original n'a plus lieu d'être
        OrderLine originalLine = sub.getOrderLine();
        if (originalLine.getQuantityReceived() == null || originalLine.getQuantityReceived() == 0) {
            orderLineService.delete(originalLine);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaMlEnvoiDTO getStatutEnvoi(Integer envoiId) {
        return pharmaMlEnvoiRepository
            .findById(envoiId)
            .map(e -> new PharmaMlEnvoiDTO(
                e.getId(),
                e.getStatut(),
                e.getRefMessage(),
                e.getTentatives(),
                e.getDerniereTentative(),
                e.getTotalLignes(),
                e.getLignesAcceptees(),
                e.getLignesRupture(),
                e.getCreatedAt(),
                e.getFournisseur().getLibelle()
            ))
            .orElseThrow(
                () -> new GenericError("Envoi PharmaML introuvable : " + envoiId, "envoiNotFound"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaMlEnvoiDTO> getHistoriqueEnvois(Integer commandeId, LocalDate orderDate) {
        return pharmaMlEnvoiRepository
            .findByCommandeIdAndCommandeOrderDateOrderByCreatedAtDesc(commandeId, orderDate)
            .stream()
            .map(e -> new PharmaMlEnvoiDTO(
                e.getId(),
                e.getStatut(),
                e.getRefMessage(),
                e.getTentatives(),
                e.getDerniereTentative(),
                e.getTotalLignes(),
                e.getLignesAcceptees(),
                e.getLignesRupture(),
                e.getCreatedAt(),
                e.getFournisseur().getLibelle()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResponseCommandeDTO reponseRupture(String ruptureId) {
        Rupture rupture = ruptureRepository
            .findById(Integer.parseInt(ruptureId))
            .orElseThrow(
                () -> new GenericError("Rupture introuvable : " + ruptureId, "ruptureNotFound"));

        VerificationResponseCommandeDTO.Item item = new VerificationResponseCommandeDTO.Item()
            .setCodeCip("")
            .setCodeEan(rupture.getProduit().getCodeEanLaboratoire())
            .setProduitLibelle(rupture.getProduit().getLibelle())
            .setQuantite(rupture.getQty())
            .setQuantitePriseEnCompte(0);

        return new VerificationResponseCommandeDTO()
            .setItems(List.of())
            .setExtraItems(List.of(item));
    }


    @Override
    @Transactional(readOnly = true)
    public List<InfoProduitDTO> demanderDisponibilite(Integer commandeId, LocalDate orderDate,
        Integer grossisteId) {
        Commande commande = commandeRepository.findById(new CommandeId(commandeId, orderDate))
            .orElseThrow(() -> new GenericError("La commande n'existe pas", "commandeNotFound"));
        Fournisseur fournisseur = isNull(grossisteId)
            ? commande.getFournisseur()
            : fournisseurService.findOneById(grossisteId);
        prevalidate(fournisseur);

        String refMessage = payloadBuilderService.generateRefMessage();
        CsrpEnveloppe payload = payloadBuilderService.buildInfoPayload(commande, fournisseur,
            refMessage);

        return httpClientService.sendInfoRequest(payload, fournisseur);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispoGrossisteResultDTO> demanderDisponibiliteMulti(DispoMultiRequestDTO request) {
        List<DispoGrossisteResultDTO> results = new ArrayList<>();

        if (request.suggestionId() != null) {
            // Branche suggestion : charger les lignes de la suggestion
            List<SuggestionLine> lignes = suggestionLineRepository.findAll(
                suggestionLineRepository.filterBySuggestionId(request.suggestionId())
            );
            if (lignes.isEmpty()) {
                return results;
            }
            for (Integer grossisteId : request.grossisteIds()) {
                try {
                    Fournisseur fournisseur = fournisseurService.findOneById(grossisteId);
                    prevalidate(fournisseur);
                    String refMessage = payloadBuilderService.generateRefMessage();
                    CsrpEnveloppe payload = payloadBuilderService.buildInfoPayloadFromSuggestionLines(
                        lignes, fournisseur, refMessage);
                    List<InfoProduitDTO> produits = httpClientService.sendInfoRequest(payload,
                        fournisseur);
                    results.add(new DispoGrossisteResultDTO(grossisteId, fournisseur.getLibelle(),
                        produits));
                } catch (Exception e) {
                    LOG.warn("Echec disponibilité grossiste {} (suggestion {}) : {}", grossisteId,
                        request.suggestionId(), e.getMessage());
                    results.add(new DispoGrossisteResultDTO(grossisteId, null, List.of()));
                }
            }
        } else {
            // Branche commande classique
            LocalDate orderDate = LocalDate.parse(request.orderDate());
            Commande commande = commandeRepository.findById(
                    new CommandeId(request.commandeId(), orderDate))
                .orElseThrow(
                    () -> new GenericError("La commande n'existe pas", "commandeNotFound"));
            for (Integer grossisteId : request.grossisteIds()) {
                try {
                    Fournisseur fournisseur = fournisseurService.findOneById(grossisteId);
                    prevalidate(fournisseur);
                    String refMessage = payloadBuilderService.generateRefMessage();
                    CsrpEnveloppe payload = payloadBuilderService.buildInfoPayload(commande,
                        fournisseur, refMessage);
                    List<InfoProduitDTO> produits = httpClientService.sendInfoRequest(payload,
                        fournisseur);
                    results.add(new DispoGrossisteResultDTO(grossisteId, fournisseur.getLibelle(),
                        produits));
                } catch (Exception e) {
                    LOG.warn("Echec disponibilité grossiste {} : {}", grossisteId, e.getMessage());
                    results.add(new DispoGrossisteResultDTO(grossisteId, null, List.of()));
                }
            }
        }
        return results;
    }

    private PharmaMlStatut determineStatut(PharmamlCommandeResponse result) {
        if (result.success()) {
            if (result.outOfStockCount() == 0) {
                return PharmaMlStatut.SUBMITTED;
            } else if (result.successCount() > 0) {
                return PharmaMlStatut.PARTIAL;
            } else {
                return PharmaMlStatut.REJECTED;
            }
        }
        return PharmaMlStatut.ERROR;
    }

    private SubstitutionProposeeDTO toSubstitutionDTO(SubstitutionProposee s) {
        OrderLine ol = s.getOrderLine();
        FournisseurProduit fpOriginal = ol.getFournisseurProduit();
        String cipOriginal = fpOriginal != null ? fpOriginal.getCodeCip() : "";
        String designOriginal = fpOriginal != null ? fpOriginal.getProduit().getLibelle() : "";
        boolean estConnu = fpOriginal != null && fournisseurProduitService
            .findByCodeCipOrProduitcodeEan(s.getCipPropose())
            .stream()
            .filter(fp -> fp.getCodeCip().equals(s.getCipPropose()))
            .findFirst()
            .map(fp -> substitutRepository.existsByProduitAndSubstitut(fpOriginal.getProduit(), fp.getProduit()))
            .orElse(false);
        return new SubstitutionProposeeDTO(
            s.getId(),
            s.getCipPropose(),
            s.getDesignation(),
            s.getTypeCodification(),
            s.getQuantite(),
            s.getStatut(),
            cipOriginal,
            designOriginal,
            s.getCreatedAt(),
            s.getCodeReponse(),
            s.getAdditif(),
            s.getTypeRemplacement(),
            estConnu
        );
    }

    private void enregistrerSubstitutLocal(Produit produit, Produit substitutProduit) {
        if (substitutRepository.existsByProduitAndSubstitut(produit, substitutProduit)) return;
        Substitut s = new Substitut();
        s.setProduit(produit);
        s.setSubstitut(substitutProduit);
        s.setType(TypeSubstitut.GENERIQUE);
        substitutRepository.save(s);
    }

    private void prevalidate(Fournisseur fournisseur) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        if (isNull(groupeFournisseur)) {
            throw new GenericError(
                String.format("Le fournisseur %s n'est pas configuré pour PharmaML",
                    fournisseur.getLibelle()),
                "fournisseurNonConfigurePharmaMl"
            );
        }
        if (
            !StringUtils.hasLength(groupeFournisseur.getUrlPharmaMl()) ||
                !StringUtils.hasLength(groupeFournisseur.getCodeRecepteurPharmaMl()) ||
                !StringUtils.hasLength(groupeFournisseur.getIdRecepteurPharmaMl())
        ) {
            throw new GenericError(
                String.format(
                    "Veuillez configurer les informations relatives à PharmaML pour %s dans le groupe auquel il appartient",
                    fournisseur.getLibelle()
                ),
                "fournisseurNonConfigurePharmaMl"
            );
        }
    }
}
