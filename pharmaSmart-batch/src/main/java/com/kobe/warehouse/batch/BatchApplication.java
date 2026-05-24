package com.kobe.warehouse.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {
        "com.kobe.warehouse.domain",
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
