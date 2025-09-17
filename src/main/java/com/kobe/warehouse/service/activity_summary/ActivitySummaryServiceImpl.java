package com.kobe.warehouse.service.activity_summary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaire;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.Recette;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;
import com.kobe.warehouse.service.dto.records.ActivitySummaryRecord;
import com.kobe.warehouse.service.dto.records.ChiffreAffaireRecord;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.utils.DateUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.nonNull;

@Service
@Transactional(readOnly = true)
public class ActivitySummaryServiceImpl implements ActivitySummaryService {

    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryServiceImpl.class);
    private final TiersPayantService payantService;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final CommandeRepository commandeRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SalePaymentRepository paymentRepository;
    private final SalesRepository salesRepository;
    private final ActivitySummaryReportService activitySummaryReportService;
    private final ObjectMapper objectMapper;

    public ActivitySummaryServiceImpl(
        TiersPayantService payantService,
        InvoicePaymentRepository invoicePaymentRepository,
        CommandeRepository commandeRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        SalePaymentRepository paymentRepository,
        SalesRepository salesRepository,
        ActivitySummaryReportService activitySummaryReportService, ObjectMapper objectMapper
    ) {
        this.payantService = payantService;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.commandeRepository = commandeRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
        this.activitySummaryReportService = activitySummaryReportService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChiffreAffaireDTO getChiffreAffaire(LocalDate fromDate, LocalDate toDate) {
        ChiffreAffaire chiffreAffaire = getChiffreAffaireSummary(fromDate, toDate);

        ChiffreAffaireAchat achats = this.fetchAchats(fromDate, toDate);
        List<MouvementCaisse> mvts = this.findMouvementsCaisse(fromDate, toDate);
        BigDecimal montantEspece = BigDecimal.ZERO;
        BigDecimal montantRegle = BigDecimal.ZERO;
        BigDecimal montantAutreModePaiement = BigDecimal.ZERO;
        List<Recette> recettes =nonNull(chiffreAffaire) ? chiffreAffaire.payments() : List.of();
        for (Recette recette : recettes) {
            montantRegle = montantRegle.add(recette.getMontantReel());
            if (recette.getModePaimentCode() == ModePaimentCode.CASH) {
                montantEspece = montantEspece.add(recette.getMontantReel());
            } else {
                montantAutreModePaiement = montantAutreModePaiement.add(recette.getMontantReel());
            }
        }
        //TODO si gestion de ug
        var caRecord = new ChiffreAffaireRecord(
          nonNull(chiffreAffaire)?  chiffreAffaire.montantTtc():BigDecimal.ZERO,
            nonNull(chiffreAffaire)? chiffreAffaire.getMontantTva():BigDecimal.ZERO,
            nonNull(chiffreAffaire)?  chiffreAffaire.montantHt():BigDecimal.ZERO,
            nonNull(chiffreAffaire)?  chiffreAffaire.montantRemise():BigDecimal.ZERO,
            nonNull(chiffreAffaire)?  chiffreAffaire.montantNet():BigDecimal.ZERO,
            montantEspece,
            nonNull(chiffreAffaire)?  chiffreAffaire.getMontantCredit():BigDecimal.ZERO,
            montantRegle,
            montantAutreModePaiement,
            nonNull(chiffreAffaire)? chiffreAffaire.getMarge():BigDecimal.ZERO
        );
        return new ChiffreAffaireDTO(recettes, caRecord, achats, mvts);
    }

    @Override
    public List<Recette> findRecettes(LocalDate fromDate, LocalDate toDate) {
        return this.paymentRepository.findRecettes(fromDate, toDate);
    }

    @Override
    public Page<GroupeFournisseurAchat> fetchAchats(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return this.commandeRepository.fetchAchats(fromDate, toDate, OrderStatut.CLOSED, pageable);
    }

    private ChiffreAffaireAchat fetchAchats(LocalDate fromDate, LocalDate toDate) {
        return this.commandeRepository.fetchAchats(fromDate, toDate, OrderStatut.CLOSED);
    }

    @Override
    public List<MouvementCaisse> findMouvementsCaisse(LocalDate fromDate, LocalDate toDate) {
        return this.paymentTransactionRepository.findMouvementsCaisse(fromDate, toDate);
    }

    @Override
    public Resource printToPdf(LocalDate fromDate, LocalDate toDate, String searchAchatTp, String searchReglement)
        throws ReportFileExportException {
        return this.activitySummaryReportService.printToPdf(
                new ActivitySummaryRecord(
                    getChiffreAffaire(fromDate, toDate),
                    fetchAchatTiersPayant(fromDate, toDate, searchAchatTp, Pageable.unpaged()).getContent(),
                    findReglementTierspayant(fromDate, toDate, searchReglement, Pageable.unpaged()).getContent(),
                    fetchAchats(fromDate, toDate, Pageable.unpaged()).getContent(),
                    " du " + DateUtil.formatFr(fromDate) + " au " + toDate
                )
            );
    }

    @Override
    public Page<AchatTiersPayant> fetchAchatTiersPayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        return this.payantService.fetchAchatTiersPayant(fromDate, toDate, search,  pageable);
    }

    @Override
    public Page<ReglementTiersPayants> findReglementTierspayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        return this.invoicePaymentRepository.findReglementTierspayant(fromDate, toDate, search, pageable);
    }

    private ChiffreAffaire getChiffreAffaireSummary(LocalDate fromDate, LocalDate toDate) {
        try {
            String jsonResult = this.salesRepository.getChiffreAffaire(fromDate, toDate, SalesStatut.getStatutForFacturation().stream().map(SalesStatut::name).toArray(String[]::new), Set.of(CategorieChiffreAffaire.CA).stream().map(CategorieChiffreAffaire::name).toArray(String[]::new),false,false);
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });

        } catch (Exception e) {
            LOG.info(null, e);
            return null;
        }
    }
}
