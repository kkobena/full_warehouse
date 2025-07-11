package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.MaxAndMinDate;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class ImportationVenteService {

    private final Logger log = LoggerFactory.getLogger(ImportationVenteService.class);
    private final TransactionTemplate transactionTemplate;
    private final RestTemplate restTemplate;
    private final SaleService saleService;
    private final SalesLineService salesLineService;
    private final CashSaleRepository cashSaleRepository;
    private final SalesLineRepository salesLineRepository;
    private final SalePaymentRepository paymentRepository;

    @Value("${legacy-url}")
    private String legacyUrl;

    public ImportationVenteService(
        TransactionTemplate transactionTemplate,
        RestTemplateBuilder restTemplateBuilder,
        SaleService saleService,
        CashSaleRepository cashSaleRepository,
        SalesLineRepository salesLineRepository,
        SalePaymentRepository paymentRepository,
        SalesLineService salesLineService
    ) {
        this.transactionTemplate = transactionTemplate;
        restTemplate = restTemplateBuilder.build();
        this.saleService = saleService;
        this.cashSaleRepository = cashSaleRepository;
        this.salesLineRepository = salesLineRepository;
        this.paymentRepository = paymentRepository;
        this.salesLineService = salesLineService;
    }

    private MaxAndMinDate findMaxAndMinDate() {
        try {
            ResponseEntity<MaxAndMinDate> maxAndMinDateResponseEntity = restTemplate.getForEntity(
                legacyUrl + "/api/v1//whareouse-maxmin",
                MaxAndMinDate.class
            );
            return maxAndMinDateResponseEntity.getBody();
        } catch (Exception e) {
            log.debug(" {}", e);
            return null;
        }
    }

    private List<SaleDTO> findFromLegacy(String date) {
        log.info("date {} ", date);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(legacyUrl + "/api/v1//whareouse-vno")
                .queryParam("dtStart", date)
                .queryParam("dtEnd", date);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            HttpEntity<SaleDTO[]> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, SaleDTO[].class);

            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.debug(" {}", e);
            return null;
        }
    }

    // @PostConstruct
    public void init() {
        try {
            updateVNOFromLegacy();
        } catch (Throwable e) {
            log.debug(" {}", e);
        }
    }

    // @Scheduled(fixedRate = 1800000)
    public void updateVNOFromLegacy() throws IOException {
        Runnable runnableTask = () -> {
            try {
                LocalDateTime start = LocalDateTime.now();
                log.info("start a", start);
                MaxAndMinDate maxAndMinDate = findMaxAndMinDate();
                if (maxAndMinDate != null) {
                    LocalDate min = LocalDate.parse(maxAndMinDate.getMinDate());
                    LocalDate max = LocalDate.parse(maxAndMinDate.getMaxDate());
                    long interval = ChronoUnit.DAYS.between(min, max);
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                    do {
                        List<SaleDTO> datas = findFromLegacy(min.toString());
                        min = min.plusDays(1);
                        for (SaleDTO d : datas) {
                            transactionTemplate.execute(
                                new TransactionCallbackWithoutResult() {
                                    @Override
                                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                                        try {
                                            CashSaleDTO dto = (CashSaleDTO) d;
                                            CashSale cashSale = saleService.fromDTOOldCashSale(dto);
                                            cashSaleRepository.save(cashSale);
                                            for (SaleLineDTO i : dto.getSalesLines()) {
                                                SalesLine item = salesLineService.buildSaleLineFromDTO(i);
                                                item.setSales(cashSale);
                                                salesLineRepository.save(item);
                                            }
                                            if (!dto.getPayments().isEmpty()) {
                                                SalePayment payment = saleService.buildPaymentFromDTO(dto.getPayments().get(0), cashSale);
                                                paymentRepository.save(payment);
                                            }
                                        } catch (Exception e) {
                                            log.debug(" {}", e);
                                        }
                                    }
                                }
                            );
                        }
                        interval--;
                        TimeUnit.MILLISECONDS.sleep(300);
                    } while (interval >= 0);
                }
                LocalDateTime end = LocalDateTime.now();
                log.info("temps passe ==========>>>>  {}", ChronoUnit.MINUTES.between(start, end));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        runnableTask.run();
    }
}
