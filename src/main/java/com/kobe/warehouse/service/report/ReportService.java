package com.kobe.warehouse.service.report;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.pdf.ImageReplacedElementFactory;
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import com.lowagie.text.DocumentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class ReportService {

    private final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;

    private final MagasinRepository magasinRepository;

    private final UserRepository userRepository;

    public ReportService(FileStorageProperties fileStorageProperties, MagasinRepository magasinRepository, UserRepository userRepository) {
        this.magasinRepository = magasinRepository;
        this.userRepository = userRepository;
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();
        log.info(
            String.format(
                "report directory %s %s",
                this.fileStorageLocation.getParent().toString(),
                this.fileStorageLocation.getRoot().toString()
            )
        );
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Map<String, Object> buildMagasinInfo() {
        Magasin magsin = magasinRepository.findAll().getFirst();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("raisonSocial", magsin.getName().toUpperCase());
        parameters.put("phone", magsin.getPhone());
        parameters.put("address", magsin.getAddress());
        parameters.put("registre", magsin.getRegistre());
        return parameters;
    }

    public void buildSaleInfo(Map<String, Object> parameters, Sales sale) {
        parameters.put("saleAmount", sale.getSalesAmount());
        parameters.put("letteAmount", convertionChiffeLettres(sale.getSalesAmount()));
        parameters.put("numberTransaction", sale.getNumberTransaction());
    }

    public void buildCustomerInfo(Map<String, Object> parameters, Customer customer) {
        parameters.put("firstName", customer.getFirstName());
        parameters.put("lastName", customer.getLastName());
        parameters.put("customerPhone", customer.getPhone());
        parameters.put("fullName", customer.getFirstName() + " " + customer.getLastName());
    }

    private JasperReport getReport(String reportName) throws Exception {
        try (InputStream resource = new FileInputStream(fileStorageLocation.resolve(reportName + ".jasper").normalize().toFile())) {
            return (JasperReport) JRLoader.loadObject(resource);
        } catch (FileNotFoundException ex) {
            return compileReport(reportName);
        }
    }

    public String buildReportToPDF(Map<String, Object> parameters, String reportName, List<?> datas) {
        String destFilePath =
            this.fileStorageLocation.resolve(
                    reportName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".pdf"
                )
                .toFile()
                .getAbsolutePath();
        log.info("destFilePath {}", destFilePath);
        try {
            JasperReport jasperReport = getReport(reportName);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datas);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, destFilePath);
        } catch (Exception ex) {
            log.debug("{0}", ex);
        }
        return destFilePath;
    }

    private User getUser() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).orElseThrow();
    }

    private String convertionChiffeLettres(Integer amount) {
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.FRANCE, RuleBasedNumberFormat.SPELLOUT);
        return formatter.format(amount);
    }

    public String buildReportToPDF(Map<String, Object> parameters, String reportName) {
        String destFilePath =
            this.fileStorageLocation.resolve(
                    reportName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".pdf"
                )
                .toFile()
                .getAbsolutePath();
        try {
            String jasperPrint = JasperFillManager.fillReportToFile(
                fileStorageProperties.getReportsDir() + "/" + reportName + ".jasper",
                parameters,
                new JREmptyDataSource()
            );
            JasperExportManager.exportReportToPdfFile(jasperPrint, destFilePath);
        } catch (Exception ex) {
            log.debug("{0}", ex);
        }
        return destFilePath;
    }

    public String buildInvoiceToPDF(String reportName, String content) {
        String destFilePath =
            this.fileStorageLocation.resolve(
                    reportName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".pdf"
                )
                .toFile()
                .getAbsolutePath();
        log.info(" buildInvoiceToPDF destFilePath {}", destFilePath);
        try {
            try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
                ITextRenderer renderer = new ITextRenderer();
                SharedContext sharedContext = renderer.getSharedContext();
                sharedContext.setReplacedElementFactory(new ImageReplacedElementFactory());
                sharedContext.getTextRenderer().setSmoothingThreshold(0);

                renderer.setDocumentFromString(content);
                renderer.layout();
                renderer.createPDF(outputStream);
            }
        } catch (IOException | DocumentException e) {
            log.debug("{0}", e);
        }

        return destFilePath;
    }

    private JasperReport compileReport(String reportName) throws Exception {
        InputStream in = null;
        InputStream in2 = null;
        FileOutputStream out = null;
        File jasperFile = null;

        try {
            // File jrxmlFile = new File(ReportUtil.class.getResource(reportPath + reportName +
            // ".jrxml").getFile());
            File jrxmlFile = fileStorageLocation.resolve(reportName + ".jrxml").normalize().toFile();
            File dir = jrxmlFile.getParentFile();
            jasperFile = new File(dir, reportName + ".jasper");
            in = new FileInputStream(jrxmlFile);
            // in = ReportUtil.class.getResourceAsStream(reportPath + reportName + ".jrxml");
            out = new FileOutputStream(jasperFile);
            JasperCompileManager.compileReportToStream(in, out);
            in2 = new FileInputStream(jasperFile); // ReportUtil.class.getResourceAsStream(reportPath + reportName +
            // ".jasper");
            return (JasperReport) JRLoader.loadObject(in2);
        } catch (FileNotFoundException | JRException e) {
            if (jasperFile != null) {
                jasperFile.delete();
            }

            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (in2 != null) {
                in2.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public Resource getResource(String path) throws MalformedURLException {
        return new UrlResource(Paths.get(path).toUri());
    }
}
