# warehouse

```
npm install
```

```

./mvnw


npm start


## requete pour le fichier csv d'importation de la table tiers-payant de prestige 2

SELECT p.str_FULLNAME,p.str_NAME,p.str_TELEPHONE,p.str_MOBILE,p.str_ADRESSE,
p.str_REGISTRE_COMMERCE,p.int_NBRE_EXEMPLAIRE_BORD,p.b_IsAbsolute,
p.dbl_PLAFOND_CREDIT,p.b_CANBEUSE,p.int_MONTANTFAC,p.dbl_REMISE_FORFETAIRE,r.str_CODE_RISQUE,t.str_CODE_TYPE_TIERS_PAYANT
FROM t_tiers_payant p , t_risque r,t_type_tiers_payant t WHERE p.lg_RISQUE_ID=r.lg_RISQUE_ID
AND t.lg_TYPE_TIERS_PAYANT_ID=p.lg_TYPE_TIERS_PAYANT_ID

# @Transactional(timeout=3600)

#Coût moyen achat
Average Cost = Total Cost of Goods(Achats) ÷ Total Quantity of Goods
Available for Sale Available for Sale

Current Ratio = Current Assets(Actifs) ÷ Current Liabilities(Passifs)
#Taux de rotation des stock
Inventory Turnover Ratio = Cost of Goods Sold ÷ Average Inventory(moyen stock pour une période
donnée

# inventory balance (solde de stock calculé à la fin du ternier jour iuvrable du mois)

#Cost of goods sold (Coût des marchandises vendues)
Beginning inventory(date fin dernier invententaire) + Purchases during the period - Ending
inventory = Cost of goods sold

#Inventory Turnover (Rotation des stocks)
Annual cost of goods sold(montant des vente annuelle) ÷ Inventory(valorisation en vente du stock
actuel) = Inventory turnover

#exemple de calcul Inventory Turnover
$8,150,000 Cost of Goods Sold
-------------------------------------------- = 5 Turns Per Year===>> 365/5 (nbre stock en main
journalier)
$1,630,000 Inventory

#Average inventory calculation
(Beginning inventory + Ending inventory) / 2
#exemple

#January ending inventory $185,000
#February ending inventory $213,000
#March ending inventory $142,000
#Total $540,000
#Average inventory = Total / 3 \$180,000

#Days of Inventory
365 ÷ (Annualized cost of goods sold ÷ Inventory)

365 ÷ ($1,000,000 ÷ $200,000) = 73 Days of inventory

#stock replenishment (réapprovisionnement de stock)
#ROP(reorder point) FORMULE
(Usage x Lead Time) + Safety Stock = ROP

usage=consommation

Lead time =delais de livraison

Safety stock =stock de secutité

#Lead time

1 week = 0.25 = 25%

4 weeks = 1.00 = 100%

2 weeks = 0.50 = 50%

5 weeks = 1.25 = 125%

3 weeks = 0.75 = 75%

6 weeks = 1.50 = 150%

#Example 1:
Assume:

• Usage rate of 1,200 items per month

• Lead time of 3 weeks

Step-by-Step Calculation:

• Calculate weekly usage. Assume a 4-week month. 1,200

items ÷ 4 weeks = 300 items per week ➜ therefore Bin 1
or working stock should contain at least 300 items

• Calculate working reserve: Given 3 weeks of lead time,
working reserve should be 1,200 items x 0.75 = 900 items

• Calculate safety stock, use 50 percent of working reserve
as a guideline (900 items x 50% = 450 items)

• Calculate ROP: (1,200 items x 0.75) + 450 items = ROP
1,350 items

#Example 2:
Assume:

• Usage rate of 1,200 items per month

• Lead time of 1 week

Step-by-Step Calculation:

• Calculate weekly usage. Assume a 4-week month. 1,200
items ÷ 4 weeks = 300 items per week ➜ therefore Bin 1

or working stock should contain at least 300 items
• Calculate working reserve: Given 1 week of lead time,
working reserve should be 1,200 items x 0.25 = 300 items

• Calculate safety stock, use 50 percent of working reserve
as a guideline (300 items x 50% = 150 items)

• Calculate ROP: (1,200 items x 0.25) + 150 items = ROP
450 items

#Economic Order Quantity (EOQ) Formula

# REQUETE POUR EXPORTER LES FOURNISSEURS EN CSV

SELECT  TRIM(g.str_LIBELLE) AS libelle,TRIM(g.str_CODE) AS code, f.libelle AS
groupeFournisseur_libelle,g.str_CODE_POSTAL AS addresse_postal,g.str_MOBILE AS mobile,
g.str_TELEPHONE AS phone ,g.str_URL_EXTRANET AS site, g.idrepartiteur AS identifiant_repartiteur
FROM t_grossiste g LEFT JOIN groupefournisseur f ON g.groupeId=f.id GROUP BY TRIM(g.str_LIBELLE);

#REQUETE FAMILLE PRODUIT
SELECT DISTINCT g.str_LIBELLE AS libelle,g.str_CODE AS code,g.str_CODE_POSTAL AS addresse_postal,g.str_MOBILE
AS mobile, g.str_TELEPHONE AS phone
,g.str_URL_EXTRANET AS site, g.idrepartiteur AS identifiant_repartiteur, f.libelle AS
groupeFournisseur_libelle
FROM t_grossiste g LEFT JOIN groupefournisseur f ON g.groupeId=f.id;

#GAMME DE PRODUITS

select g.libelle AS libelle from gamme_produit g;

#LABORATOIRE PRODUITS

select l.libelle AS libelle from laboratoire l;

# REQUETE POUR EXPORTER LES RAYONS EN CSV

SELECT DISTINCT  TRIM(r.str_LIBELLEE) AS libelle,TRIM(r.str_CODE) AS code, r.bool_ACCOUNT AS exclude FROM
t_zone_geographique r
WHERE r.str_STATUT='enable' AND r.str_LIBELLEE <> 'Default' GROUP BY TRIM(r.str_LIBELLEE);

#REQUETE POUR EXPORTER LES GROUPE DE TIERSPAYANTS

SELECT g.str_LIBELLE as name,g.str_ADRESSE as adresse,g.str_TELEPHONE as telephone FROM
t_groupe_tierspayant g;
SELECT f.libelle AS libelle FROM  groupefournisseur f ORDER BY  f.id;
const interval = setInterval(() => {
this.entityService.findImortation().subscribe(
res => {
if (res.body) {
this.responsedto = res.body;
this.jsonFileUploadProgress = Math.round(this.responsedto.size!\*100/this.responsedto.totalSize!);
if (this.responsedto.completed) {
this.jsonFileUploadStatutProgress = this.jsonFileUploadStatutCompleted;
this.loadPage();
clearInterval(interval);
}
}
},
() => clearInterval(interval)
);
}, 100);

Mobile/small credit card terminals (Verifone, etc): 57mm x 50mm, 57mm x 40mm, 57mm x 38mm
Thermal receipt printers (Epson, Star, etc): 80mm x 80mm, 80mm x 70mm, 80mm x 60mm

## apache poi

https://poi.apache.org/components/spreadsheet/quick-guide.html

* "0 0 * * * *" = the top of every hour of every day.
* "*/10 * * * * *" = every ten seconds.
* "0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
* "0 0 8,10 * * *" = 8 and 10 o'clock of every day.
* "0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30 and 10 o'clock every day.
* "0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
* "0 0 0 25 12 ?" = every Christmas Day at midnight
  <dependency>
  <groupId>commons-net</groupId>
  <artifactId>commons-net</artifactId>
  <version>3.6</version>
  </dependency>
* <dependency>
  <groupId>commons-net</groupId>
  <artifactId>commons-net</artifactId>
  <version>3.9.0</version>

</dependency>
* https://tf.nist.gov/tf-cgi/servers.cgi
* public static final void timeTCP(String host) throws IOException {
  TimeTCPClient client = new TimeTCPClient();

// We want to timeout if a response takes longer than 60 seconds
client.setDefaultTimeout(60000);/*www.ja v a 2 s . c o m*/
client.connect(host);
System.out.println(client.getDate().toString());
client.disconnect();
}
#lpr:hostname:port/printername impression remote



transform(value: Product[]): Product[] {
if (value) {
return value.sort((a: Product, b: Product) => {
if (a.name < b.name) {
return -1;
} else if (b.name < a.name) {
return 1;
}
return 0;
});
}
return [];
}

@HostBinding
@HostListener
ViewContainerRef
*ngFor="let product of (products$ | async)! | sort; let i=index
products$: Observable<Product[]> | undefined;

private getProducts() {
this.products$ = this.productService.getProducts();
}


input.ng-touched {
border: 3px solid lightblue;
}
• ng-untouched: Indicates that we have not interacted with the control yet
• ng-touched: Indicates that we have interacted with the control
• ng-dirty: Indicates that we have set a value to the control
• ng-pristine: Indicates that the control does not have a value yet
• ng-valid: Indicates that the value of the control is valid
• ng-invalid: Indicates that the value of the control is not valid


https://www.freecodecamp.org/news/javascript-keycode-list-keypress-event-key-codes/
1. Pouvoir une vente au comptant
2. Faire une vente à crédit
**Parametre d'impression ou pas du ticket (0=pas de ticket , 1=ticket systematique)
** Faire le point sur le CA réalisé(par type de vente, periode)

3. Passer des commandes (exportation csv)
4. Faire une entrée en stock
	*saisie manuelle produit par produit
	*(importation csv/excel)

	*faire le point sur les produits et BL saisis

5. suivi mouvement article

6. Retour à la pharmacie principale

7. Retour fournisseur dans le cas d'une saisie grossiste

8. Ajustement de stock avec motif d'ajustement

10. inventaire depot

	*saisie manuelle
	*inventaire avec le mobile

11.Facturation

	*Point des ventes à credit saisie
	*Edition facture des ventes à credit saisies

12. Saisie de mouvement de caisse

13. Saisie des produits périmés

# REQUETE POUR EXPORTER LES tierspayants EN CSV

SELECT p.lg_TIERS_PAYANT_ID id, p.str_NAME  as name,p.str_FULLNAME as fullName,p.str_CODE_ORGANISME as codeOrganisme
,p.int_NBREBONS as nbreBordereaux,p.is_cmus as isCmus,p.str_TELEPHONE as telephoneFixe,
 p.str_MOBILE as telephone,p.str_ADRESSE as adresse,p.dbl_REMISE_FORFETAIRE as remiseForfaitaire,gp.str_LIBELLE  asgroupeTiersPayantName,
 tp.str_LIBELLE_TYPE_TIERS_PAYANT as typeTiersPayantName,p.str_CODE_REGROUPEMENT as codeRegroupement,p.to_be_exclude as toBeExclude,
 p.dbl_PLAFOND_CREDIT as plafondCredit,mf.str_VALUE as modelFactureName,
 p.account  as account,p.b_IsAbsolute plafondCreditIsAbsolute
,p.int_MONTANTFAC as montantMaxParFcture,
 p.is_depot as isdepot,p.grouping_by_taux as groupingByTaux,p.db_CONSOMMATION_MENSUELLE as consommationMensuelle,
 p.bool_IsACCOUNT as isAccount
FROM  t_tiers_payant p  left join t_groupe_tierspayant gp ON  p.lg_GROUPE_ID=gp.lg_GROUPE_ID
left join t_type_tiers_payant tp on p.lg_TYPE_TIERS_PAYANT_ID = tp.lg_TYPE_TIERS_PAYANT_ID
left join t_model_facture mf on p.lg_MODEL_FACTURE_ID = mf.lg_MODEL_FACTURE_ID
WHERE p.str_STATUT='enable' ;


    # REQUETE POUR EXPORTER LES CLIENTS EN CSV

select c.lg_CLIENT_ID as id, c.str_FIRST_NAME as firstName,c.str_LAST_NAME as lastName,
c.str_SEXE  as sexe,c.dt_NAISSANCE  asdatNaiss,c.lg_TYPE_CLIENT_ID as typeClientId,c.str_ADRESSE as phone,
c.str_NUMERO_SECURITE_SOCIAL as numSecu
from t_client c where str_STATUT='enable' ;

# REQUETE POUR EXPORTER LES AYANDROIT EN CSV

SELECT a.lg_AYANTS_DROITS_ID as id,  a.lg_CLIENT_ID as idClient,
       a.str_FIRST_NAME as firstName,a.str_LAST_NAME as lastName,
       a.str_SEXE  as sexe,a.dt_NAISSANCE  asdatNaiss
       FROM t_ayant_droit a WHERE str_STATUT='enable'  AND a.lg_AYANTS_DROITS_ID <> a.lg_CLIENT_ID;

# REQUETE POUR EXPORTER LES compte client EN CSV
SELECT cp.lg_COMPTE_CLIENT_TIERS_PAYANT_ID as id, cl.lg_CLIENT_ID as clientId,tp.lg_TIERS_PAYANT_ID as tiersPayantId,cp.str_NUMERO_SECURITE_SOCIAL as numSecu
,cp.b_IS_RO as ro,cp.int_PRIORITY as priority,cp.int_POURCENTAGE as taux,cp.db_PLAFOND_ENCOURS as plafondEncours,
 cp.dbl_PLAFOND as plafond,cp.dbl_QUOTA_CONSO_VENTE as planfondVente,cp.b_IsAbsolute as plafondIsAbsolute
FROM t_compte_client_tiers_payant cp join t_compte_client c on cp.lg_COMPTE_CLIENT_ID = c.lg_COMPTE_CLIENT_ID
         JOIN  t_tiers_payant tp on cp.lg_TIERS_PAYANT_ID = tp.lg_TIERS_PAYANT_ID
       JOIN t_client cl on c.lg_CLIENT_ID = cl.lg_CLIENT_ID
         WHERE cp.str_STATUT='enable' ;

  # export requete to csv
mysql -u username -p -e "SELECT * FROM your_table;" -B > output.csv

##REQUETE POUR EXPORTER LES produits EN CSV
 `SELECT TRIM(f.int_CIP) AS codeCip,IFNULL(f.int_EAN13,'')   AS codeEan,TRIM(f.str_NAME) AS produitName,f.int_PAF AS prixAchat,f.int_PRICE AS prixUni,
