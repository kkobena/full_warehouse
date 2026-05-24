package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.AvoirClientUtilisation;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.AvoirClientUtilisationRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.sale.AvoirClientNotificationService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvoirClientDocumentServiceImpl implements AvoirClientDocumentService {

    private final AvoirClientRepository avoirClientRepository;
    private final SalesLineRepository salesLineRepository;
    private final ReferenceService referenceService;
    private final StorageService storageService;
    private final StockProduitRepository stockProduitRepository;
    private final AvoirClientNotificationService avoirClientNotificationService;
    private final AppConfigurationService appConfigurationService;
    private final AvoirClientUtilisationRepository utilisationRepository;

    public AvoirClientDocumentServiceImpl(
        AvoirClientRepository avoirClientRepository,
        SalesLineRepository salesLineRepository,
        ReferenceService referenceService,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        AvoirClientNotificationService avoirClientNotificationService,
        AppConfigurationService appConfigurationService,
        AvoirClientUtilisationRepository utilisationRepository
    ) {
        this.avoirClientRepository = avoirClientRepository;
        this.salesLineRepository = salesLineRepository;
        this.referenceService = referenceService;
        this.storageService = storageService;
        this.stockProduitRepository = stockProduitRepository;
        this.avoirClientNotificationService = avoirClientNotificationService;
        this.appConfigurationService = appConfigurationService;
        this.utilisationRepository = utilisationRepository;
    }


    @Override
    public void createAvoirsFromSale(SalesLine salesLine, Customer customer) {
        if (Objects.isNull(customer) && salesLine.getQuantityAvoir() <= 0) {
            return;
        }
        avoirClientRepository.save(buildAvoirClientFromSale(salesLine, customer));
    }

    @Override
    public void cancelAvoirsFromSale(Long salesLineId) {
        avoirClientRepository.findBySalesLineId(salesLineId).ifPresent(ac -> {
            ac.setStatut(AvoirClientStatut.ANNULE);
            avoirClientRepository.save(ac);
        });
    }

    @Override
    public void linkCommandeToAvoirs(Commande commande) {
        if (!avoirClientRepository.existsByStatutAndCommandeIsNull(AvoirClientStatut.OUVERT)) {
            return;
        }
        Set<Integer> produitIds = commande.getOrderLines().stream()
            .map(ol -> ol.getFournisseurProduit().getProduit().getId())
            .collect(Collectors.toSet());
        if (produitIds.isEmpty()) return;

        List<AvoirClient> avoirs = avoirClientRepository.findAll(AvoirClientRepository.forCommande(produitIds));
        if (avoirs.isEmpty()) return;

        avoirs.forEach(a -> a.setCommande(commande));
        avoirClientRepository.saveAll(avoirs);
    }

    @Override
    public AvoirClientDocumentDTO cloturerAvoir(Integer avoirId, CloturerAvoirRequest request) {
        AvoirClient avoir = avoirClientRepository.findById(avoirId)
            .orElseThrow(() -> new GenericError("Avoir introuvable : " + avoirId));
        if (avoir.getStatut() != AvoirClientStatut.OUVERT) {
            throw new GenericError("Cet avoir est déjà clôturé");
        }
        Produit produit = avoir.getProduit();
        if (produit != null) {
            Integer magasinId = storageService.getUser().getMagasin().getId();
            Integer stockTotal = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(magasinId, produit.getId());
            int stock = Objects.requireNonNullElse(stockTotal, 0);
            if (stock < avoir.getQuantite()) {
                throw new GenericError(
                    "Stock insuffisant pour clôturer l'avoir : stock disponible = " + stock
                    + ", quantité avoir = " + avoir.getQuantite()
                );
            }
        }
        AppUser user = storageService.getUser();
        int montantAUtiliser = resoudreMontantUtilise(avoir, request);
        int nouveauMontantUtilise = avoir.getMontantUtilise() + montantAUtiliser;

        avoir.setMontantUtilise(nouveauMontantUtilise);
        avoir.setModeCloture(request.modeCloture());
        avoir.setCommentaire(request.commentaire());

        utilisationRepository.save(new AvoirClientUtilisation()
            .setAvoirClient(avoir)
            .setMontantUtilise(montantAUtiliser)
            .setCommentaire(request.commentaire())
            .setUtilisePar(user));

        boolean soldeEpuise = nouveauMontantUtilise >= avoir.getMontant();
        if (soldeEpuise) {
            avoir.setStatut(AvoirClientStatut.CLOTURE);
            avoir.setClotureLe(LocalDateTime.now());
            avoir.setClosedBy(user);
            SalesLine sl = avoir.getSalesLine();
            if (sl != null) {
                sl.setQuantityAvoir(0);
                salesLineRepository.save(sl);
            }
        }

        AvoirClient saved = avoirClientRepository.save(avoir);
        if (request.modeCloture() == ModeClotureAvoir.RETOUR_PRODUIT && soldeEpuise) {
            avoirClientNotificationService.notifierProduitsDisponibles(saved);
        }
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvoirClientDocumentDTO> findAllByCustomer(Integer customerId) {
        return avoirClientRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvoirClientDocumentDTO> findAll(
        String search, LocalDate fromDate, LocalDate toDate,
        AvoirClientStatut statut, Pageable pageable
    ) {
        return avoirClientRepository.findAll(
            AvoirClientRepository.buildSpec(search, fromDate, toDate, statut), pageable
        ).map(this::toDTO);
    }

    private int resoudreMontantUtilise(AvoirClient avoir, CloturerAvoirRequest request) {
        int montantRestant = avoir.getMontantRestant();
        if (request.montantUtilise() == null || request.montantUtilise() <= 0) {
            return montantRestant;
        }
        if (request.montantUtilise() > montantRestant) {
            throw new GenericError(
                "Montant à utiliser (" + request.montantUtilise()
                + ") supérieur au montant restant de l'avoir (" + montantRestant + ")");
        }
        return request.montantUtilise();
    }

    private AvoirClient buildAvoirClientFromSale(SalesLine salesLine, Customer customer) {
        LocalDate expiration = LocalDate.now().plusDays(appConfigurationService.getDelaiValiditeAvoir());
        return new AvoirClient()
            .setReference(referenceService.buildNumAvoirClient())
            .setSalesLine(salesLine)
            .setCommande(null)
            .setProduit(salesLine.getProduit())
            .setCustomer(customer)
            .setQuantite(salesLine.getQuantityAvoir())
            .setMontant(salesLine.getQuantityAvoir() * salesLine.getRegularUnitPrice())
            .setDateExpiration(expiration)
            .setCreatedBy(storageService.getUser());
    }

    private AvoirClientDocumentDTO toDTO(AvoirClient ac) {
        SalesLine sl = ac.getSalesLine();
        String numberTransaction = sl != null ? sl.getSales().getNumberTransaction() : null;
        Long salesLineId = sl != null ? sl.getId().getId() : null;
        LocalDate salesLineDate = sl != null ? sl.getSaleDate() : null;

        Produit produit = ac.getProduit();
        FournisseurProduit fp = produit != null ? produit.getFournisseurProduitPrincipal() : null;
        String codeCip = fp != null ? fp.getCodeCip()
            : (produit != null ? produit.getCodeEanLaboratoire() : null);

        Customer customer = ac.getCustomer();
        String customerName = customer != null
            ? (customer.getFirstName() + " " + Objects.requireNonNullElse(customer.getLastName(), "")).strip()
            : null;

        String closedByName = ac.getClosedBy() != null
            ? ac.getClosedBy().getFirstName() + " " + ac.getClosedBy().getLastName()
            : null;

        String commandeRef = ac.getCommande() != null ? ac.getCommande().getReceiptReference() : null;

        LocalDate dateExpiration = ac.getDateExpiration();
        boolean procheExpiration = dateExpiration != null
            && ac.getStatut() == AvoirClientStatut.OUVERT
            && !dateExpiration.isBefore(LocalDate.now())
            && dateExpiration.isBefore(LocalDate.now().plusDays(7));

        return new AvoirClientDocumentDTO(
            ac.getId(),
            ac.getReference(),
            ac.getCreatedAt(),
            ac.getClotureLe(),
            ac.getStatut(),
            ac.getModeCloture(),
            ac.getQuantite(),
            ac.getMontant(),
            ac.getCommentaire(),
            customerName,
            produit != null ? produit.getLibelle() : null,
            codeCip,
            salesLineId,
            salesLineDate,
            numberTransaction,
            commandeRef,
            closedByName,
            dateExpiration,
            procheExpiration,
            ac.getMontantUtilise(),
            ac.getMontantRestant()
        );
    }
}
