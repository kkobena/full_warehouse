# Implémentation UI Android — Composant sales-home

## Composant principal
`C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\sales-home\sales-home.component.html`

---

## 1. Si `useSimpleSale = true`

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

## 2. Si `useSimpleSale = false`

### UI générale
- Tab bar avec deux onglets :
  1. **Vente en cours**
  2. **Prévente**

---

### Onglet : Vente en cours

#### Contenu
- Champ de recherche
- Bouton **Nouvelle vente**  
  `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\selling-home.component.html`

#### Types de ventes
1. Comptant  
   `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\comptant`
2. Carnet  
   `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\carnet`

---

### Onglet : Prévente

#### Types de préventes
1. Comptant  
   `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\comptant`
2. Carnet  
   `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\carnet`
3. Assurance  
   `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\selling-home\assurance`
