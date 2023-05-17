# warehouse

This application was generated using JHipster 6.10.5, you can find documentation and help
at [https://www.jhipster.tech/documentation-archive/v6.10.5](https://www.jhipster.tech/documentation-archive/v6.10.5).

## Development

Before you can build this project, you must install and configure the following dependencies on your
machine:

1. [Node.js][]: We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

```
npm install
```

We use npm scripts and [Webpack][] as our build system.

Run the following commands in two separate terminals to create a blissful development experience
where your browser
auto-refreshes when files change on your hard drive.

```

./mvnw


npm start
```

Npm is also used to manage CSS and JavaScript dependencies used in this application. You can upgrade
dependencies by
specifying a newer version in [package.json](package.json). You can also run `npm update`
and `npm install` to manage dependencies.
Add the `help` flag on any command to see how you can use it. For example, `npm help update`.

The `npm run` command will list all of the scripts available to run for this project.

### PWA Support

JHipster ships with PWA (Progressive Web App) support, and it's turned off by default. One of the
main components of a PWA is a service worker.

The service worker initialization code is commented out by default. To enable it, uncomment the
following code in `src/main/webapp/index.html`:

```html

<script>
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('./service-worker.js').then(function() {
            console.log('Service Worker Registered');
        });
    }
</script>
```

Note: [Workbox](https://developers.google.com/web/tools/workbox/) powers JHipster's service worker.
It dynamically generates the `service-worker.js` file.

### Managing dependencies

For example, to add [Leaflet][] library as a runtime dependency of your application, you would run
following command:

```
npm install --save --save-exact leaflet
```

To benefit from TypeScript type definitions from [DefinitelyTyped][] repository in development, you
would run following command:

```
npm install --save-dev --save-exact @types/leaflet
```

Then you would import the JS and CSS files specified in library's installation instructions so
that [Webpack][] knows about them:
Edit [src/main/webapp/app/vendor.ts](src/main/webapp/app/vendor.ts) file:

```
import 'leaflet/dist/leaflet.js';
```

Edit [src/main/webapp/content/scss/vendor.scss](src/main/webapp/content/scss/vendor.scss) file:

```
@import '~leaflet/dist/leaflet.css';
```

Note: There are still a few other things remaining to do for Leaflet that we won't detail here.

For further instructions on how to develop with JHipster, have a look
at [Using JHipster in development][].

### Using Angular CLI

You can also use [Angular CLI][] to generate some custom client code.

For example, the following command:

```
ng generate component my-component
```

will generate few files:

```
create src/main/webapp/app/my-component/my-component.component.html
create src/main/webapp/app/my-component/my-component.component.ts
update src/main/webapp/app/app.module.ts
```

## Building for production

### Packaging as jar

To build the final jar and optimize the warehouse application for production, run:

```

./mvnw -Pprod clean verify


```

This will concatenate and minify the client CSS and JavaScript files. It will also
modify `index.html` so it references these new files.
To ensure everything worked, run:

```

java -jar target/*.jar


```

Then navigate to [http://localhost:8080](http://localhost:8080) in your browser.

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```

./mvnw -Pprod,war clean verify


```

## Testing

To launch your application's tests, run:

```
./mvnw verify
```

### Client tests

Unit tests are run by [Jest][] and written with [Jasmine][]. They're located
in [src/test/javascript/](src/test/javascript/) and can be run with:

```
npm test
```

For more information, refer to the [Running tests page][].

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible
on http://localhost:9001) with:

```
docker-compose -f src/main/docker/sonar.yml up -d
```

You can run a Sonar analysis with using
the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by
using the maven plugin.

Then, run a Sonar analysis:

```
./mvnw -Pprod clean verify sonar:sonar
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase
since Sonar properties are loaded from the sonar-project.properties file.

```
./mvnw initialize sonar:sonar
```

For more information, refer to the [Code quality page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose
configuration are available in the [src/main/docker](src/main/docker) folder to launch required
third party services.

For example, to start a mariadb database in a docker container, run:

```
docker-compose -f src/main/docker/mariadb.yml up -d
```

To stop it and remove the container, run:

```
docker-compose -f src/main/docker/mariadb.yml down
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

```
./mvnw -Pprod verify jib:dockerBuild
```

Then run:

```
docker-compose -f src/main/docker/app.yml up -d
```

For more information refer to [Using Docker and Docker-Compose][], this page also contains
information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to
generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you
generate configuration files for a number of Continuous Integration systems. Consult
the [Setting up Continuous Integration][] page for more information.

[jhipster homepage and latest documentation]: https://www.jhipster.tech

[jhipster 6.10.5 archive]: https://www.jhipster.tech/documentation-archive/v6.10.5

[using jhipster in development]: https://www.jhipster.tech/documentation-archive/v6.10.5/development/

[using docker and docker-compose]: https://www.jhipster.tech/documentation-archive/v6.10.5/docker-compose

[using jhipster in production]: https://www.jhipster.tech/documentation-archive/v6.10.5/production/

[running tests page]: https://www.jhipster.tech/documentation-archive/v6.10.5/running-tests/

[code quality page]: https://www.jhipster.tech/documentation-archive/v6.10.5/code-quality/

[setting up continuous integration]: https://www.jhipster.tech/documentation-archive/v6.10.5/setting-up-ci/

[node.js]: https://nodejs.org/

[yarn]: https://yarnpkg.org/

[webpack]: https://webpack.github.io/

[angular cli]: https://cli.angular.io/

[browsersync]: https://www.browsersync.io/

[jest]: https://facebook.github.io/jest/

[jasmine]: https://jasmine.github.io/2.0/introduction.html

[protractor]: https://angular.github.io/protractor/

[leaflet]: https://leafletjs.com/

[definitelytyped]: https://definitelytyped.org/

https://github.com/ng-select/ng-select

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

<div [ngSwitch]="name">
<p *ngSwitchCase="'Webcam'">
Product is used for video
</p>
<p *ngSwitchCase="'Microphone'">
Product is used for audio
</p>
<p *ngSwitchDefault>Product is for general use</p>
</div>
<li *ngFor="let product of products | keyvalue">
{{product.key}}
</li>

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

@Output() deleted = new EventEmitter();
V<app-product-detail
*ngIf="selectedProduct; else noProduct"
[id]="selectedProduct.id"
(deleted)="onDelete()"
(bought)="onBuy()">
</app-product-detail>
{ path: '', redirectTo: '/products', pathMatch: 'full' }
input.ng-touched {
border: 3px solid lightblue;
}
• ng-untouched: Indicates that we have not interacted with the control yet
• ng-touched: Indicates that we have interacted with the control
• ng-dirty: Indicates that we have set a value to the control
• ng-pristine: Indicates that the control does not have a value yet
• ng-valid: Indicates that the value of the control is valid
• ng-invalid: Indicates that the value of the control is not valid

input.ng-dirty.ng-valid {
border: 2px solid green;
}
input.ng-dirty.ng-invalid {
border: 2px solid red;
}
<div [formGroup]="cartForm">
<div
formArrayName="products"
*ngFor="let product of cartForm.controls.products.controls; let
i=index">
<label>{{cart[i].name}}</label>
<input type="number" [formControlName]="i" />
</div>
</div>
