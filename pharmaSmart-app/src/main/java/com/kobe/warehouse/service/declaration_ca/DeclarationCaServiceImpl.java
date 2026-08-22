package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.aop.license.LicenseExempt;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.ExclusionMotif;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Applique les exclusions de chiffre d'affaires à une vente close.
 *
 * <p>Le pipeline est volontairement le seul point du code qui écrive les montants déclarables au
 * moment de la vente : les rapports agrègent ces colonnes, et deux écrivains produiraient des états
 * qui ne se recoupent pas. La ponction (traitement de période) est l'unique autre écrivain, et elle
 * ne touche que des ventes qu'aucune exclusion n'a déjà réduites.
 */
@Service
@Transactional
public class DeclarationCaServiceImpl implements DeclarationCaService {

    /** Ordre de ponction des règlements : les espèces d'abord, puis le reste. */
    private static final Comparator<SalePayment> ESPECES_DABORD = Comparator.comparingInt(payment ->
        ModePaimentCode.CASH.name().equalsIgnoreCase(codeReglement(payment)) ? 0 : 1
    );

    private final LicenseService licenseService;
    private final AppConfigurationService appConfigurationService;
    private final RayonProduitRepository rayonProduitRepository;
    private final StorageService storageService;

    public DeclarationCaServiceImpl(
        LicenseService licenseService,
        AppConfigurationService appConfigurationService,
        RayonProduitRepository rayonProduitRepository,
        StorageService storageService
    ) {
        this.licenseService = licenseService;
        this.appConfigurationService = appConfigurationService;
        this.rayonProduitRepository = rayonProduitRepository;
        this.storageService = storageService;
    }

    @Override
    @LicenseExempt
    public void appliquerExclusions(Sales sales) {
        if (sales == null || CollectionUtils.isEmpty(sales.getSalesLines())) {
            return;
        }
        boolean retraite =
            switch (sales) {
                case CashSale cashSale -> appliquerAuComptant(cashSale);
                case ThirdPartySales thirdPartySales -> appliquerAuTiersPayant(thirdPartySales);
                // Les ventes dépôt portent ca = CA_DEPOT : elles n'entrent pas dans le CA déclaré.
                default -> false;
            };

        if (retraite) {
            repartirSurLesReglements(sales);
        }
    }

    // ===== Ventes comptant : unités gratuites et rayons =====

    private boolean appliquerAuComptant(CashSale cashSale) {
        boolean exclureUg = licenseService.hasFeature(Feature.EXCLUSION_UG) && appConfigurationService.excludeFreeUnit();
        boolean exclureRayon = licenseService.hasFeature(Feature.EXCLUSION_RAYON);
        if (!exclureUg && !exclureRayon) {
            return false;
        }

        Integer storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        boolean retraite = false;

        for (SalesLine ligne : cashSale.getSalesLines()) {
            int montantReel = montantReel(ligne);

            if (exclureRayon && appartientAUnRayonExclu(ligne, storageId)) {
                ligne.setAmountToBeTakenIntoAccount(0);
                ligne.setExclusionMotif(ExclusionMotif.RAYON);
                retraite = true;
                continue;
            }
            if (exclureUg && ligne.getQuantityUg() != null && ligne.getQuantityUg() > 0) {
                int montantUg = ligne.getQuantityUg() * ligne.getRegularUnitPrice();
                ligne.setAmountToBeTakenIntoAccount(Math.max(0, montantReel - montantUg));
                ligne.setExclusionMotif(ExclusionMotif.UG);
                retraite = true;
            }
        }
        return retraite;
    }

    /**
     * Le rayon retenu est celui du dépôt principal de l'utilisateur, exactement celui que l'écran
     * d'exclusion lui a présenté. Lire ailleurs appliquerait une décision prise sur d'autres rayons.
     */
    private boolean appartientAUnRayonExclu(SalesLine ligne, Integer storageId) {
        return rayonProduitRepository
            .findByProduitIdAndStorageId(ligne.getProduit().getId(), storageId)
            .map(rayonProduit -> rayonProduit.getRayon().isExclude())
            .orElse(false);
    }

    // ===== Ventes tiers-payant : exclusion de la vente entière =====

    private boolean appliquerAuTiersPayant(ThirdPartySales vente) {
        if (!licenseService.hasFeature(Feature.EXCLUSION_TP) || !releveDunTiersPayantExclu(vente)) {
            return false;
        }
        vente.getSalesLines().forEach(ligne -> {
            ligne.setAmountToBeTakenIntoAccount(0);
            ligne.setExclusionMotif(ExclusionMotif.TIERS_PAYANT);
        });
        vente.setAmountToBeTakenIntoAccount(0);
        return true;
    }

    /** Une vente peut porter plusieurs tiers-payants : un seul exclu suffit à écarter la vente. */
    private boolean releveDunTiersPayantExclu(ThirdPartySales vente) {
        List<ThirdPartySaleLine> lignes = vente.getThirdPartySaleLines();
        if (lignes == null) {
            return false;
        }
        return lignes
            .stream()
            .map(ThirdPartySaleLine::getClientTiersPayant)
            .filter(Objects::nonNull)
            .map(clientTiersPayant -> clientTiersPayant.getTiersPayant())
            .filter(Objects::nonNull)
            .anyMatch(tiersPayant -> tiersPayant.isBeExclude());
    }

    // ===== Répartition de la réduction sur les règlements =====

    /**
     * Un CA déclaré de 800 face à un encaissement de 1 000 rend l'état indéfendable : l'écart ne se
     * rattache à rien. La réduction est donc reportée sur les règlements, espèces d'abord.
     *
     * <p>Si les règlements ne couvrent pas toute la réduction — vente différée, dont les règlements
     * arrivent plus tard — le reliquat n'est pas imputé : il n'y a rien à réduire. L'encaissement
     * reste inférieur au chiffre d'affaires, ce qui est le propre d'une vente à crédit.
     */
    private void repartirSurLesReglements(Sales sales) {
        int montantReel = sales.getSalesLines().stream().mapToInt(DeclarationCaServiceImpl::montantReel).sum();
        int montantDeclare = sales
            .getSalesLines()
            .stream()
            .mapToInt(ligne -> Objects.requireNonNullElse(ligne.getAmountToBeTakenIntoAccount(), 0))
            .sum();
        sales.setAmountToBeTakenIntoAccount(montantDeclare);

        int reduction = montantReel - montantDeclare;
        if (reduction <= 0 || sales.getPayments() == null || sales.getPayments().isEmpty()) {
            return;
        }

        List<SalePayment> reglements = sales.getPayments().stream().sorted(ESPECES_DABORD).toList();
        int reste = reduction;
        for (SalePayment reglement : reglements) {
            int encaisse = Objects.requireNonNullElse(reglement.getPaidAmount(), 0);
            int prise = Math.min(reste, encaisse);
            reglement.setAmountToBeTakenIntoAccount(encaisse - prise);
            reste -= prise;
            if (reste == 0) {
                break;
            }
        }
    }

    private static int montantReel(SalesLine ligne) {
        return ligne.getQuantityRequested() * ligne.getRegularUnitPrice();
    }

    private static String codeReglement(PaymentTransaction payment) {
        return payment.getPaymentMode() == null ? null : payment.getPaymentMode().getCode();
    }
}
