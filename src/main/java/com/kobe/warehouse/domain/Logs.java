package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TransactionType;

import javax.persistence.*;
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
    private Long indentityKey;

    public Long getId() {
        return id;
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

    public Long getIndentityKey() {
        return indentityKey;
    }

    public Logs setIndentityKey(Long indentityKey) {
        this.indentityKey = indentityKey;
        return this;
    }
}
