package com.kobe.warehouse.web.rest.promotion;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.security.navaccess.NavAccessExempt;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.promotion.BilanPromotionDTO;
import com.kobe.warehouse.service.dto.promotion.ChoixPerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PackComptoirDTO;
import com.kobe.warehouse.service.dto.promotion.PerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionArticleDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionFormDTO;
import com.kobe.warehouse.service.etiquette.EtiquetteUsageService;
import com.kobe.warehouse.service.promotion.BilanPromotionService;
import com.kobe.warehouse.service.promotion.EtiquettesPromotion;
import com.kobe.warehouse.service.promotion.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Référentiel > Promotions : celles du magasin de l'utilisateur. Le droit est le nav_item {@code promotions}. */
@RestController
@RequestMapping("/api/promotions")
@RequiresNavAccess("promotions")
public class PromotionResource {

    private final PromotionService promotionService;
    private final EtiquetteUsageService etiquetteUsageService;
    private final BilanPromotionService bilanPromotionService;

    public PromotionResource(
            PromotionService promotionService,
            EtiquetteUsageService etiquetteUsageService,
            BilanPromotionService bilanPromotionService) {
        this.bilanPromotionService = bilanPromotionService;
        this.promotionService = promotionService;
        this.etiquetteUsageService = etiquetteUsageService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> lister() {
        return ResponseEntity.ok(promotionService.lister());
    }

    /** Les badges « en promotion » du catalogue et des favoris : le caissier les voit sans gérer les promotions. */
    @GetMapping("/en-vigueur")
    @NavAccessExempt("lecture, badge « en promotion » au comptoir pour tous les rôles")
    public ResponseEntity<List<PromotionArticleDTO>> articlesEnVigueur() {
        return ResponseEntity.ok(promotionService.articlesEnVigueur());
    }

    /**
     * {@code GET /api/promotions/pack?code=PK00000042} : le pack scanné au comptoir. Ouvert à tous les
     * rôles, comme le badge : le caissier vend le pack sans gérer les promotions.
     */
    @GetMapping("/pack")
    @NavAccessExempt("lecture, scan d'un pack au comptoir pour tous les rôles")
    public ResponseEntity<PackComptoirDTO> pack(@RequestParam String code) {
        return ResponseEntity.ok(promotionService.pack(code));
    }

    /** Les rayons du magasin et les familles, pour remplir d'un geste le périmètre d'une promotion. */
    @GetMapping("/perimetre/choix")
    public ResponseEntity<ChoixPerimetreDTO> choixPerimetre() {
        return ResponseEntity.ok(promotionService.choixPerimetre());
    }

    /** {@code GET /api/promotions/perimetre?rayonId=12} (ou {@code familleId}) : les articles à ajouter. */
    @GetMapping("/perimetre")
    public ResponseEntity<PerimetreDTO> perimetre(
            @RequestParam(required = false) Integer rayonId, @RequestParam(required = false) Integer familleId) {
        return ResponseEntity.ok(promotionService.perimetre(rayonId, familleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO> lire(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.lire(id));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO> creer(@Valid @RequestBody PromotionFormDTO form) {
        return ResponseEntity.ok(promotionService.creer(form));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDTO> modifier(@PathVariable Long id, @Valid @RequestBody PromotionFormDTO form) {
        return ResponseEntity.ok(promotionService.modifier(id, form));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<PromotionDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.valider(id));
    }

    @PutMapping("/{id}/suspendre")
    public ResponseEntity<PromotionDTO> suspendre(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.suspendre(id));
    }

    /**
     * {@code GET /api/promotions/{id}/etiquettes} : étiquettes de rayon « PROMO », une par article
     * remisé, répétée {@code qty} fois — sur le catalogue de formats commun aux étiquettes.
     */
    @GetMapping("/{id}/etiquettes")
    public ResponseEntity<byte[]> etiquettes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int qty,
            @RequestParam(defaultValue = "1") int startAt,
            @RequestParam(required = false) FormatEtiquette format) {
        PromotionDTO promotion = promotionService.lire(id);
        FormatEtiquette leFormat = format == null ? FormatEtiquette.parDefaut() : format;
        // Un pack s'étiquette sur son emballage (code-barres), une autre promotion en rayon.
        byte[] pdf = promotion.type() == TypePromotion.PROMO_PACK
                ? etiquetteUsageService.imprimerPack(
                        EtiquettesPromotion.construirePack(promotion, qty), startAt, leFormat)
                : etiquetteUsageService.imprimerPromotion(
                        EtiquettesPromotion.construire(promotion, qty), startAt, leFormat);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "etiquettes-promotion-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** Les mêmes étiquettes en ZPL, pour une imprimante à rouleau. */
    @GetMapping(value = "/{id}/etiquettes/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> etiquettesZpl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int qty,
            @RequestParam(required = false) FormatEtiquette format,
            @RequestParam(required = false) ResolutionEtiquette resolution) {
        PromotionDTO promotion = promotionService.lire(id);
        FormatEtiquette leFormat = format == null ? FormatEtiquette.parDefaut() : format;
        ResolutionEtiquette laResolution = resolution == null ? ResolutionEtiquette.parDefaut() : resolution;
        return ResponseEntity.ok(
                promotion.type() == TypePromotion.PROMO_PACK
                        ? etiquetteUsageService.genererZplPack(
                                EtiquettesPromotion.construirePack(promotion, qty), leFormat, laResolution)
                        : etiquetteUsageService.genererZplPromotion(
                                EtiquettesPromotion.construire(promotion, qty), leFormat, laResolution));
    }

    /**
     * {@code GET /api/promotions/{id}/bilan} : ce que l'opération a coûté, ce qu'elle a fait vendre, et
     * l'effet sur la marge de ses articles par rapport à la période précédente de même durée.
     */
    @GetMapping("/{id}/bilan")
    public ResponseEntity<BilanPromotionDTO> bilan(@PathVariable Long id) {
        return ResponseEntity.ok(bilanPromotionService.bilan(id));
    }

    /** Le bilan en CSV, une ligne par article : il demande le droit d'export. */
    @GetMapping("/{id}/bilan/csv")
    @RequiresNavAccess(value = "promotions", action = NavAction.EXPORT)
    public ResponseEntity<byte[]> bilanCsv(@PathVariable Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "bilan-promotion-" + id + ".csv");
        return ResponseEntity.ok().headers(headers).body(bilanPromotionService.exporter(id));
    }

    /** Une annulation est définitive : elle demande le droit de suppression. */
    @PutMapping("/{id}/annuler")
    @RequiresNavAccess(value = "promotions", action = NavAction.DELETE)
    public ResponseEntity<PromotionDTO> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.annuler(id));
    }
}
