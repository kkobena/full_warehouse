
# 🧩 Project: Supplier Returns Management UI

## 🎯 Goal
Implement an interactive UI screen for managing supplier returns.

---

## /agents

### /Software_Architect
- Analyze the functional requirements.
- Define the architecture and data flow for the supplier return management screen.
- Specify UI components, data bindings, and entity relationships.
- Ensure the `com.kobe.warehouse.domain.RetourBon` entity is properly integrated into the design.

### /Code_Writer
- Implement the UI according to the architect’s design.
- Ensure the following functionalities are available:
  1. Supplier selection dropdown.
  2. Order selection based on the chosen supplier.
  3. Order line selection.
  4. Input for return quantity (must not exceed the ordered quantity).
  5. Input for a return reason per order line.
  6. Dynamic table displaying added return lines, with editable quantity and reason columns.

### /Code_Reviewer
- Review the implementation for:
  - Validation rules (quantity ≤ ordered).
  - Code clarity, maintainability, and adherence to architectural guidelines.
  - UI responsiveness and accessibility.
  - Proper use of the `RetourBon` entity.

---

## 🧩 Functional Requirements

1. **Supplier Selection**
  - The user can select a supplier from a list.

2. **Order Selection**
  - Once a supplier is chosen, the user can select an order to return items from.

3. **Order Line Selection**
  - Allows selecting an individual order line (product/item).

4. **Quantity Input**
  - The user enters the quantity to return.
  - **Constraint:** Must not exceed the ordered quantity.

5. **Return Reason**
  - A reason for return must be entered for each order line.

6. **Return Table**
  - Each added line appears in a table.
  - The quantity and reason fields should be **editable inline**.

---

## ⚙️ Technical Notes

- **Domain Entity:** `com.kobe.warehouse.domain.RetourBon`
- The UI must integrate with backend services handling supplier orders and returns.
- Validation should occur both client-side and server-side.

---

## ✅ Expected Deliverables

- A functional UI screen implementing the above workflow.
- Code reviewed and approved by the `/Code_Reviewer` agent.





# 🧩 UI Implementation Request — Supplier Returns Management

## 🎯 Goal
Implement a user interface (UI) for managing supplier returns.

---

## 🧱 Functional Requirements

1. **Supplier Selection**
  - The user must be able to select a supplier from a list.

2. **Order Selection**
  - Once a supplier is selected, the user must be able to choose the specific order to return items from.

3. **Order Line Selection**
  - The user must be able to select an individual order line (product or item).

4. **Quantity to Return**
  - The user can input the quantity to return.
  - **Constraint:** The return quantity cannot exceed the originally ordered quantity.

5. **Return Reason**
  - For each order line, the user can specify a reason for the return.

6. **Return Table**
  - Added return lines must appear in a table within the UI.
  - The quantity column should remain **editable** to adjust the return quantity directly from the table.

---

## 🧩 Technical Details

- **Domain Entity to Use:**  
  `com.kobe.warehouse.domain.RetourBon`
  `com.kobe.warehouse.domain.RetourBonItem`
  `com.kobe.warehouse.domain.MotifRetourProduit`

---

## 💡 Expected Outcome

A functional and user-friendly screen that allows:
- Dynamic supplier and order selection
- Per-line return management
- Real-time validation of return quantities
- Inline editing and display of return lines

---

## ⚙️ Suggested Steps (for implementation)

```yaml
steps:
  - name: Design UI layout
    prompt: >
      Create a form with sections for supplier selection, order selection, and a dynamic return table.

  - name: Implement return logic
    prompt: >
      Use the RetourBon entity to store and validate return data.
      Ensure the quantity constraint (return ≤ ordered) is enforced.

  - name: Enable table editing
    prompt: >
      Allow inline editing of return quantities and reasons in the return table.

  - name: Final validation
    prompt: >
      Add validation messages for invalid quantities or missing return reasons.
