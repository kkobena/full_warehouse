package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.StatutCompteFournisseur;
import com.kobe.warehouse.domain.enumeration.StatutLigneFournisseurAP;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.PaymentFournisseurRepository;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import com.kobe.warehouse.service.dto.ReglementBLDTO;
import com.kobe.warehouse.service.dto.ReglementFournisseurAPCommand;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.report.pdf.AccountsPayableApPdfExportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountsPayableServiceImpl implements AccountsPayableService {

    private static final Set<OrderStatut> RECEIVED_STATUTS = EnumSet.of(OrderStatut.CLOSED);

    private final CommandeRepository commandeRepository;
    private final PaymentFournisseurRepository paymentFournisseurRepository;
    private final CashRegisterService cashRegisterService;
    private final TransactionIdGeneratorService idGenerator;
    private final AppConfigurationService appConfigurationService;
    private final AccountsPayableApPdfExportService pdfExportService;

    public AccountsPayableServiceImpl(
        CommandeRepository commandeRepository,
        PaymentFournisseurRepository paymentFournisseurRepository,
        CashRegisterService cashRegisterService,
        TransactionIdGeneratorService idGenerator,
        AppConfigurationService appConfigurationService,
        AccountsPayableApPdfExportService pdfExportService
    ) {
        this.commandeRepository = commandeRepository;
        this.paymentFournisseurRepository = paymentFournisseurRepository;
        this.cashRegisterService = cashRegisterService;
        this.idGenerator = idGenerator;
        this.appConfigurationService = appConfigurationService;
        this.pdfExportService = pdfExportService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompteFournisseurAPDTO> getComptes(LocalDate fromDate, LocalDate toDate) {
        List<Commande> commandes = loadUnpaidCommandes(null, fromDate, toDate);
        Map<Integer, Long> paidMap = loadPaidAmounts(commandes);

        Map<Integer, List<Commande>> byFournisseur = commandes.stream()
            .filter(c -> c.getFournisseur() != null)
            .collect(Collectors.groupingBy(c -> c.getFournisseur().getId()));

        return byFournisseur.values().stream()
            .map(fCmds -> buildCompteFournisseur(fCmds, paidMap))
            .sorted(Comparator.comparingLong(CompteFournisseurAPDTO::solde).reversed())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FournisseurAPSummaryDTO getSummary() {
        List<Commande> commandes = loadUnpaidCommandes(null, null, null);
        Map<Integer, Long> paidMap = loadPaidAmounts(commandes);

        LocalDate now = LocalDate.now();
        LocalDate soonLimit = now.plusDays(7);

        long totalDu = 0;
        Set<Integer> depassees = new HashSet<>();
        Set<Integer> prochaines = new HashSet<>();
        Set<Integer> actifs = new HashSet<>();

        for (Commande c : commandes) {
            if (c.getFournisseur() == null) continue;
            Integer fId = c.getFournisseur().getId();
            actifs.add(fId);

            long gross = (long) Objects.requireNonNullElse(c.getGrossAmount(), 0);
            long paid = paidMap.getOrDefault(intId(c), 0L);
            long restant = gross - paid;
            if (restant > 0) totalDu += restant;

            LocalDate ech = computeEcheance(c);
            if (ech.isBefore(now)) {
                depassees.add(fId);
            } else if (!ech.isAfter(soonLimit)) {
                prochaines.add(fId);
            }
        }

        return new FournisseurAPSummaryDTO(totalDu, depassees.size(), prochaines.size(), actifs.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFournisseurAPDTO> getLignes(Integer fournisseurId, StatutLigneFournisseurAP statut, Pageable pageable) {
        List<Commande> commandes = loadUnpaidCommandes(fournisseurId, null, null);
        Map<Integer, Long> paidMap = loadPaidAmounts(commandes);

        LocalDate now = LocalDate.now();
        List<LigneFournisseurAPDTO> all = commandes.stream()
            .map(c -> {
                long montant = (long) Objects.requireNonNullElse(c.getGrossAmount(), 0);
                long montantRegle = paidMap.getOrDefault(intId(c), 0L);
                long restantDu = montant - montantRegle;
                LocalDate ech = computeEcheance(c);
                StatutLigneFournisseurAP s = computeStatutLigne(c.getPaimentStatut(), montantRegle, ech, now);
                String numBon = Objects.requireNonNullElse(c.getReceiptReference(), c.getOrderReference());
                return new LigneFournisseurAPDTO(intId(c), numBon, c.getOrderDate().toString(), ech.toString(), montant, montantRegle, restantDu, s.name());
            })
            .filter(dto -> statut == null || statut.name().equals(dto.statut()))
            .sorted(Comparator.comparing(LigneFournisseurAPDTO::dateCommande))
            .collect(Collectors.toList());

        int total = all.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<LigneFournisseurAPDTO> page = start < total ? all.subList(start, end) : List.of();
        return new PageImpl<>(page, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReglementBLDTO> getReglementsBl(Integer fournisseurId, Integer commandeId) {
        List<PaymentFournisseur> payments = paymentFournisseurRepository
            .findByCommandeIdAndFournisseurId(commandeId, fournisseurId);

        return payments.stream().map(pf -> {
            String operateur = null;
            if (pf.getCashRegister() != null && pf.getCashRegister().getUser() != null) {
                var u = pf.getCashRegister().getUser();
                operateur = u.getFirstName() + " " + u.getLastName();
            }
            return new ReglementBLDTO(
                pf.getId() != null ? pf.getId().getId() : null,
                pf.getTransactionDate().toString(),
                Objects.requireNonNullElse(pf.getPaidAmount(), 0),
                pf.getTransactionNumber(),
                pf.getCommentaire(),
                operateur
            );
        }).collect(Collectors.toList());
    }

    @Override
    public void enregistrerReglement(Integer fournisseurId, ReglementFournisseurAPCommand command) {
        List<Commande> commandes = loadUnpaidCommandes(fournisseurId, null, null);
        Map<Integer, Long> paidMap = loadPaidAmounts(commandes);

        int remaining = command.montant();
        List<PaymentFournisseur> toSave = new ArrayList<>();
        LocalDate paymentDate = LocalDate.parse(command.dateReglement(), DateTimeFormatter.ISO_DATE);

        // Si commandeId fourni, imputer sur ce BL en priorité
        if (command.commandeId() != null) {
            remaining = imputerSurCommande(command.commandeId(), commandes, paidMap, remaining,
                paymentDate, command, toSave);
        }

        // Distribuer le restant sur les autres commandes (FIFO)
        for (Commande commande : commandes) {
            if (remaining <= 0) break;
            if (command.commandeId() != null && intId(commande).equals(command.commandeId())) continue;

            long alreadyPaid = paidMap.getOrDefault(intId(commande), 0L);
            long restant = (long) Objects.requireNonNullElse(commande.getGrossAmount(), 0) - alreadyPaid;
            if (restant <= 0) continue;

            int allocated = (int) Math.min(remaining, restant);
            remaining -= allocated;
            toSave.add(buildPayment(commande, allocated, (int) restant, command.montant(), paymentDate, command));

            if (alreadyPaid + allocated >= (long) Objects.requireNonNullElse(commande.getGrossAmount(), 0)) {
                commande.setPaimentStatut(PaimentStatut.PAID);
                commandeRepository.save(commande);
            }
        }

        paymentFournisseurRepository.saveAll(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportComptesAsPdf(LocalDate fromDate, LocalDate toDate) {
        List<CompteFournisseurAPDTO> comptes = getComptes(fromDate, toDate);
        FournisseurAPSummaryDTO summary = getSummary();
        return pdfExportService.exportGlobal(comptes, summary, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportFournisseurAsPdf(Integer fournisseurId) {
        List<Commande> commandes = loadUnpaidCommandes(fournisseurId, null, null);
        Map<Integer, Long> paidMap = loadPaidAmounts(commandes);

        if (commandes.isEmpty()) {
            return pdfExportService.exportGlobal(List.of(), null, null, null);
        }

        CompteFournisseurAPDTO compte = buildCompteFournisseur(commandes, paidMap);
        LocalDate now = LocalDate.now();
        List<LigneFournisseurAPDTO> lignes = commandes.stream()
            .map(c -> {
                long montant = (long) Objects.requireNonNullElse(c.getGrossAmount(), 0);
                long montantRegle = paidMap.getOrDefault(intId(c), 0L);
                long restantDu = montant - montantRegle;
                LocalDate ech = computeEcheance(c);
                StatutLigneFournisseurAP s = computeStatutLigne(c.getPaimentStatut(), montantRegle, ech, now);
                String numBon = Objects.requireNonNullElse(c.getReceiptReference(), c.getOrderReference());
                return new LigneFournisseurAPDTO(intId(c), numBon, c.getOrderDate().toString(), ech.toString(), montant, montantRegle, restantDu, s.name());
            })
            .sorted(Comparator.comparing(LigneFournisseurAPDTO::dateCommande))
            .collect(Collectors.toList());

        return pdfExportService.exportFournisseur(compte, lignes);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int imputerSurCommande(
        Integer commandeId,
        List<Commande> commandes,
        Map<Integer, Long> paidMap,
        int remaining,
        LocalDate paymentDate,
        ReglementFournisseurAPCommand command,
        List<PaymentFournisseur> toSave
    ) {
        for (Commande commande : commandes) {
            if (!intId(commande).equals(commandeId)) continue;

            long alreadyPaid = paidMap.getOrDefault(intId(commande), 0L);
            long restant = (long) Objects.requireNonNullElse(commande.getGrossAmount(), 0) - alreadyPaid;
            if (restant <= 0) break;

            int allocated = (int) Math.min(remaining, restant);
            remaining -= allocated;
            toSave.add(buildPayment(commande, allocated, (int) restant, command.montant(), paymentDate, command));

            if (alreadyPaid + allocated >= (long) Objects.requireNonNullElse(commande.getGrossAmount(), 0)) {
                commande.setPaimentStatut(PaimentStatut.PAID);
                commandeRepository.save(commande);
            }
            break;
        }
        return remaining;
    }

    private PaymentFournisseur buildPayment(
        Commande commande,
        int allocated,
        int restant,
        int montantVerse,
        LocalDate paymentDate,
        ReglementFournisseurAPCommand command
    ) {
        PaymentFournisseur pf = new PaymentFournisseur();
        pf.setId(idGenerator.nextId());
        pf.setCommande(commande);
        pf.setTransactionDate(paymentDate);
        pf.setPaidAmount(allocated);
        pf.setReelAmount(allocated);
        pf.setExpectedAmount(restant);
        pf.setMontantVerse(montantVerse);
        pf.setTypeFinancialTransaction(TypeFinancialTransaction.REGLMENT_FOURNISSEUR);
        pf.setCashRegister(cashRegisterService.getCashRegister());
        pf.setPaymentMode(new PaymentMode().code(command.modeReglement()));
        pf.setTransactionNumber(command.reference());
        pf.setCommentaire(command.commentaire());
        return pf;
    }

    private CompteFournisseurAPDTO buildCompteFournisseur(List<Commande> fCmds, Map<Integer, Long> paidMap) {
        Fournisseur f = fCmds.getFirst().getFournisseur();
        int creditDays = resolveJoursCredit(f);
        int critiqueDays = resolveJoursCritique(f);

        LocalDate now = LocalDate.now();
        long totalCommande = 0;
        long totalRegle = 0;
        long nbEnAttente = 0;
        LocalDate prochaineEcheance = null;
        boolean hasCritique = false;
        boolean hasEnRetard = false;

        for (Commande c : fCmds) {
            long gross = (long) Objects.requireNonNullElse(c.getGrossAmount(), 0);
            long paid = paidMap.getOrDefault(intId(c), 0L);
            totalCommande += gross;
            totalRegle += paid;

            if (c.getPaimentStatut() != PaimentStatut.PAID) {
                nbEnAttente++;
                LocalDate ech = computeEcheanceFor(c, creditDays);
                if (prochaineEcheance == null || ech.isBefore(prochaineEcheance)) {
                    prochaineEcheance = ech;
                }
                if (ech.plusDays(critiqueDays).isBefore(now)) {
                    hasCritique = true;
                } else if (ech.isBefore(now)) {
                    hasEnRetard = true;
                }
            }
        }

        StatutCompteFournisseur statut = hasCritique ? StatutCompteFournisseur.CRITIQUE
            : hasEnRetard ? StatutCompteFournisseur.EN_RETARD
            : StatutCompteFournisseur.A_JOUR;

        return new CompteFournisseurAPDTO(
            f.getId(), f.getLibelle(), f.getCode(), f.getPhone(), f.getMobile(),
            totalCommande, totalRegle, totalCommande - totalRegle,
            nbEnAttente,
            prochaineEcheance != null ? prochaineEcheance.toString() : null,
            statut.name()
        );
    }

    private int resolveJoursCredit(Fournisseur f) {
        if (f.getJoursCredit() != null) return f.getJoursCredit();
        GroupeFournisseur g = f.getGroupeFournisseur();
        if (g != null && g.getJoursCredit() != null) return g.getJoursCredit();
        return appConfigurationService.getApDefaultCreditDays();
    }

    private int resolveJoursCritique(Fournisseur f) {
        if (f.getJoursCritique() != null) return f.getJoursCritique();
        GroupeFournisseur g = f.getGroupeFournisseur();
        if (g != null && g.getJoursCritique() != null) return g.getJoursCritique();
        return appConfigurationService.getApDefaultCritiqueDays();
    }

    private Integer intId(Commande c) {
        return c.getId().getId();
    }

    private LocalDate computeEcheance(Commande c) {
        Fournisseur f = c.getFournisseur();
        int days = (f != null) ? resolveJoursCredit(f) : appConfigurationService.getApDefaultCreditDays();
        return computeEcheanceFor(c, days);
    }

    private LocalDate computeEcheanceFor(Commande c, int creditDays) {
        LocalDate base = Objects.requireNonNullElse(c.getReceiptDate(), c.getOrderDate());
        return base.plusDays(creditDays);
    }

    private StatutLigneFournisseurAP computeStatutLigne(PaimentStatut paimentStatut, long montantRegle, LocalDate echeance, LocalDate now) {
        if (paimentStatut == PaimentStatut.PAID) return StatutLigneFournisseurAP.REGLE;
        if (montantRegle > 0) return StatutLigneFournisseurAP.PARTIEL;
        if (echeance.isBefore(now)) return StatutLigneFournisseurAP.EN_RETARD;
        return StatutLigneFournisseurAP.EN_ATTENTE;
    }

    private List<Commande> loadUnpaidCommandes(Integer fournisseurId, LocalDate fromDate, LocalDate toDate) {
        if (fournisseurId != null) {
            return commandeRepository.findUnpaidCommandesApByFournisseurAndPeriod(
                RECEIVED_STATUTS, PaimentStatut.PAID, fournisseurId, fromDate, toDate);
        }
        return commandeRepository.findUnpaidCommandesApByPeriod(RECEIVED_STATUTS, PaimentStatut.PAID, fromDate, toDate);
    }

    private Map<Integer, Long> loadPaidAmounts(List<Commande> commandes) {
        if (commandes.isEmpty()) return Map.of();

        List<Integer> ids = commandes.stream()
            .map(this::intId)
            .collect(Collectors.toList());

        List<Object[]> rows = paymentFournisseurRepository.sumPaidAmountsByCommandeIds(ids);

        Map<Integer, Long> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            result.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }
}
