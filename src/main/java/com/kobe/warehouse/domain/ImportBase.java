package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_base")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ImportBase {

    @Id
    private Long id;

    @NotNull
    @Column(name = "import_id", nullable = false, unique = true)
    private String importId;

    private LocalDateTime mvDate = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public LocalDateTime getMvDate() {
        return mvDate;
    }

    public void setMvDate(LocalDateTime mvDate) {
        this.mvDate = mvDate;
    }
}