s.int_NUMBER_AVAILABLE AS produitStock,f.int_SEUIL_MIN AS seuilMin,
f.int_QTE_REAPPROVISIONNEMENT AS qtyReappro,f.bool_ACCOUNT AS chiffre,
f.is_scheduled AS scheduled,
IFNULL( f.cmu_price,0) AS prixCmu,
tva.int_VALUE AS codeTva,IFNULL(f.str_CODE_REMISE,'0') AS codeRemise,IFNULL(TRIM(f.int_T),'') AS codeTableau,TRIM(z.str_CODE) AS codeRayon
,TRIM(fm.str_CODE_FAMILLE) AS codeFamille,TRIM(gr.str_CODE)  AS codeFournnisseur,IFNULL(f.bool_CHECKEXPIRATIONDATE,'0') AS checkExpiryDate,
IFNULL(DATE_FORMAT(f.dt_PEREMPTION,'%Y-%m-%d'),'') AS perimeAt,IFNULL(TRIM(lab.libelle),'') AS libelleLab,
IFNULL(TRIM(game.libelle),'') AS libelleGamme,IFNULL(f.int_NUMBERDETAIL,1) AS nombreDetail,IFNULL(decon.int_NUMBER_AVAILABLE,'') AS deconQty,IFNULL(decon.prixDT,'') AS prixUniDetail
,IFNULL(decon.prixAchatDT,'') AS prixAchatDetail
 FROM  t_famille f JOIN t_famille_stock s ON f.lg_FAMILLE_ID=s.lg_FAMILLE_ID
