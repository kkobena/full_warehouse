package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUserNames;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.AvoirClientUtilisation;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.AvoirClientUtilisationRepository;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.sale.AvoirClientNotificationService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final LotService lotService;
    private final LotStockLocationService lotStockLocationService;
    private final LogsService logsService;
    private final CashRegisterService cashRegisterService;
    private final FinancialTransactionService financialTransactionService;
    private final PaymentModeRepository paymentModeRepository;

    public AvoirClientDocumentServiceImpl(
        AvoirClientRepository avoirClientRepository,
        SalesLineRepository salesLineRepository,
        ReferenceService referenceService,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        AvoirClientNotificationService avoirClientNotificationService,
        AppConfigurationService appConfigurationService,
        AvoirClientUtilisationRepository utilisationRepository,
        LotService lotService,
        LotStockLocationService lotStockLocationService,
        LogsService logsService,
        CashRegisterService cashRegisterService,
        FinancialTransactionService financialTransactionService,
        PaymentModeRepository paymentModeRepository
    ) {
        this.cashRegisterService = cashRegisterService;
        this.financialTransactionService = financialTransactionService;
        this.paymentModeRepository = paymentModeRepository;
        this.logsService = logsService;
        this.lotService = lotService;
        this.lotStockLocationService = lotStockLocationService;
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
        prevenirLesClients(avoirs);
    }

    /**
     * Les produits dus sont arrivés : on prévient chaque client (e-mail, SMS selon la configuration et son consentement).
     * Un avoir n'est lié qu'une fois à une commande, donc prévenu une seule fois. L'envoi attend la validation de la
     * réception : une réception annulée ne doit rien annoncer.
     */
    private void prevenirLesClients(List<AvoirClient> avoirs) {
        Runnable envoi = () -> avoirs.forEach(avoirClientNotificationService::notifierProduitsDisponibles);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoi.run();
                }
            });
        } else {
            envoi.run();
        }
    }

    @Override
    public AvoirClientDocumentDTO cloturerAvoir(Integer avoirId, CloturerAvoirRequest request) {
        AvoirClient avoir = avoirClientRepository.findById(avoirId)
            .orElseThrow(() -> new GenericError("Avoir introuvable : " + avoirId));
        if (avoir.getStatut() != AvoirClientStatut.OUVERT) {
            throw new GenericError("Cet avoir est déjà clôturé");
        }
        Produit produit = avoir.getProduit();
        boolean remiseProduit = produit != null && request.modeCloture() == ModeClotureAvoir.RETOUR_PRODUIT;
        // Remise du produit : on parle d'unités, jamais de montant. Sans quantité, toutes les unités restantes sont remises ;
        // avec une quantité, une remise partielle. Le montant imputé en découle.
        int unitesRemises = 0;
        if (remiseProduit) {
            unitesRemises = resoudreUnitesRemises(avoir, request);
            // La vente a déjà déduit la quantité due : un stock ≥ 0 signifie que toutes les dettes sont couvertes.
            Integer magasinId = storageService.getUser().getMagasin().getId();
            Integer stockTotal = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(magasinId, produit.getId());
            int stock = Objects.requireNonNullElse(stockTotal, 0);
            if (stock < 0) {
                throw new GenericError(
                    "Stock insuffisant pour remettre le produit : il manque " + (-stock)
                    + " unité(s) pour couvrir les avoirs en cours"
                );
            }
        }
        AppUser user = storageService.getUser();
        int montantAUtiliser = remiseProduit ? montantDesUnites(avoir, unitesRemises, request) : resoudreMontantUtilise(avoir, request);
        // Contrôlé avant toute écriture : sans caisse ouverte, rien n'est modifié.
        boolean remboursement = request.modeCloture() == ModeClotureAvoir.REMBOURSEMENT_ESPECES
            || request.modeCloture() == ModeClotureAvoir.REMBOURSEMENT_CB;
        if (remboursement) {
            exigerCaisseOuverte(avoir, user);
        }
        int nouveauMontantUtilise = avoir.getMontantUtilise() + montantAUtiliser;

        avoir.setMontantUtilise(nouveauMontantUtilise);
        avoir.setQuantiteRemise(avoir.getQuantiteRemise() + unitesRemises);
        avoir.setModeCloture(request.modeCloture());
        avoir.setCommentaire(request.commentaire());

        utilisationRepository.save(new AvoirClientUtilisation()
            .setAvoirClient(avoir)
            .setMontantUtilise(montantAUtiliser)
            .setCommentaire(commentaireUtilisation(request, unitesRemises))
            .setUtilisePar(user));

        if (remiseProduit && unitesRemises > 0) {
            // qty_stock a été débité à la vente, les lots non : les unités remises sortent maintenant, à chaque remise.
            lotService.adjustLots(produit, -unitesRemises);
            lotStockLocationService.debitFefo(produit, storageService.getDefaultConnectedUserMainStorage(), unitesRemises);
        }

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
            // Les unités jamais remises (avoir soldé en argent, ou reste d'une remise partielle) reviennent en stock.
            int nonRemises = avoir.getQuantiteRestante();
            if (produit != null && nonRemises > 0) {
                recrediterStock(avoir, produit, nonRemises);
            }
        }

        if (remboursement) {
            enregistrerSortieDeCaisse(avoir, request, montantAUtiliser);
        }

        return toDTO(avoirClientRepository.save(avoir));
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

    private void exigerCaisseOuverte(AvoirClient avoir, AppUser user) {
        if (cashRegisterService.getOpiningCashRegisterByUser(user).isEmpty()) {
            throw new GenericError(
                "Aucune caisse ouverte : ouvrez votre caisse avant de rembourser l'avoir " + avoir.getReference()
            );
        }
    }

    /** Le remboursement, en espèces comme par CB, sort de la caisse : un mouvement SORTIE_CAISSE au mode de paiement choisi. */
    private void enregistrerSortieDeCaisse(AvoirClient avoir, CloturerAvoirRequest request, int montant) {
        ModePaimentCode code = request.modeCloture() == ModeClotureAvoir.REMBOURSEMENT_ESPECES ? ModePaimentCode.CASH : ModePaimentCode.CB;
        PaymentMode mode = paymentModeRepository.findById(code.name())
            .orElseThrow(() -> new GenericError("Mode de paiement introuvable : " + code));
        String commentaire = "Remboursement avoir " + avoir.getReference();
        if (request.commentaire() != null && !request.commentaire().isBlank()) {
            commentaire += " — " + request.commentaire().strip();
        }
        financialTransactionService.create(
            new FinancialTransactionDTO()
                .setAmount(montant)
                .setPaymentMode(mode)
                .setTypeTransaction(TypeFinancialTransaction.SORTIE_CAISSE)
                .setCommentaire(commentaire)
        );
    }

    /** Avoir soldé sans remise du produit : la quantité due, sortie à la vente, revient en stock. */
    private void recrediterStock(AvoirClient avoir, Produit produit, int quantite) {
        Storage storage = storageService.getDefaultConnectedUserMainStorage();
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), storage.getId());
        if (stockProduit == null) {
            return;
        }
        int stockAvant = stockProduit.getQtyStock();
        stockProduit.setQtyStock(stockAvant + quantite);
        stockProduit.setQtyVirtual(stockProduit.getQtyStock());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
        logsService.create(
            TransactionType.AVOIR_SOLDE_SANS_PRODUIT,
            String.format(
                "Avoir %s soldé (%s) : %d unité(s) de %s recréditée(s), stock %d -> %d",
                avoir.getReference(), avoir.getModeCloture(), quantite,
                produit.getLibelle(), stockAvant, stockProduit.getQtyStock()
            ),
            avoir.getId().toString()
        );
    }

    /** Unités remises par cette clôture : toutes celles qui restent, ou la quantité demandée (1 à restantes). */
    private int resoudreUnitesRemises(AvoirClient avoir, CloturerAvoirRequest request) {
        if (request.montantUtilise() != null && request.montantUtilise() > 0) {
            throw new GenericError("La remise du produit se compte en unités : indiquez une quantité, pas un montant");
        }
        int restantes = avoir.getQuantiteRestante();
        Integer demandees = request.quantiteRemise();
        if (demandees == null) {
            return restantes;
        }
        if (demandees <= 0 || demandees > restantes) {
            throw new GenericError("Quantité à remettre invalide : " + demandees + " (il reste " + restantes + " unité(s) à remettre)");
        }
        return demandees;
    }

    /** Montant imputé par une remise : les unités au prix de l'avoir, et tout le solde pour la dernière, sans écart d'arrondi. */
    private int montantDesUnites(AvoirClient avoir, int unites, CloturerAvoirRequest request) {
        if (request.quantiteRemise() == null || unites >= avoir.getQuantiteRestante() || avoir.getQuantite() <= 0) {
            return avoir.getMontantRestant();
        }
        return Math.min(unites * (avoir.getMontant() / avoir.getQuantite()), avoir.getMontantRestant());
    }

    private String commentaireUtilisation(CloturerAvoirRequest request, int unitesRemises) {
        if (unitesRemises <= 0) {
            return request.commentaire();
        }
        String remise = unitesRemises + " unité(s) remise(s)";
        return request.commentaire() == null || request.commentaire().isBlank() ? remise : remise + " — " + request.commentaire();
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
        Long salesLineId = sl != null ? Objects.requireNonNull(sl.getId()).getId() : null;
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
            ? AppUserNames.fullName(ac.getClosedBy())
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
            ac.getMontantRestant(),
            ac.getQuantiteRemise(),
            ac.getQuantiteRestante()
        );
    }
}
