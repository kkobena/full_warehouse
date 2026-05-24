package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelDto;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/edition-factures")
public class RecapitulatifFactureResource {

    private final RecapitulatifMensuelService recapitulatifMensuelService;

    public RecapitulatifFactureResource(RecapitulatifMensuelService recapitulatifMensuelService) {
        this.recapitulatifMensuelService = recapitulatifMensuelService;
    }

    @GetMapping("/recapitulatif")
    public ResponseEntity<List<RecapitulatifMensuelDto>> getRecapitulatif(
        @RequestParam int annee,
        @RequestParam int mois,
        @RequestParam(required = false) List<Integer> tiersPayantIds,
        @RequestParam(required = false) List<Integer> groupIds,
        @RequestParam(required = false) TypeFacture typeFacture,
        Pageable pageable
    ) {
        RecapitulatifMensuelParams params = new RecapitulatifMensuelParams(annee, mois, tiersPayantIds, groupIds, typeFacture);
        Page<RecapitulatifMensuelDto> page = recapitulatifMensuelService.getRecapitulatif(params, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/recapitulatif/pdf")
    public ResponseEntity<byte[]> exportPdf(
        @RequestParam int annee,
        @RequestParam int mois,
        @RequestParam(required = false) List<Integer> tiersPayantIds,
        @RequestParam(required = false) List<Integer> groupIds,
        @RequestParam(required = false) TypeFacture typeFacture
    ) {
        RecapitulatifMensuelParams params = new RecapitulatifMensuelParams(annee, mois, tiersPayantIds, groupIds, typeFacture);
        byte[] pdf = recapitulatifMensuelService.exportPdf(params);
        return Utils.printPDF(pdf, "recapitulatif_" + annee + "_" + mois + ".pdf");
    }

    @GetMapping("/recapitulatif/excel")
    public ResponseEntity<byte[]> exportExcel(
        @RequestParam int annee,
        @RequestParam int mois,
        @RequestParam(required = false) List<Integer> tiersPayantIds,
        @RequestParam(required = false) List<Integer> groupIds,
        @RequestParam(required = false) TypeFacture typeFacture
    ) {
        RecapitulatifMensuelParams params = new RecapitulatifMensuelParams(annee, mois, tiersPayantIds, groupIds, typeFacture);
        byte[] excel = recapitulatifMensuelService.exportExcel(params);
        return Utils.exportExcel(excel, "recapitulatif_" + annee + "_" + mois + ".xlsx");
    }
}
