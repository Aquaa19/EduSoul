// File: main/java/com/aquaa/edusoul/adapters/AdminDashboardCardsAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.google.android.material.card.MaterialCardView

data class DashboardCardItem(
    val id: Int,
    val title: String,
    val iconResId: Int,
    val targetActivity: Class<*>,
    var isVisible: Boolean = true
)

class AdminDashboardCardsAdapter(
    private val context: Context,
    private var cards: MutableList<DashboardCardItem>,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<AdminDashboardCardsAdapter.CardViewHolder>() {

    interface OnItemClickListener {
        fun onCardClick(cardItem: DashboardCardItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_admin_dashboard_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = cards[position]

        holder.textViewCardTitle.text = cardItem.title
        holder.imageViewCardIcon.setImageResource(cardItem.iconResId)

        holder.itemView.visibility = if (cardItem.isVisible) View.VISIBLE else View.GONE
        holder.itemView.layoutParams.height = if (cardItem.isVisible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        holder.itemView.layoutParams.width = if (cardItem.isVisible) ViewGroup.LayoutParams.MATCH_PARENT else 0


        holder.itemView.setOnClickListener {
            if (cardItem.isVisible) {
                onItemClickListener.onCardClick(cardItem)
            }
        }
    }

    override fun getItemCount(): Int = cards.size

    fun updateList(newList: List<DashboardCardItem>) {
        cards.clear()
        cards.addAll(newList)
        notifyDataSetChanged()
    }

    fun getCurrentOrder(): List<DashboardCardItem> {
        return cards
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val materialCardView: MaterialCardView = itemView.findViewById(R.id.materialCardView)
        val imageViewCardIcon: ImageView = itemView.findViewById(R.id.imageViewCardIcon)
        val textViewCardTitle: TextView = itemView.findViewById(R.id.textViewCardTitle)
    }
}