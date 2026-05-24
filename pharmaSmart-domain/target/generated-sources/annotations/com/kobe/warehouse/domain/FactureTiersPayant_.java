package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.service.fne.model.FneResponse;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.FactureTiersPayant}
 **/
@StaticMetamodel(FactureTiersPayant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class FactureTiersPayant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #invoiceDate
	 **/
	public static final String INVOICE_DATE = "invoiceDate";
	
	/**
	 * @see #numFacture
	 **/
	public static final String NUM_FACTURE = "numFacture";
	
	/**
	 * @see #remiseForfetaire
	 **/
	public static final String REMISE_FORFETAIRE = "remiseForfetaire";
	
	/**
	 * @see #tiersPayant
	 **/
	public static final String TIERS_PAYANT = "tiersPayant";
	
	/**
	 * @see #factureProvisoire
	 **/
	public static final String FACTURE_PROVISOIRE = "factureProvisoire";
	
	/**
	 * @see #origineGeneration
	 **/
	public static final String ORIGINE_GENERATION = "origineGeneration";
	
	/**
	 * @see #debutPeriode
	 **/
	public static final String DEBUT_PERIODE = "debutPeriode";
	
	/**
	 * @see #finPeriode
	 **/
	public static final String FIN_PERIODE = "finPeriode";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #montantRegle
	 **/
	public static final String MONTANT_REGLE = "montantRegle";
	
	/**
	 * @see #montantTtc
	 **/
	public static final String MONTANT_TTC = "montantTtc";
	
	/**
	 * @see #montantTva
	 **/
	public static final String MONTANT_TVA = "montantTva";
	
	/**
	 * @see #montantNet
	 **/
	public static final String MONTANT_NET = "montantNet";
	
	/**
	 * @see #montantHt
	 **/
	public static final String MONTANT_HT = "montantHt";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #groupeTiersPayant
	 **/
	public static final String GROUPE_TIERS_PAYANT = "groupeTiersPayant";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #generationCode
	 **/
	public static final String GENERATION_CODE = "generationCode";
	
	/**
	 * @see #factureTiersPayants
	 **/
	public static final String FACTURE_TIERS_PAYANTS = "factureTiersPayants";
	
	/**
	 * @see #invoicePayments
	 **/
	public static final String INVOICE_PAYMENTS = "invoicePayments";
	
	/**
	 * @see #groupeFactureTiersPayant
	 **/
	public static final String GROUPE_FACTURE_TIERS_PAYANT = "groupeFactureTiersPayant";
	
	/**
	 * @see #facturesDetails
	 **/
	public static final String FACTURES_DETAILS = "facturesDetails";
	
	/**
	 * @see #repartitions
	 **/
	public static final String REPARTITIONS = "repartitions";
	
	/**
	 * @see #fneResponse
	 **/
	public static final String FNE_RESPONSE = "fneResponse";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.FactureTiersPayant}
	 **/
	public static volatile EntityType<FactureTiersPayant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#id}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#invoiceDate}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, LocalDate> invoiceDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#numFacture}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, String> numFacture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#remiseForfetaire}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, Integer> remiseForfetaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#tiersPayant}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, TiersPayant> tiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#factureProvisoire}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, Boolean> factureProvisoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#origineGeneration}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, OrigineGeneration> origineGeneration;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#debutPeriode}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, LocalDate> debutPeriode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#finPeriode}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, LocalDate> finPeriode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#created}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#updated}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#montantRegle}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, Integer> montantRegle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#montantTtc}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, BigDecimal> montantTtc;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#montantTva}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, BigDecimal> montantTva;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#montantNet}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, BigDecimal> montantNet;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#montantHt}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, BigDecimal> montantHt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#statut}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, InvoiceStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#groupeTiersPayant}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, GroupeTiersPayant> groupeTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#user}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#generationCode}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, Integer> generationCode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#factureTiersPayants}
	 **/
	public static volatile ListAttribute<FactureTiersPayant, FactureTiersPayant> factureTiersPayants;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#invoicePayments}
	 **/
	public static volatile ListAttribute<FactureTiersPayant, InvoicePayment> invoicePayments;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#groupeFactureTiersPayant}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, FactureTiersPayant> groupeFactureTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#facturesDetails}
	 **/
	public static volatile ListAttribute<FactureTiersPayant, ThirdPartySaleLine> facturesDetails;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#repartitions}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, List> repartitions;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FactureTiersPayant#fneResponse}
	 **/
	public static volatile SingularAttribute<FactureTiersPayant, FneResponse> fneResponse;

}

