package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class RetourClientPdfServiceTest {

    @Mock private RetourClientService retourClientService;
    @Mock private StorageService storageService;
    @Mock private SpringTemplateEngine templateEngine;
    @Mock private FileStorageProperties fileStorageProperties;

    private TestableRetourClientPdfService service;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        magasin = new Magasin();
        magasin.setName("Pharmacie Test");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        lenient().when(storageService.getUser()).thenReturn(user);
        service = new TestableRetourClientPdfService(
            retourClientService, storageService, templateEngine, fileStorageProperties
        );
    }

    @Test
    void exposesReceiptTemplateContract() {
        when(templateEngine.process(eq("retour-client/receipt/main"), any(Context.class)))
            .thenReturn("<html>retour</html>");

        assertEquals(0, service.getMaxiRowCount());
        assertEquals(0, service.getItems().size());
        assertEquals("retour_client", service.getGenerateFileName());
        assertEquals("<html>retour</html>", service.getTemplateAsHtml());

        Context context = new Context();
        service.getParameters().put("custom", "value");
        assertEquals("<html>retour</html>", service.getTemplateAsHtml(context));
        assertEquals("value", context.getVariable("custom"));
    }

    @Test
    void generatesPdfWithFreshReturnAndCommonParameters() {
        RetourClientDTO retour = new RetourClientDTO(
            17, "RET-17", null, null, null, null, 0, 0,
            null, null, null, null, java.util.List.of(), false, null
        );
        byte[] expected = {1, 2, 3};
        service.pdf = expected;
        service.getParameters().put("obsolete", true);
        when(retourClientService.findById(17)).thenReturn(retour);

        byte[] result = service.generatePdf(17);

        assertArrayEquals(expected, result);
        Map<String, Object> parameters = service.getParameters();
        assertEquals(4, parameters.size());
        assertSame(retour, parameters.get("retour"));
        assertEquals("80mm 297mm", parameters.get(Constant.SIZE));
        assertSame(magasin, parameters.get(Constant.MAGASIN));
        verify(retourClientService).findById(17);
        assertEquals(1, service.exportCalls);
    }

    private static final class TestableRetourClientPdfService extends RetourClientPdfService {
        private byte[] pdf;
        private int exportCalls;

        private TestableRetourClientPdfService(
            RetourClientService retourClientService,
            StorageService storageService,
            SpringTemplateEngine templateEngine,
            FileStorageProperties fileStorageProperties
        ) {
            super(retourClientService, storageService, templateEngine, fileStorageProperties);
        }

        @Override
        public byte[] exportReportToPdf() {
            exportCalls++;
            return pdf;
        }
    }
}


