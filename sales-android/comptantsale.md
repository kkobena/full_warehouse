# Implémentation UI Android — Composant sales-home
**Agent: Architect**

## Composant principal
`C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\sales-home\sales-home.component.html`

---

### UI à implémenter
- Champ de recherche
- Bouton **Nouvelle vente**  
  `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\comptant-home\comptant-home.component.html`

### Liste des ventes en cours
Pour chaque vente, afficher :
- `numberTransaction`
- `updatedAt`
- `customer.firstName`
- `customer.lastName` *(si `customer != null`)*

### Navigation
- Cliquez sur une vente → ouvrir l’écran de vente simple  
  `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\comptant-home\comptant-home.component.html`

---
**Agent: CodeWriter**
## omptant-home
- propose different layouts for small and large screens(smartphone vs tablet)
- allows users to search for products by name or code
- displays product search results in a list or grid depending on screen size:
  - small screens: list view with product name, price,current stock and add-to-cart button
  - large screens: grid view product name, price,current stock and add-to-cart button
- allows users to add products to the cart by clicking on them
- maintains the cart state and updates it when products are added
- allows users to modify product quantities in the cart
- allows users to remove products from the cart
- shows the current cart contents and total price
- provides buttons to clear the cart or proceed to checkout
- uses Angular Material components for UI elements like buttons, lists, and grids
- handles user interactions and updates the UI accordingly
- take into account the space constraints
# Payment process
- allows users to select a payment method (cash, credit card, mobile money):
  - for mobile money, allows customers to scan a QR code to make payment (C:\Users\k.kobena\Documents\full_warehouse\src\main\java\com\kobe\warehouse\domain\PaymentMode.java) if qrCode not null
# Receipt generation
- add com.sunmi printerlibrary
- add confirmation dialog before printing receipt:
  - "Souhaitez-vous imprimer le reçu?"
  - options: "Oui" and "Non"
- generate and print receipts after successful payment
- example of the java code to print C:\Users\k.kobena\Documents\full_warehouse\src\main\java\com\kobe\warehouse\service\receipt\service\CashSaleReceiptService.java
