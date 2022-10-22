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
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {
  @Autowired ResourceLoader resourceLoader;
  @Autowired private MagasinRepository magasinRepository;
  @Autowired private UserRepository userRepository;
  private final Path fileStorageLocation;
  private final FileStorageProperties fileStorageProperties;

  public ReportService(FileStorageProperties fileStorageProperties) {
    this.fileStorageProperties = fileStorageProperties;
    this.fileStorageLocation =
        Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
  }

  public Map<String, Object> buildMagasinInfo() {
    Magasin magsin = magasinRepository.findAll().get(0);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("raisonSocial", magsin.getName());
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

  private JasperReport getReport(String reportName)
      throws JRException, FileNotFoundException, Exception {
    // Resource resource2 = resourceLoader.getResource("classpath:/static/content/reports/");
    try (InputStream resource =
        new FileInputStream(
            fileStorageLocation.resolve(reportName + ".jasper").normalize().toFile())) {

      return (JasperReport) JRLoader.loadObject(resource);
    } catch (FileNotFoundException ex) {
      ex.printStackTrace(System.err);
      throw new FileNotFoundException(String.format("Le %s fichier n'existe", reportName));
    }
  }

  public String buildReportToPDF(Map<String, Object> parameters, String reportName, List<?> datas)
      throws IOException {

    String destFilePath =
        this.fileStorageLocation
            .resolve(
                reportName
                    + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                    + ".pdf")
            .toFile()
            .getAbsolutePath();
    ;
    try {
      JasperReport jasperReport = getReport(reportName);
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datas);
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      JasperExportManager.exportReportToPdfFile(jasperPrint, destFilePath);

    } catch (JRException e) {
      e.printStackTrace(System.err);

    } catch (Exception ex) {
      ex.printStackTrace(System.err);
    }
    return destFilePath;
  }

  private User getUser() {
    Optional<User> user =
        SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
    return user.orElseGet(null);
  }

  private String convertionChiffeLettres(Integer amount) {
    RuleBasedNumberFormat formatter =
        new RuleBasedNumberFormat(Locale.FRANCE, RuleBasedNumberFormat.SPELLOUT);
    String result = formatter.format(amount);
    return result;
  }

  public String buildReportToPDF(Map<String, Object> parameters, String reportName) {
    String destFilePath =
        this.fileStorageLocation
            .resolve(
                reportName
                    + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                    + ".pdf")
            .toFile()
            .getAbsolutePath();
    try {

      String jasperPrint =
          JasperFillManager.fillReportToFile(
              fileStorageProperties.getReportsDir() + "/" + reportName + ".jasper",
              parameters,
              new JREmptyDataSource());
      JasperExportManager.exportReportToPdfFile(jasperPrint, destFilePath);

    } catch (JRException e) {
      e.printStackTrace(System.err);

    } catch (Exception ex) {
      ex.printStackTrace(System.err);
    }
    return destFilePath;
  }

  public String buildInvoiceToPDF(String reportName, String content) {
    String destFilePath =
        this.fileStorageLocation
            .resolve(
                reportName
                    + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                    + ".pdf")
            .toFile()
            .getAbsolutePath();
    try {

      OutputStream outputStream = new FileOutputStream(destFilePath);
      ITextRenderer renderer = new ITextRenderer();
      SharedContext sharedContext = renderer.getSharedContext();
      sharedContext.setReplacedElementFactory(new ImageReplacedElementFactory());
      sharedContext.getTextRenderer().setSmoothingThreshold(0);
      // sharedContext.setPrint();
      renderer.setDocumentFromString(content);
      renderer.layout();
      renderer.createPDF(outputStream);
      outputStream.close();
    } catch (IOException | DocumentException e) {
      e.printStackTrace(System.err);
    }

    return destFilePath;
  }
}
