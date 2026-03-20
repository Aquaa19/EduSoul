package com.aquaa.edusoul.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.FeeStructure
import java.util.ArrayList

class FeeStructureAdapter(
    private val context: Context,
    private var feeStructureList: MutableList<FeeStructure>,
    private val listener: OnItemClickListener?,
    private val isSelectionMode: Boolean = false
) : RecyclerView.Adapter<FeeStructureAdapter.FeeStructureViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(feeStructure: FeeStructure)
        fun onEditClick(feeStructure: FeeStructure)
        fun onDeleteClick(feeStructure: FeeStructure)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): FeeStructureViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_fee_structure, parent, false)
        return FeeStructureViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: FeeStructureViewHolder, position: Int) {
        val feeStructure = feeStructureList[position]
        holder.textViewTitle.text = feeStructure.title
        holder.textViewDescription.text = feeStructure.description ?: "No description"
        holder.textViewAmount.text = "Amount: ${feeStructure.amount ?: "N/A"}"
        holder.textViewDuration.text = "Duration: ${feeStructure.duration ?: "N/A"}"

        if (isSelectionMode) {
            holder.imageButtonEdit.visibility = View.GONE
            holder.imageButtonDelete.visibility = View.GONE
            holder.itemView.setOnClickListener {
                Log.d("FeeAdapter", "Fee structure item clicked: ${feeStructure.title} (ID: ${feeStructure.id})")
                listener?.onItemClick(feeStructure)
            }
        } else {
            holder.imageButtonEdit.visibility = View.VISIBLE
            holder.imageButtonDelete.visibility = View.VISIBLE
            holder.imageButtonEdit.setOnClickListener {
                listener?.onEditClick(feeStructure)
            }
            holder.imageButtonDelete.setOnClickListener {
                listener?.onDeleteClick(feeStructure)
            }
        }
    }

    override fun getItemCount(): Int {
        return feeStructureList.size
    }

    fun setFeeStructures(newFeeStructureList: List<FeeStructure>) {
        this.feeStructureList.clear()
        this.feeStructureList.addAll(newFeeStructureList)
        notifyDataSetChanged()
    }

    class FeeStructureViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewFeeTitle)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewFeeDescription) // Corrected ID
        val textViewAmount: TextView = itemView.findViewById(R.id.textViewFeeAmount)
        val textViewDuration: TextView = itemView.findViewById(R.id.textViewFeeFrequency)
        val imageButtonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEditFeeStructure)
        val imageButtonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDeleteFeeStructure)
    }
}