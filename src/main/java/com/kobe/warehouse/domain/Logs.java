package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TransactionType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "logs", indexes = {
    @Index(columnList = "transaction_type", name = "transaction_type_index"),
    @Index(columnList = "created_at", name = "createdAt_index"),
    @Index(columnList = "indentity_key", name = "indentityKey_index")
})
public class Logs implements Serializable {
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
    private Instant createdAt = Instant.now();
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

    public Logs setId(Long id) {
        this.id = id;
        return this;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Logs setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Logs setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getComments() {
        return comments;
    }

    public Logs setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Logs setUser(User user) {
        this.user = user;
        return this;
    }

    public String getIndentityKey() {
        return indentityKey;
    }

    public Logs setIndentityKey(String indentityKey) {
        this.indentityKey = indentityKey;
        return this;
    }
}
