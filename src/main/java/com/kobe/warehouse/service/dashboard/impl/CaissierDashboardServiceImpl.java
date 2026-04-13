package com.kobe.warehouse.service.dashboard.impl;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CashRegisterItemRepository;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dto.dashboard.CaissierDashboardDTO;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.dashboard.DiffereARelancerDTO;
import com.kobe.warehouse.service.dto.dashboard.EncaissementParModeDTO;
import com.kobe.warehouse.service.dto.dashboard.LivraisonAttendueDTO;
import com.kobe.warehouse.service.dto.dashboard.ResumeDifferesDTO;
import com.kobe.warehouse.service.dto.dashboard.SessionEncaissementsDTO;
import com.kobe.warehouse.service.dto.dashboard.VenteRecenteDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CaissierDashboardServiceImpl implements CaissierDashboardService {

    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("HH:mm");


    private static final Set<String> VENTE_CASH_TRANSACTIONS = Set.of(
        TypeFinancialTransaction.CASH_SALE.name(),
        TypeFinancialTransaction.CREDIT_SALE.name(),
        TypeFinancialTransaction.REGLEMENT_DIFFERE.name(),
        TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT.name(),
        TypeFinancialTransaction.ENTREE_CAISSE.name()
    );


    private static final Set<String> EXCLUDED_PAYMENT_DTYPES = Set.of(
        "PaymentFournisseur"

    );

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterItemRepository cashRegisterItemRepository;
    private final SalesRepository salesRepository;
    private final CommandeRepository commandeRepository;

    public CaissierDashboardServiceImpl(
        CashRegisterRepository cashRegisterRepository,
        CashRegisterItemRepository cashRegisterItemRepository,
        SalesRepository salesRepository,
        CommandeRepository commandeRepository
    ) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashRegisterItemRepository = cashRegisterItemRepository;
        this.salesRepository = salesRepository;
        this.commandeRepository = commandeRepository;
    }



    @Override
    public CaissierDashboardDTO getDashboardData() {
        return new CaissierDashboardDTO(
            getCaisseStatus(),
            getSessionEncaissements(),
            getDifferesRelance(),
            getLivraisonsJour(),
            getVentesRecentes(8)
        );
    }



    @Override
    public CaisseStatusDTO getCaisseStatus() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return emptyStatus();

        List<Object[]> rows = cashRegisterRepository.findCurrentByUserLogin(login);

        if (rows.isEmpty()) {
            return buildStatusFermee(login);
        }

        Object[] row = rows.getFirst();
        Integer cashRegisterId = toInt(row[0]);
        Long fondOuverture     = toLong(row[1]);
        LocalDateTime beginTime = toLocalDateTime(row[2]);
        String statut           = (String) row[3];
        LocalDateTime endTime   = toLocalDateTime(row[4]);

        boolean isOpen = CashRegisterStatut.OPEN.name().equals(statut) || CashRegisterStatut.PENDING.name().equals(statut);
        String heureOuverture = beginTime != null ? beginTime.format(HEURE_FMT) : null;
        String etat = isOpen ? "OUVERTE" : "FERMEE";

        Long encaissementsEspeces = cashRegisterItemRepository
            .sumEncaissementsEspeces(cashRegisterId, VENTE_CASH_TRANSACTIONS, EXCLUDED_PAYMENT_DTYPES);
        if (encaissementsEspeces == null) encaissementsEspeces = 0L;
        Long especesTheoriques = fondOuverture + encaissementsEspeces;

        return new CaisseStatusDTO(
            fondOuverture, encaissementsEspeces, especesTheoriques,
            heureOuverture, etat,
            isOpen ? null : endTime
        );
    }

    private CaisseStatusDTO buildStatusFermee(String login) {
        List<Object> rows = cashRegisterRepository.findLastClosedTimeByUserLogin(login);
        LocalDateTime lastClose = rows.isEmpty() ? null : toLocalDateTime(rows.getFirst());
        return new CaisseStatusDTO(0L, 0L, 0L, null, "FERMEE", lastClose);
    }

    private CaisseStatusDTO emptyStatus() {
        return new CaisseStatusDTO(0L, 0L, 0L, null, "FERMEE", null);
    }



    private static final Set<String> GROUPES_ENCAISSE = Set.of(PaymentGroup.CASH.name(), PaymentGroup.MOBILE.name(),PaymentGroup.CB.name(),
        PaymentGroup.CHEQUE.name(), PaymentGroup.VIREMENT.name());

    @Override
    public SessionEncaissementsDTO getSessionEncaissements() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return emptySessionEncaissements();

        Integer cashRegisterId = findCurrentCashRegisterId(login);
        if (cashRegisterId == null) return emptySessionEncaissements();

        // Lignes dynamiques : une par mode de paiement utilisé dans la session
        List<Object[]> itemRows = cashRegisterItemRepository
            .findEncaissementsParMode(cashRegisterId, EXCLUDED_PAYMENT_DTYPES);

        List<EncaissementParModeDTO> lignes = new ArrayList<>();
        long totalEncaisse = 0L;
        long totalARecouvrerModes = 0L;

        for (Object[] r : itemRows) {
            String code         = (String) r[0];
            String libelle      = (String) r[1];
            String paymentGroup = (String) r[2];
            long montant        = toLong(r[3]);

            lignes.add(new EncaissementParModeDTO(code, libelle, paymentGroup, montant));

            if (GROUPES_ENCAISSE.contains(paymentGroup)) {
                totalEncaisse += montant;
            } else {
                totalARecouvrerModes += montant;
            }
        }

        // Compléments depuis les ventes (carnet + différé)
        List<Object[]> saleRows = salesRepository.findSalesEncaissementsForCaisse(cashRegisterId);
        long carnet = 0L, differe = 0L;
        int nombreTransactions = 0;
        if (!saleRows.isEmpty()) {
            Object[] r = saleRows.getFirst();
            carnet             = toLong(r[0]);
            differe            = toLong(r[1]);
            nombreTransactions = toInt(r[2]);
        }

        long totalARecouvrer = totalARecouvrerModes + carnet + differe;

        return new SessionEncaissementsDTO(
            lignes, carnet, differe,
            totalEncaisse, totalARecouvrer, nombreTransactions
        );
    }

    private Integer findCurrentCashRegisterId(String login) {
        List<Object> rows = cashRegisterRepository.findCurrentIdByUserLogin(login);
        return rows.isEmpty() ? null : toInt(rows.getFirst());
    }

    private SessionEncaissementsDTO emptySessionEncaissements() {
        return new SessionEncaissementsDTO(List.of(), 0L, 0L, 0L, 0L, 0);
    }


    @Override
    public ResumeDifferesDTO getDifferesRelance() {
        List<Object[]> rows = salesRepository.findDifferesARelancer();

        List<DiffereARelancerDTO> differes = new ArrayList<>();
        long montantTotal = 0L;
        int nbAujourdhui = 0;

        for (Object[] row : rows) {
            Long saleId         = toLong(row[0]);
            String clientNom    = (String) row[1];
            String telephone    = (String) row[2];
            long montantDu      = toLong(row[3]);
            LocalDate date      = toLocalDate(row[4]);
            int joursRetard = toInt(row[5]);

            String urgence;
            if (joursRetard == 0) {
                urgence = "AUJOURD_HUI";
                nbAujourdhui++;
            } else if (joursRetard > 7) {
                urgence = "CRITIQUE";
            } else {
                urgence = "RETARD";
            }

            montantTotal += montantDu;
            differes.add(new DiffereARelancerDTO(
                saleId, clientNom, telephone, montantDu, date, joursRetard, urgence
            ));
        }

        return new ResumeDifferesDTO(nbAujourdhui, montantTotal, differes);
    }


    @Override
    public List<LivraisonAttendueDTO> getLivraisonsJour() {
        List<Object[]> rows = commandeRepository.findLivraisonsAttenduesAujourdhui();
        List<LivraisonAttendueDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Integer commandeId   = toInt(row[0]);
            String fournisseurNom = (String) row[1];
            Integer nombreRefs    = toInt(row[2]);
            result.add(new LivraisonAttendueDTO(commandeId, fournisseurNom,  nombreRefs));
        }

        return result;
    }


    @Override
    public List<VenteRecenteDTO> getVentesRecentes(Integer limit) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return Collections.emptyList();

        int maxResults = limit != null && limit > 0 ? limit : 8;
        List<Object[]> rows = salesRepository.findVentesRecentesByCaissier(login);

        List<VenteRecenteDTO> ventes = new ArrayList<>();
        for (Object[] row : rows.stream().limit(maxResults).toList()) {
            Long saleId          = toLong(row[0]);
            String numeroRecu    = (String) row[1];
            Long montant         = toLong(row[2]);
            LocalDateTime date   = toLocalDateTime(row[3]);
            String typeVente     = (String) row[4];
            String clientNom     = (String) row[5];

            ventes.add(new VenteRecenteDTO(
                saleId, numeroRecu, montant, date,
                typeVente, // modePaiement dérivé du type
                typeVente,
                clientNom
            ));
        }

        return ventes;
    }


    private static int toInt(Object value) {
        if (value == null) return 0;
        return ((Number) value).intValue();
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        return ((Number) value).longValue();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        return null;
    }
}
