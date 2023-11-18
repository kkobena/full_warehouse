package com.kobe.warehouse.service.report.jasper;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Magasin;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class JasperReportService {

  private final Logger log = LoggerFactory.getLogger(JasperReportService.class);
  private final Path fileStorageLocation;
  private final FileStorageProperties fileStorageProperties;
  private final MagasinRepository magasinRepository;
  private final UserRepository userRepository;
  private final Map<String, Object> parameters = new HashMap<>();
  private Magasin magasin;

  public JasperReportService(
      FileStorageProperties fileStorageProperties,
      MagasinRepository magasinRepository,
      UserRepository userRepository) {
    this.magasinRepository = magasinRepository;
    this.userRepository = userRepository;
    this.fileStorageProperties = fileStorageProperties;
    this.fileStorageLocation =
        Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
    setMagasin();
  }

  public void addParametter(String key, Object value) {
    parameters.put(key, value);
  }

  private void setMagasin() {
    try {
      magasin = magasinRepository.findAll().get(0);
    } catch (Exception e) {
      log.debug("{}", e);
    }
  }

  public void buildTitle(String title) {
    parameters.put("title", title);
  }

  public void buildMagasinInfo() {
    if (magasin != null) {
      parameters.put("raisonSocial", magasin.getName().toUpperCase());
      parameters.put("phone", magasin.getPhone());
      parameters.put("address", magasin.getAddress());
      parameters.put("registre", magasin.getRegistre());
    }
  }

  public void buildCustomerInfo(Customer customer) {
    if (customer != null) {
      parameters.put("firstName", customer.getFirstName());
      parameters.put("lastName", customer.getLastName());
      parameters.put("customerPhone", customer.getPhone());
      parameters.put("fullName", customer.getFirstName() + " " + customer.getLastName());
    }
  }

  private JasperReport getReport(String reportName) throws Exception {
    try (InputStream resource =
        new FileInputStream(
            fileStorageLocation.resolve(reportName + ".jasper").normalize().toFile())) {

      return (JasperReport) JRLoader.loadObject(resource);
    } catch (FileNotFoundException ex) {
      return compileReport(reportName);
      // throw new FileNotFoundException(String.format("Le %s fichier n'existe", reportName));
    }
  }

  public String buildReportToPDF(String reportName, List<?> datas) {

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

    return SecurityUtils.getCurrentUserLogin()
        .flatMap(userRepository::findOneByLogin)
        .orElseThrow();
  }

  private String convertNumberToLetters(long amount) {
    RuleBasedNumberFormat formatter =
        new RuleBasedNumberFormat(Locale.FRANCE, RuleBasedNumberFormat.SPELLOUT);
    return formatter.format(amount);
  }

  public String buildReportToPDF(String reportName) {
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

    } catch (Exception ex) {
      log.debug("{}", ex);
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
      log.debug("{}", e);
    }

    return destFilePath;
  }

  public void builderFooter() {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.isNotEmpty(magasin.getRegistre())) {
      builder.append("RC N° ").append(magasin.getRegistre());
    }
    if (StringUtils.isNotEmpty(magasin.getCompteContribuable())) {
      builder.append(" - CC N° ").append(magasin.getCompteContribuable());
    }
    if (StringUtils.isNotEmpty(magasin.getNumComptable())) {
      builder.append(" - CPT N°: ").append(magasin.getNumComptable());
    }
    if (StringUtils.isNotEmpty(magasin.getPhone())) {
      builder.append("- Tel: ").append(magasin.getPhone());
    }
    if (StringUtils.isNotEmpty(magasin.getAddress())) {
      builder.append("- Adr: ").append(magasin.getAddress());
    }
    parameters.put("footer", builder.toString());
  }

  public Resource getResource(String path) throws MalformedURLException {
    return new UrlResource(Paths.get(path).toUri());
  }

  public Resource printToPdf(String templateName, List<?> datas, Customer customer)
      throws MalformedURLException {
    buildMagasinInfo();
    builderFooter();
    buildCustomerInfo(customer);
    return new UrlResource(Paths.get(buildReportToPDF(templateName, datas)).toUri());
  }

  public JasperReport compileReport(String reportName) throws Exception {
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
      in2 =
          new FileInputStream(
              jasperFile); // ReportUtil.class.getResourceAsStream(reportPath + reportName +
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
}
