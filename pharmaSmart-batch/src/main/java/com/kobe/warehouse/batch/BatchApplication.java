package com.kobe.warehouse.batch;

import com.kobe.warehouse.service.EtatProduitServiceImpl;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionServiceIml;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableJpaRepositories({"com.kobe.warehouse.repository"})
@EntityScan({"com.kobe.warehouse.domain"})
@Import({StorageService.class, UserService.class, EtatProduitServiceImpl.class, ReferenceService.class, InventoryTransactionServiceIml.class})
@SpringBootApplication(
    scanBasePackages = {
        "com.kobe.warehouse.domain",
        "com.kobe.warehouse.service.criteria",
        "com.kobe.warehouse.service.id_generator",
        "com.kobe.warehouse.repository",
        "com.kobe.warehouse.service.scheduler",
        "com.kobe.warehouse.service.settings",
        "com.kobe.warehouse.service.stock",
        "com.kobe.warehouse.service.classification",
        "com.kobe.warehouse.config",
        "com.kobe.warehouse.batch"
    },
    excludeName = {
        "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration",
        "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
        "org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration"
    }
)
@EnableScheduling
@EnableAsync
public class BatchApplication {

    static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}