JOIN t_zone_geographique z ON f.lg_ZONE_GEO_ID=z.lg_ZONE_GEO_ID
JOIN t_famillearticle fm ON f.lg_FAMILLEARTICLE_ID =fm.lg_FAMILLEARTICLE_ID
JOIN t_grossiste gr ON f.lg_GROSSISTE_ID=gr.lg_GROSSISTE_ID
LEFT JOIN laboratoire lab ON f.laboratoire_id=lab.id LEFT JOIN gamme_produit game ON f.gamme_id=game.id
JOIN t_code_tva tva ON f.lg_CODE_TVA_ID=tva.lg_CODE_TVA_ID
LEFT JOIN (SELECT id.int_PRICE AS prixDT,id.int_PAF AS prixAchatDT, ds.int_NUMBER_AVAILABLE,id.lg_FAMILLE_PARENT_ID FROM t_famille id JOIN  t_famille_stock ds ON id.lg_FAMILLE_ID=ds.lg_FAMILLE_ID WHERE ds.lg_EMPLACEMENT_ID='1'
AND id.str_STATUT='enable' AND id.bool_DECONDITIONNE=1 AND ds.int_NUMBER_AVAILABLE >=0 GROUP  BY id.lg_FAMILLE_PARENT_ID) AS decon ON f.lg_FAMILLE_ID=decon.lg_FAMILLE_PARENT_ID

