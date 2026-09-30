package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.license.DemoWatermark;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Documents remis au client (docs/PLAN-FICHE-CLIENT.md, lot 5) : relevé de compte et attestation
 * de dépenses sur une période.
 *
 * <p>Le relevé suit la règle de l'encours (lot 0) : ventes différées clôturées et non annulées ;
 * chacune entre au débit pour ce qui restait dû à la vente, chaque règlement au crédit. Le solde
 * à aujourd'hui est donc l'encours affiché sur la fiche.
 */
@Service
@Transactional(readOnly = true)
public class CustomerDocumentService {

    private static final String VENTES_DIFFEREES = "s.customer_id = :id AND s.differe AND s.statut = 'CLOSED' AND NOT s.canceled";

    private final EntityManager em;
    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final AppConfigurationService appConfigurationService;

    public CustomerDocumentService(
        EntityManager em,
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        AppConfigurationService appConfigurationService
    ) {
        this.em = em;
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.appConfigurationService = appConfigurationService;
    }

    public ReleveCompteDTO releve(Integer customerId, LocalDate debut, LocalDate fin) {
        ClientDocumentDTO client = client(customerId);
        verifierPeriode(debut, fin);

        List<Mouvement> mouvements = Stream.concat(ventesDifferees(customerId).stream(), reglements(customerId).stream())
            .sorted(Comparator.comparing(Mouvement::date).thenComparing(Mouvement::horodatage, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        long solde = mouvements.stream().filter(m -> m.date().isBefore(debut)).mapToLong(m -> m.debit() - m.credit()).sum();
        long soldeInitial = solde;
        long totalDebit = 0;
        long totalCredit = 0;
        List<LigneReleveDTO> lignes = new ArrayList<>();
        for (Mouvement m : mouvements) {
            if (m.date().isBefore(debut) || m.date().isAfter(fin)) {
                continue;
            }
            solde += m.debit() - m.credit();
            totalDebit += m.debit();
            totalCredit += m.credit();
            lignes.add(new LigneReleveDTO(m.date(), m.libelle(), m.reference(), m.debit(), m.credit(), solde));
        }
        return new ReleveCompteDTO(client, debut, fin, soldeInitial, lignes, totalDebit, totalCredit, solde);
    }

    /** Achats clôturés du client et de ses ayants droit, ou de l'ayant droit lui-même. */
    public AttestationDepensesDTO attestation(Integer customerId, LocalDate debut, LocalDate fin) {
        ClientDocumentDTO client = client(customerId);
        verifierPeriode(debut, fin);
        String filtre =
            " FROM sales s JOIN customer c ON c.id = s.customer_id LEFT JOIN customer ad ON ad.id = s.ayant_droit_id" +
            " WHERE (s.customer_id = :id OR s.ayant_droit_id = :id) AND s.statut = 'CLOSED' AND NOT s.canceled" +
            " AND s.sale_date BETWEEN :debut AND :fin";

        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em
            .createNativeQuery(
                "SELECT s.id, s.sale_date, p.libelle, l.quantity_sold, l.sales_amount" +
                filtre.replace(" FROM sales s", " FROM sales_line l JOIN produit p ON p.id = l.produit_id JOIN sales s ON s.id = l.sales_id AND s.sale_date = l.sales_sale_date") +
                " ORDER BY p.libelle"
            )
            .setParameter("id", customerId)
            .setParameter("debut", debut)
            .setParameter("fin", fin)
            .getResultList();
        Map<String, List<DepenseDTO.ProduitDepenseDTO>> produits = lignes
            .stream()
            .collect(
                Collectors.groupingBy(
                    r -> cle(r[0], r[1]),
                    LinkedHashMap::new,
                    Collectors.mapping(r -> new DepenseDTO.ProduitDepenseDTO((String) r[2], entier(r[3]), longue(r[4])), Collectors.toList())
                )
            );

        @SuppressWarnings("unchecked")
        List<Object[]> ventes = em
            .createNativeQuery(
                "SELECT s.id, s.sale_date, s.number_transaction, s.sales_amount - s.discount_amount, COALESCE(s.part_tiers_payant, 0)," +
                " COALESCE(ad.first_name || ' ' || ad.last_name, c.first_name || ' ' || c.last_name)" +
                filtre +
                " ORDER BY s.sale_date, s.created_at"
            )
            .setParameter("id", customerId)
            .setParameter("debut", debut)
            .setParameter("fin", fin)
            .getResultList();
        List<DepenseDTO> depenses = ventes
            .stream()
            .map(r -> {
                long montant = longue(r[3]);
                long partTiersPayant = longue(r[4]);
                return new DepenseDTO(
                    date(r[1]),
                    (String) r[2],
                    (String) r[5],
                    produits.getOrDefault(cle(r[0], r[1]), List.of()),
                    montant,
                    partTiersPayant,
                    montant - partTiersPayant
                );
            })
            .toList();
        return new AttestationDepensesDTO(
            client,
            debut,
            fin,
            depenses,
            depenses.stream().mapToLong(DepenseDTO::montant).sum(),
            depenses.stream().mapToLong(DepenseDTO::partTiersPayant).sum(),
            depenses.stream().mapToLong(DepenseDTO::partClient).sum()
        );
    }

    public byte[] relevePdf(Integer customerId, LocalDate debut, LocalDate fin) {
        ReleveCompteDTO releve = releve(customerId, debut, fin);
        return pdf("customer/releve", "RELEVÉ DE COMPTE DU " + jour(debut) + " AU " + jour(fin), Map.of("releve", releve));
    }

    public byte[] attestationPdf(Integer customerId, LocalDate debut, LocalDate fin) {
        AttestationDepensesDTO attestation = attestation(customerId, debut, fin);
        return pdf("customer/attestation", "ATTESTATION DE DÉPENSES", Map.of("attestation", attestation));
    }

    private byte[] pdf(String template, String titre, Map<String, Object> variables) {
        Magasin magasin = storageService.getUser().getMagasin();
        Context context = new Context(Locale.FRENCH);
        context.setVariable("magasin", magasin);
        context.setVariable("reportTitle", titre);
        context.setVariable("footer", "\"" + piedDePage(magasin) + "\"");
        context.setVariable("devise", appConfigurationService.getDevise());
        context.setVariable("dateEdition", LocalDate.now());
        variables.forEach(context::setVariable);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(DemoWatermark.apply(templateEngine.process(template, context)));
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Génération du document client impossible : " + e.getMessage(), e);
        }
    }

    private static String piedDePage(Magasin magasin) {
        return Stream.of(
            StringUtils.hasText(magasin.getRegistre()) ? "RC N° " + magasin.getRegistre() : null,
            StringUtils.hasText(magasin.getCompteContribuable()) ? "CC N° " + magasin.getCompteContribuable() : null,
            StringUtils.hasText(magasin.getPhone()) ? "Tel: " + magasin.getPhone() : null,
            StringUtils.hasText(magasin.getAddress()) ? "Adr: " + magasin.getAddress() : null
        )
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" - "));
    }

    private ClientDocumentDTO client(Integer customerId) {
        Customer c = em.find(Customer.class, customerId);
        if (c == null) {
            throw new GenericError("Client introuvable", "customerNotFound");
        }
        return new ClientDocumentDTO(c.getId(), c.getCode(), c.getFirstName() + " " + c.getLastName(), c.getPhone());
    }

    private static void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || debut.isAfter(fin)) {
            throw new GenericError("Période invalide", "periodeInvalide");
        }
    }

    /** Débit : ce qui restait dû à la vente, soit le reste actuel plus ce qui a été réglé depuis. */
    private List<Mouvement> ventesDifferees(Integer customerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em
            .createNativeQuery(
                "SELECT s.sale_date, s.number_transaction, s.rest_to_pay + COALESCE((SELECT SUM(i.paid_amount) FROM differe_payment_item i" +
                " WHERE i.sale_id = s.id AND i.sale_sale_date = s.sale_date), 0), s.created_at FROM sales s WHERE " +
                VENTES_DIFFEREES
            )
            .setParameter("id", customerId)
            .getResultList();
        return rows.stream().map(r -> new Mouvement(date(r[0]), "Vente différée", (String) r[1], longue(r[2]), 0, horodatage(r[3]))).toList();
    }

    /** Crédit : la part de chaque règlement imputée aux ventes du relevé. */
    private List<Mouvement> reglements(Integer customerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em
            .createNativeQuery(
                "SELECT p.transaction_date, p.transaction_number, SUM(i.paid_amount), pm.libelle, p.created_at" +
                " FROM differe_payment_item i" +
                " JOIN payment_transaction p ON p.id = i.differe_payment_id AND p.transaction_date = i.differe_payment_transaction_date" +
                " JOIN sales s ON s.id = i.sale_id AND s.sale_date = i.sale_sale_date" +
                " LEFT JOIN payment_mode pm ON pm.code = p.payment_mode_code" +
                " WHERE " +
                VENTES_DIFFEREES +
                " GROUP BY p.id, p.transaction_date, p.transaction_number, pm.libelle, p.created_at"
            )
            .setParameter("id", customerId)
            .getResultList();
        return rows
            .stream()
            .map(r ->
                new Mouvement(
                    date(r[0]),
                    r[3] == null ? "Règlement" : "Règlement " + ((String) r[3]).toLowerCase(Locale.ROOT),
                    (String) r[1],
                    0,
                    longue(r[2]),
                    horodatage(r[4])
                )
            )
            .toList();
    }

    private record Mouvement(LocalDate date, String libelle, String reference, long debit, long credit, LocalDateTime horodatage) {}

    private static String cle(Object id, Object date) {
        return id + "|" + date;
    }

    private static String jour(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static LocalDate date(Object valeur) {
        return valeur instanceof Date d ? d.toLocalDate() : (LocalDate) valeur;
    }

    private static LocalDateTime horodatage(Object valeur) {
        return valeur instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) valeur;
    }

    private static long longue(Object valeur) {
        return valeur == null ? 0 : ((Number) valeur).longValue();
    }

    private static int entier(Object valeur) {
        return valeur == null ? 0 : ((Number) valeur).intValue();
    }
}
