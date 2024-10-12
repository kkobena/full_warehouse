package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "logs",
    indexes = {
        @Index(columnList = "transaction_type", name = "transaction_type_index"),
        @Index(columnList = "created_at", name = "createdAt_index"),
        @Index(columnList = "indentity_key", name = "indentityKey_index"),
    }
)
public class Logs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name = "comments", nullable = false)
    private String comments;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @NotNull
    @Column(name = "indentity_key", nullable = false)
    private String indentityKey;

    @Column(name = "old_object")
    private String oldObject;

    @Column(name = "new_object")
    private String newObject;

    public Long getId() {
        return id;
    }

    public Logs setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull TransactionType getTransactionType() {
        return transactionType;
    }

    public Logs setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Logs setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public @NotNull String getComments() {
        return comments;
    }

    public Logs setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public @NotNull User getUser() {
        return user;
    }

    public Logs setUser(User user) {
        this.user = user;
        return this;
    }

    public @NotNull String getIndentityKey() {
        return indentityKey;
    }

    public Logs setIndentityKey(String indentityKey) {
        this.indentityKey = indentityKey;
        return this;
    }

    public String getOldObject() {
        return oldObject;
    }

    public Logs setOldObject(String oldObject) {
        this.oldObject = oldObject;
        return this;
    }

    public String getNewObject() {
        return newObject;
    }

    public Logs setNewObject(String newObject) {
        this.newObject = newObject;
        return this;
    }
}
