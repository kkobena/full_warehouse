package com.kobe.warehouse.service.promotion.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.repository.PromotionRepository.Frequentation;
import com.kobe.warehouse.repository.PromotionRepository.VentesArticle;
import com.kobe.warehouse.service.dto.promotion.BilanPromotionDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.promotion.BilanPromotionService;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le bilan d'une promotion : ce qu'elle a coûté (la remise des lignes qu'elle a posées), ce qu'elle a
 * fait vendre, et l'effet sur la marge de ses articles comparé à la période précédente de même durée —
 * celle qui finit la veille de l'opération. La comparaison porte sur les mêmes articles, toutes ventes
 * confondues : c'est leur vente qui a bougé, pas seulement celle des lignes remisées.
 */
@Service
@Transactional(readOnly = true)
public class BilanPromotionServiceImpl implements BilanPromotionService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionRepository promotionRepository;
    private final PorteeMagasinService porteeMagasinService;
    private final CsvExportService csvExportService;

    public BilanPromotionServiceImpl(
            PromotionRepository promotionRepository,
            PorteeMagasinService porteeMagasinService,
            CsvExportService csvExportService) {
        this.promotionRepository = promotionRepository;
        this.porteeMagasinService = porteeMagasinService;
        this.csvExportService = csvExportService;
    }

    @Override
    public BilanPromotionDTO bilan(Long promotionId) {
        return bilan(promotionId, LocalDate.now());
    }

    BilanPromotionDTO bilan(Long promotionId, LocalDate aujourdhui) {
        Integer magasinId =
                porteeMagasinService.resoudreLeMagasinDExploitation().getId();
        Promotion promotion = promotionRepository
                .findOneWithProduits(promotionId)
                .filter(p -> p.getMagasins().stream().anyMatch(m -> Objects.equals(m.getId(), magasinId)))
                .orElseThrow(() -> new BadRequestAlertException("Promotion introuvable", "promotion", "idnotfound"));
        LocalDate debut = promotion.getDateDebut();
        if (debut.isAfter(aujourdhui)) {
            throw new BadRequestAlertException(
                    "La promotion n'a pas encore commencé : son bilan se lira à partir du " + debut.format(JOUR) + ".",
                    "promotion",
                    "bilanavenir");
        }
        LocalDate fin = promotion.getDateFin().isBefore(aujourdhui) ? promotion.getDateFin() : aujourdhui;
        long duree = ChronoUnit.DAYS.between(debut, fin) + 1;
        LocalDate finAvant = debut.minusDays(1);
        LocalDate debutAvant = debut.minusDays(duree);

        Map<Integer, String> libelles = promotion.getProduits().stream()
                .map(PromotionProduit::getProduit)
                .collect(Collectors.toMap(
                        Produit::getId, p -> Objects.requireNonNullElse(p.getLibelle(), ""), (a, b) -> a));
        List<Integer> produits = List.copyOf(libelles.keySet());

        Map<Integer, VentesArticle> pendant =
                parProduit(promotionRepository.ventesParArticle(promotion.getId(), produits, magasinId, debut, fin));
        Map<Integer, VentesArticle> avant = parProduit(
                promotionRepository.ventesParArticle(promotion.getId(), produits, magasinId, debutAvant, finAvant));

        List<BilanPromotionDTO.Article> articles = produits.stream()
                .map(id -> article(id, libelles.get(id), pendant.get(id), avant.get(id)))
                .sorted(Comparator.comparingLong(BilanPromotionDTO.Article::chiffreAffaires)
                        .reversed()
                        .thenComparing(BilanPromotionDTO.Article::libelle))
                .toList();

        Frequentation promo = promotionRepository.frequentationDeLaPromotion(promotion.getId(), magasinId);
        return new BilanPromotionDTO(
                promotion.getId(),
                promotion.getCode(),
                promotion.getLibelle(),
                debut,
                fin,
                debutAvant,
                finAvant,
                articles.stream().mapToLong(BilanPromotionDTO.Article::remise).sum(),
                articles.stream()
                        .mapToLong(BilanPromotionDTO.Article::unitesRemisees)
                        .sum(),
                promo == null ? 0 : valeur(promo.getTickets()),
                promo == null ? 0 : valeur(promo.getClients()),
                periode(pendant, promotionRepository.frequentationDesArticles(produits, magasinId, debut, fin)),
                periode(avant, promotionRepository.frequentationDesArticles(produits, magasinId, debutAvant, finAvant)),
                articles);
    }

    @Override
    public byte[] exporter(Long promotionId) {
        BilanPromotionDTO bilan = bilan(promotionId);
        String titre = "Bilan de la promotion " + bilan.code() + " « " + bilan.libelle() + " » du "
                + bilan.debut().format(JOUR) + " au " + bilan.fin().format(JOUR) + ", comparée au "
                + bilan.debutAvant().format(JOUR) + " - " + bilan.finAvant().format(JOUR) + " : remise "
                + bilan.remise() + ", " + bilan.tickets() + " tickets, " + bilan.clients() + " clients";
        String[] entetes = {
            "Article",
            "Unités vendues",
            "Unités avant",
            "Unités remisées",
            "Remise",
            "Chiffre d'affaires net",
            "Marge",
            "Marge avant",
            "Écart de marge"
        };
        try {
            return csvExportService.addUtf8Bom(csvExportService.createCsvReport(
                    titre, entetes, bilan.articles(), a -> new String[] {
                        a.libelle(),
                        String.valueOf(a.unites()),
                        String.valueOf(a.unitesAvant()),
                        String.valueOf(a.unitesRemisees()),
                        String.valueOf(a.remise()),
                        String.valueOf(a.chiffreAffaires()),
                        String.valueOf(a.marge()),
                        String.valueOf(a.margeAvant()),
                        String.valueOf(a.marge() - a.margeAvant())
                    }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<Integer, VentesArticle> parProduit(List<VentesArticle> ventes) {
        return ventes.stream().collect(Collectors.toMap(VentesArticle::getProduitId, Function.identity()));
    }

    private static BilanPromotionDTO.Article article(
            Integer produitId, String libelle, VentesArticle pendant, VentesArticle avant) {
        return new BilanPromotionDTO.Article(
                produitId,
                libelle,
                pendant == null ? 0 : valeur(pendant.getUnites()),
                avant == null ? 0 : valeur(avant.getUnites()),
                pendant == null ? 0 : valeur(pendant.getUnitesRemisees()),
                pendant == null ? 0 : valeur(pendant.getRemise()),
                pendant == null ? 0 : valeur(pendant.getChiffreAffaires()),
                pendant == null ? 0 : valeur(pendant.getMarge()),
                avant == null ? 0 : valeur(avant.getMarge()));
    }

    private static BilanPromotionDTO.Periode periode(Map<Integer, VentesArticle> ventes, Frequentation frequentation) {
        return new BilanPromotionDTO.Periode(
                ventes.values().stream().mapToLong(v -> valeur(v.getUnites())).sum(),
                ventes.values().stream()
                        .mapToLong(v -> valeur(v.getChiffreAffaires()))
                        .sum(),
                ventes.values().stream().mapToLong(v -> valeur(v.getMarge())).sum(),
                frequentation == null ? 0 : valeur(frequentation.getTickets()),
                frequentation == null ? 0 : valeur(frequentation.getClients()));
    }

    private static long valeur(Long nombre) {
        return nombre == null ? 0 : nombre;
    }
}
