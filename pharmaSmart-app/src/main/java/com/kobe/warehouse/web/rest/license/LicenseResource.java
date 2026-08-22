package com.kobe.warehouse.web.rest.license;

import com.kobe.warehouse.aop.license.LicenseExempt;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.license.LicenseInfo;
import com.kobe.warehouse.license.LicenseProperties;
import com.kobe.warehouse.repository.LicenseAuditRepository;
import com.kobe.warehouse.service.license.LicenseActivationException;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.service.license.dto.LicenseAuditDTO;
import com.kobe.warehouse.service.license.dto.PublisherContactsDTO;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.io.IOException;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API de consultation et d'activation de la licence.
 *
 * <p>Volontairement exposée sous {@code /api/license} et <strong>non</strong> sous
 * {@code /api/admin/} : la configuration de sécurité réserve ce dernier préfixe aux
 * administrateurs, alors que {@code GET /status} doit rester lisible par un caissier — c'est lui
 * qui voit la bannière et le décompte avant échéance.
 *
 * <p>Les opérations sensibles exigent le privilège {@code pr-gere-licence} plutôt que
 * {@code ROLE_ADMIN} : dans une officine, le renouvellement est souvent traité par le pharmacien
 * titulaire ou un gérant, sans qu'on veuille lui ouvrir toute l'administration. Le privilège est un
 * item de navigation de type {@code ACTION}, fusionné dans les authorities à l'authentification
 * (cf. {@code DomainUserDetailsService}), donc attribuable à n'importe quel rôle depuis l'écran
 * « Rôles &amp; Autorisations ». Il est accordé d'office à {@code ROLE_ADMIN} par la migration
 * {@code V1.8.8}, qui préserve le comportement antérieur.
 */
@RestController
@RequestMapping("/api/license")
public class LicenseResource {

    private final LicenseService licenseService;
    private final LicenseAuditRepository licenseAuditRepository;
    private final LicenseProperties licenseProperties;

    public LicenseResource(LicenseService licenseService,
        LicenseAuditRepository licenseAuditRepository,
        LicenseProperties licenseProperties) {
        this.licenseService = licenseService;
        this.licenseAuditRepository = licenseAuditRepository;
        this.licenseProperties = licenseProperties;
    }

    /**
     * Statut courant : alimente le toast de connexion (B2), la bannière (B3) et les gardes de
     * route.
     */
    @GetMapping("/status")
    public ResponseEntity<LicenseInfo> status() {
        return ResponseEntity.ok(licenseService.currentStatus());
    }

    /**
     * Catalogue des modules : code, intitulé et caractère optionnel.
     *
     * <p>Le navigateur ne doit pas tenir sa propre copie de l'enum {@code Feature}. Celle qui
     * existait avait déjà divergé — elle omettait les quatre modules optionnels, si bien que
     * {@code hasFeature()} les considérait tous comme accordés et qu'aucune garde de route ne
     * bloquait plus rien.
     *
     * <p>Ni privilège ni licence valide ne sont exigés : l'écran de licence affiche aussi les
     * modules <strong>non</strong> souscrits, pour que le client voie ce qu'il peut acheter.
     */
    @GetMapping("/features")
    public ResponseEntity<List<FeatureDTO>> features() {
        return ResponseEntity.ok(
            Arrays.stream(Feature.values())
                .map(feature -> new FeatureDTO(feature.name(), feature.getLibelle(), feature.isOptional()))
                .toList()
        );
    }

    /** Un module du catalogue, tel que l'écran de licence l'affiche. */
    public record FeatureDTO(String code, String label, boolean optional) {}

    /**
     * Coordonnées de l'éditeur, telles que configurées dans {@code application.yml}.
     *
     * <p>Ni privilège ni licence valide ne sont exigés : c'est précisément à la première
     * installation, sans licence, que le client a besoin de savoir à qui s'adresser.
     */
    @GetMapping("/publisher")
    public ResponseEntity<PublisherContactsDTO> publisher() {
        return ResponseEntity.ok(PublisherContactsDTO.of(licenseProperties.getPublisher()));
    }

    /**
     * Dépôt d'un fichier {@code .lic}.
     *
     * <p>{@link LicenseExempt} est ici <strong>vital</strong> : sans exemption, une licence
     * expirée
     * empêcherait son propre remplacement et l'officine resterait bloquée définitivement.
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('pr-gere-licence')")
    @LicenseExempt("Renouvellement : cet endpoint doit rester accessible licence expirée ou absente")
    public ResponseEntity<LicenseInfo> activate(@RequestParam("file") MultipartFile file)
        throws IOException {
        if (file == null || file.isEmpty()) {
            throw new LicenseActivationException("Aucun fichier de licence n'a été déposé.",
                "license.activate.empty");
        }
        return ResponseEntity.ok(
            licenseService.activate(file.getBytes(), file.getOriginalFilename()));
    }

    /**
     * Empreinte matérielle du poste serveur.
     *
     * <p>Elle n'est <strong>plus nécessaire à l'émission courante</strong> : depuis le 12/08/2026,
     * une licence {@code SUBSCRIPTION} se lie par défaut à la seule raison sociale de l'officine et
     * s'émet donc avant toute installation. L'empreinte ne sert qu'aux licences que l'éditeur décide
     * de réserver à un poste précis ({@code MAGASIN_AND_HARDWARE}) : il la réclame alors au client,
     * qui la relève dans l'écran « Gérer ma licence ».
     *
     * <p>Une divergence ultérieure — changement de serveur, remplacement de carte réseau — n'est
     * bloquante qu'au-delà du délai de régularisation
     * ({@code fingerprint-mismatch-tolerance-days}), le temps de faire réémettre le fichier.
     */
    @GetMapping("/fingerprint")
    @PreAuthorize("hasAuthority('pr-gere-licence')")
    public ResponseEntity<Map<String, String>> fingerprint() {
        return ResponseEntity.ok(Map.of("fingerprint", licenseService.hardwareFingerprint()));
    }

    /**
     * Historique des événements de licence : activations, rejets, écritures bloquées.
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('pr-gere-licence')")
    public ResponseEntity<java.util.List<LicenseAuditDTO>> audit(Pageable pageable) {
        Page<LicenseAuditDTO> page = licenseAuditRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(LicenseAuditDTO::new);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
