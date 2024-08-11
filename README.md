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

SELECT g.str_LIBELLE AS libelle,g.str_CODE AS code, f.libelle AS
groupeFournisseur_libelle,g.str_CODE_POSTAL AS addresse_postal,g.str_MOBILE AS mobile,
g.str_TELEPHONE AS phone ,g.str_URL_EXTRANET AS site, g.idrepartiteur AS identifiant_repartiteur
FROM t_grossiste g LEFT JOIN groupefournisseur f ON g.groupeId=f.id;

#REQUETE FAMILLE PRODUIT
SELECT g.str_LIBELLE AS libelle,g.str_CODE AS code,g.str_CODE_POSTAL AS addresse_postal,g.str_MOBILE
AS mobile, g.str_TELEPHONE AS phone
,g.str_URL_EXTRANET AS site, g.idrepartiteur AS identifiant_repartiteur, f.libelle AS
groupeFournisseur_libelle
FROM t_grossiste g LEFT JOIN groupefournisseur f ON g.groupeId=f.id;

#GAMME DE PRODUITS

select g.libelle AS libelle from gamme_produit g;

#LABORATOIRE PRODUITS

select l.libelle AS libelle from laboratoire l;

# REQUETE POUR EXPORTER LES RAYONS EN CSV

SELECT r.str_LIBELLEE AS libelle,r.str_CODE AS code, r.bool_ACCOUNT AS exclude FROM
t_zone_geographique r
WHERE r.str_STATUT='enable' AND r.str_LIBELLEE <> 'Default';

#REQUETE POUR EXPORTER LES GROUPE DE TIERSPAYANTS

SELECT g.str_LIBELLE as name,g.str_ADRESSE as adresse,g.str_TELEPHONE as telephone FROM
t_groupe_tierspayant g;

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
