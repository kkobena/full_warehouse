package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Commande}
 **/
@StaticMetamodel(Commande.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Commande_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #orderDate
	 **/
	public static final String ORDER_DATE = "orderDate";
	
	/**
	 * @see #orderReference
	 **/
	public static final String ORDER_REFERENCE = "orderReference";
	
	/**
	 * @see #receiptReference
	 **/
	public static final String RECEIPT_REFERENCE = "receiptReference";
	
	/**
	 * @see #receiptDate
	 **/
	public static final String RECEIPT_DATE = "receiptDate";
	
	/**
	 * @see #discountAmount
	 **/
	public static final String DISCOUNT_AMOUNT = "discountAmount";
	
	/**
	 * @see #orderAmount
	 **/
	public static final String ORDER_AMOUNT = "orderAmount";
	
	/**
	 * @see #finalAmount
	 **/
	public static final String FINAL_AMOUNT = "finalAmount";
	
	/**
	 * @see #grossAmount
	 **/
	public static final String GROSS_AMOUNT = "grossAmount";
	
	/**
	 * @see #htAmount
	 **/
	public static final String HT_AMOUNT = "htAmount";
	
	/**
	 * @see #taxAmount
	 **/
	public static final String TAX_AMOUNT = "taxAmount";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #orderStatus
	 **/
	public static final String ORDER_STATUS = "orderStatus";
	
	/**
	 * @see #orderLines
	 **/
	public static final String ORDER_LINES = "orderLines";
	
	/**
	 * @see #paimentStatut
	 **/
	public static final String PAIMENT_STATUT = "paimentStatut";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #motifBed
	 **/
	public static final String MOTIF_BED = "motifBed";
	
	/**
	 * @see #commentaireBed
	 **/
	public static final String COMMENTAIRE_BED = "commentaireBed";
	
	/**
	 * @see #hasBeenSubmittedToPharmaML
	 **/
	public static final String HAS_BEEN_SUBMITTED_TO_PHARMA_ML = "hasBeenSubmittedToPharmaML";
	
	/**
	 * @see #originalCommandeId
	 **/
	public static final String ORIGINAL_COMMANDE_ID = "originalCommandeId";
	
	/**
	 * @see #reliquatDeCommandeId
	 **/
	public static final String RELIQUAT_DE_COMMANDE_ID = "reliquatDeCommandeId";
	
	/**
	 * @see #reconciliation
	 **/
	public static final String RECONCILIATION = "reconciliation";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Commande}
	 **/
	public static volatile EntityType<Commande> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#id}
	 **/
	public static volatile SingularAttribute<Commande, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#orderDate}
	 **/
	public static volatile SingularAttribute<Commande, LocalDate> orderDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#orderReference}
	 **/
	public static volatile SingularAttribute<Commande, String> orderReference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#receiptReference}
	 **/
	public static volatile SingularAttribute<Commande, String> receiptReference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#receiptDate}
	 **/
	public static volatile SingularAttribute<Commande, LocalDate> receiptDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#discountAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> discountAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#orderAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> orderAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#finalAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> finalAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#grossAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> grossAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#htAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> htAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#taxAmount}
	 **/
	public static volatile SingularAttribute<Commande, Integer> taxAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#createdAt}
	 **/
	public static volatile SingularAttribute<Commande, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#updatedAt}
	 **/
	public static volatile SingularAttribute<Commande, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#orderStatus}
	 **/
	public static volatile SingularAttribute<Commande, OrderStatut> orderStatus;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#orderLines}
	 **/
	public static volatile ListAttribute<Commande, OrderLine> orderLines;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#paimentStatut}
	 **/
	public static volatile SingularAttribute<Commande, PaimentStatut> paimentStatut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#type}
	 **/
	public static volatile SingularAttribute<Commande, TypeDeliveryReceipt> type;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#user}
	 **/
	public static volatile SingularAttribute<Commande, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#fournisseur}
	 **/
	public static volatile SingularAttribute<Commande, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#motifBed}
	 **/
	public static volatile SingularAttribute<Commande, MotifBed> motifBed;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#commentaireBed}
	 **/
	public static volatile SingularAttribute<Commande, String> commentaireBed;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#hasBeenSubmittedToPharmaML}
	 **/
	public static volatile SingularAttribute<Commande, Boolean> hasBeenSubmittedToPharmaML;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#originalCommandeId}
	 **/
	public static volatile SingularAttribute<Commande, Integer> originalCommandeId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#reliquatDeCommandeId}
	 **/
	public static volatile SingularAttribute<Commande, Integer> reliquatDeCommandeId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Commande#reconciliation}
	 **/
	public static volatile SingularAttribute<Commande, ReconciliationFactureFournisseur> reconciliation;

}

