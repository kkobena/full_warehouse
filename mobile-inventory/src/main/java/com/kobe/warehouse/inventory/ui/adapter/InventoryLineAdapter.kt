package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.databinding.ItemInventoryLineBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * @param onQuantityEntered quantité validée sur une ligne (champ de saisie en ligne)
 * @param onLotsClick produit suivi par lots : le comptage se fait lot par lot
 */
class InventoryLineAdapter(
    private val onQuantityEntered: (StoreInventoryLine, Int) -> Unit,
    private val onLotsClick: (StoreInventoryLine) -> Unit,
    private val onAdvance: (fromPosition: Int) -> Unit
) : ListAdapter<StoreInventoryLine, InventoryLineAdapter.InventoryLineViewHolder>(InventoryLineDiffCallback()) {

    companion object {
        /** Seule la visibilité stock/écart change : voir [showStock] */
        private const val PAYLOAD_SHOW_STOCK = "PAYLOAD_SHOW_STOCK"

        /** Seule la pastille « à transmettre » change : voir [pendingSyncIds] */
        private const val PAYLOAD_SYNC = "PAYLOAD_SYNC"
    }

    /**
     * Mode aveugle : sans le privilège pr-voir-stock-inventaire, le stock
     * théorique (Init) et l'écart sont masqués pendant le comptage.
     *
     * Le basculement ne modifie pas les données, seulement leur présentation : on
     * notifie un changement partiel porteur d'une charge utile, qui ne réattache aucune
     * vue et ne rebinde que les deux champs concernés. Un `notifyDataSetChanged()`
     * invaliderait toute la liste, perdrait la position de défilement et interdirait
     * les animations d'élément.
     */
    var showStock: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SHOW_STOCK)
        }

    /** Lignes saisies mais pas encore transmises au serveur */
    var pendingSyncIds: Set<Long> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SYNC)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryLineViewHolder {
        val binding = ItemInventoryLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryLineViewHolder(binding, onQuantityEntered, onLotsClick, onAdvance)
    }

    override fun onBindViewHolder(holder: InventoryLineViewHolder, position: Int) {
        holder.bind(getItem(position), showStock, getItem(position).id in pendingSyncIds)
    }

    override fun onBindViewHolder(
        holder: InventoryLineViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        // Rebind complet dès qu'une charge utile inconnue est présente : on ne peut
        // pas savoir ce qu'elle recouvre
        val line = getItem(position)
        when {
            payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SHOW_STOCK } ->
                holder.bindStockVisibility(line, showStock)

            payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SYNC } ->
                holder.bindSyncBadge(line.id in pendingSyncIds)

            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    /**
     * Le champ de saisie est recyclé : sans cela, la vue réutilisée pour une autre
     * ligne conserverait le focus et la frappe partirait sur le mauvais produit.
     */
    override fun onViewRecycled(holder: InventoryLineViewHolder) {
        holder.releaseEditor()
        super.onViewRecycled(holder)
    }

    class InventoryLineViewHolder(
        private val binding: ItemInventoryLineBinding,
        private val onQuantityEntered: (StoreInventoryLine, Int) -> Unit,
        private val onLotsClick: (StoreInventoryLine) -> Unit,
        private val onAdvance: (fromPosition: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Ligne actuellement affichée : lue par les écouteurs, qui survivent au recyclage */
        private var boundLine: StoreInventoryLine? = null

        /**
         * Dernière quantité envoyée depuis ce champ.
         *
         * Valider puis passer à la ligne suivante fait perdre le focus au champ, ce
         * qui rejouerait [commit] : la ligne liée porte encore l'ancienne quantité
         * tant que la sauvegarde n'est pas revenue, et la même valeur partirait une
         * seconde fois — écrasant au passage le point de restauration de l'annulation.
         */
        private var committedQuantity: Int? = null

        init {
            // La validation se fait sur la touche « Terminé », pas à chaque frappe :
            // un TextWatcher enverrait une écriture par chiffre tapé (1, 12, 120…)
            // « Suivant » plutôt que « Terminé » : la touche annonce l'enchaînement
            // sur la ligne suivante. Le déplacement de focus par défaut d'Android est
            // remplacé par le nôtre, qui saute les produits suivis par lots.
            binding.etQuantity.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    commit()
                    // Le focus n'est pas relâché ici : il est donné à la ligne
                    // suivante, qui le retire à celle-ci
                    onAdvance(bindingAdapterPosition)
                    true
                } else {
                    false
                }
            }
            // Sortie de champ (autre ligne touchée, liste défilée) : la saisie en
            // cours ne doit pas être perdue silencieusement
            binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) commit()
            }
            binding.btnLots.setOnClickListener {
                boundLine?.let(onLotsClick)
            }
            // Toucher la ligne amène au champ : cible large, utilisable avec des gants
            binding.root.setOnClickListener {
                val line = boundLine ?: return@setOnClickListener
                if (line.lotCount > 0) onLotsClick(line) else binding.etQuantity.requestFocus()
            }
        }

        fun bind(line: StoreInventoryLine, showStock: Boolean, pendingSync: Boolean) {
            boundLine = line
            committedQuantity = line.quantityOnHand
            binding.apply {
                tvProductName.text = line.produitLibelle
                tvProductCode.text = "Code: ${line.produitCip ?: "N/A"}"

                val lotManaged = line.lotCount > 0
                etQuantity.visibility = if (lotManaged) View.GONE else View.VISIBLE
                btnLots.visibility = if (lotManaged) View.VISIBLE else View.GONE
                if (lotManaged) {
                    btnLots.text = root.context.getString(R.string.lots_button_format, line.lotCount)
                } else {
                    setQuantityText(line.quantityOnHand?.toString().orEmpty())
                }

                bindStockVisibility(line, showStock)
                bindSyncBadge(pendingSync)

                // Traçabilité : qui a compté cette ligne (et quand)
                if (line.countedBy.isNullOrBlank()) {
                    tvCountedBy.visibility = View.GONE
                } else {
                    tvCountedBy.visibility = View.VISIBLE
                    val date = line.updatedAt?.let { formatDate(it) }
                    tvCountedBy.text = if (date != null) {
                        root.context.getString(R.string.counted_by_at, line.countedBy, date)
                    } else {
                        root.context.getString(R.string.counted_by, line.countedBy)
                    }
                }
            }
        }

        fun bindSyncBadge(pendingSync: Boolean) {
            binding.tvSyncBadge.visibility = if (pendingSync) View.VISIBLE else View.GONE
        }

        /**
         * Stock théorique et écart : seul fragment de la ligne dépendant du mode
         * aveugle, donc seul rebindé lors d'un changement de privilège.
         *
         * L'écart n'a de sens qu'une fois la ligne comptée : sur une ligne non
         * comptée il vaudrait -quantityInit et révélerait le stock théorique
         * même en mode aveugle.
         */
        fun bindStockVisibility(line: StoreInventoryLine, showStock: Boolean) {
            binding.apply {
                if (showStock) {
                    tvQuantityInit.visibility = View.VISIBLE
                    tvQuantityInit.text = "Init: ${line.quantityInit}"
                } else {
                    tvQuantityInit.visibility = View.INVISIBLE
                }

                if (showStock && line.isCounted()) {
                    tvGap.visibility = View.VISIBLE
                    val gap = line.gap ?: line.calculateGap()
                    tvGap.text = "Écart: $gap"
                    when {
                        gap > 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                        gap < 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                        else -> tvGap.setTextColor(root.context.getColor(android.R.color.darker_gray))
                    }
                } else {
                    tvGap.visibility = View.INVISIBLE
                }
            }
        }

        /** Le champ perd le focus avant d'être réaffecté à une autre ligne */
        fun releaseEditor() {
            binding.etQuantity.clearFocus()
            boundLine = null
        }

        /**
         * Renseigne le champ sans déclencher la validation : `setText` provoque une
         * perte de focus sur certaines surcouches, ce qui rejouerait l'écriture.
         */
        private fun setQuantityText(value: String) {
            if (binding.etQuantity.text.toString() == value) return
            binding.etQuantity.setText(value)
        }

        /** Envoie la quantité si elle est valide et diffère de celle déjà enregistrée */
        private fun commit() {
            val line = boundLine ?: return
            if (line.lotCount > 0) return
            val typed = binding.etQuantity.text.toString().trim()
            if (typed.isEmpty()) return
            val quantity = typed.toIntOrNull()
            if (quantity == null || quantity < 0) {
                setQuantityText(line.quantityOnHand?.toString().orEmpty())
                return
            }
            if (quantity == committedQuantity) return
            committedQuantity = quantity
            onQuantityEntered(line, quantity)
        }

        /** ISO LocalDateTime → dd/MM HH:mm ; renvoie null si non parsable */
        private fun formatDate(iso: String): String? = try {
            LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    class InventoryLineDiffCallback : DiffUtil.ItemCallback<StoreInventoryLine>() {
        override fun areItemsTheSame(oldItem: StoreInventoryLine, newItem: StoreInventoryLine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoreInventoryLine, newItem: StoreInventoryLine): Boolean {
            return oldItem == newItem
        }
    }
}
