package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.RelanceDiffere;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.RelanceDiffereRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.SmsService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Relance SMS d'un client pour ses ventes différées (docs/PLAN-FICHE-CLIENT.md, lot 3). Chaque
 * relance est enregistrée : la fiche montre la dernière, ce qui évite de harceler un client.
 *
 * <p>L'envoi passe par {@link SmsService}, dont le fournisseur reste à brancher : tant qu'il ne
 * l'est pas, le message est journalisé côté serveur, sans être transmis.
 */
@Service
@Transactional
public class RelanceDiffereService {

    private final CustomerRepository customerRepository;
    private final SalesRepository salesRepository;
    private final RelanceDiffereRepository relanceRepository;
    private final SmsService smsService;
    private final AppConfigurationService appConfigurationService;
    private final DerogationAuthorizer derogationAuthorizer;

    public RelanceDiffereService(
        CustomerRepository customerRepository,
        SalesRepository salesRepository,
        RelanceDiffereRepository relanceRepository,
        SmsService smsService,
        AppConfigurationService appConfigurationService,
        DerogationAuthorizer derogationAuthorizer
    ) {
        this.customerRepository = customerRepository;
        this.salesRepository = salesRepository;
        this.relanceRepository = relanceRepository;
        this.smsService = smsService;
        this.appConfigurationService = appConfigurationService;
        this.derogationAuthorizer = derogationAuthorizer;
    }

    public RelanceDiffereDTO relancer(Integer customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new GenericError("Client introuvable", "customerNotFound"));
        if (!StringUtils.hasText(customer.getPhone())) {
            throw new GenericError("Le client n'a pas de numéro de téléphone : relance impossible.", "relanceSansTelephone");
        }
        BigDecimal solde = salesRepository.getDiffereSoldeByCustomerId(customerId);
        int montant = solde == null ? 0 : solde.intValue();
        if (montant <= 0) {
            throw new GenericError("Le client n'a aucune vente différée à régler.", "relanceSansEncours");
        }
        AppUser utilisateur = derogationAuthorizer.utilisateurCourant();
        String message =
            "Bonjour %s, %s vous rappelle un solde de %s %s à régler. Merci de votre visite.".formatted(
                    customer.getFirstName(),
                    utilisateur.getMagasin().getName(),
                    montantPourSms(montant),
                    appConfigurationService.getDevise()
                );
        smsService.sendSms(customer.getPhone(), message);
        RelanceDiffere relance = relanceRepository.save(
            new RelanceDiffere().setCustomerId(customerId).setTelephone(customer.getPhone()).setMontant(montant).setMessage(message).setUser(utilisateur)
        );
        return toDto(relance);
    }

    /** Espace ordinaire comme séparateur de milliers : l'espace fine du format français sort de l'alphabet SMS. */
    private static String montantPourSms(int montant) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(montant).replace(' ', ' ').replace(' ', ' ');
    }

    @Transactional(readOnly = true)
    public List<RelanceDiffereDTO> historique(Integer customerId) {
        return relanceRepository.findTop10ByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(RelanceDiffereService::toDto).toList();
    }

    private static RelanceDiffereDTO toDto(RelanceDiffere r) {
        return new RelanceDiffereDTO(
            r.getCreatedAt(),
            r.getTelephone(),
            r.getMontant(),
            r.getMessage(),
            r.getUser().getFirstName() + " " + r.getUser().getLastName()
        );
    }
}