WHERE s.lg_EMPLACEMENT_ID='1' AND f.str_STATUT='enable' AND f.bool_DECONDITIONNE=0  GROUP BY TRIM(f.int_CIP), gr.lg_GROSSISTE_ID;
`
npm install libphonenumber-js --save

CREATE USER warehouse IDENTIFIED BY 'warehouse2802';
GRANT ALL privileges ON `warehouse`.* TO `warehouse`;
```

SELECT SUM(s.sales_amount) AS montantTtc,SUM(s.discount_amount) AS montantRemise,SUM(s.ht_amount) AS
htAmount,
SUM(s.tax_amount) AS tvaAmount,
SUM(s.part_tiers_payant) AS montantTiersPayant,
SUM(s.net_amount) AS montantNet,
SUM(s.cost_amount) AS montantAchat,
SUM(s.rest_to_pay) montantRestant

FROM sales s WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED') ;

SELECT SUM(p.paid_amount) AS montant,p.payment_mode_code,pm.libelle FROM payment p JOIN sales s ON
p.sales_id = s.id
JOIN payment_mode pm ON p.payment_mode_code = pm.code
WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED','REMOVE') GROUP BY p.payment_mode_code;

SELECT p.type_transaction AS typeTransaction,SUM(p.amount) AS amount FROM payment_transaction p
JOIN payment_mode pm ON p.payment_mode_code = pm.code
WHERE p.categorie_ca IN ('CA') GROUP BY p.type_transaction ;

SELECT SUM(d.net_amount) AS netAmount,SUM(d.tax_amount) AS taxAmount,SUM(d.receipt_amount) AS
amountTtc
FROM delivery_receipt d;
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dumps/heapdump.hprof
<dependency>
<groupId>org.docx4j</groupId>
<artifactId>docx4j-ImportXHTML-core</artifactId>
<version>11.5.3</version>
</dependency>
