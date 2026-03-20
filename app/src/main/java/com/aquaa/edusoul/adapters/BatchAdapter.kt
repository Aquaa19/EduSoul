// File: main/java/com/aquaa/edusoul/adapters/BatchAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Batch
import com.google.android.material.button.MaterialButton
import java.util.ArrayList

/*
 * BatchAdapter: RecyclerView adapter for displaying a list of batches.
 * Handles binding Batch data to the item_batch layout and provides
 * callbacks for edit, delete, and manage enrollments actions.
 * Migrated to Kotlin. No longer directly uses DatabaseHelper for fee structure name.
 */
class BatchAdapter(
    private val context: Context,
    private var batchList: ArrayList<Batch>, // Changed to specific ArrayList
    private val listener: OnBatchActionsListener?,
    private val getFeeStructureTitleById: (String?) -> String // Changed from Long? to String?
) : RecyclerView.Adapter<BatchAdapter.BatchViewHolder>() {

    interface OnBatchActionsListener {
        fun onEditBatch(batch: Batch, position: Int)
        fun onDeleteBatch(batch: Batch, position: Int)
        fun onManageEnrollments(batch: Batch, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): BatchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_batch, parent, false)
        return BatchViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: BatchViewHolder, position: Int) {
        val batch = batchList[position]
        holder.textViewBatchName.text = batch.batchName
        holder.textViewBatchDetails.text = "Grade: ${batch.gradeLevel ?: "N/A"} | Year: ${batch.academicYear ?: "N/A"}"

        val feeStructureTitle = getFeeStructureTitleById(batch.feeStructureId)
        if (feeStructureTitle.isNotBlank() && feeStructureTitle != "N/A") {
            holder.textViewFeeStructure.text = "Fee Plan: $feeStructureTitle"
            holder.textViewFeeStructure.visibility = View.VISIBLE
        } else {
            holder.textViewFeeStructure.visibility = View.GONE
        }

        holder.imageButtonEditBatch.setOnClickListener {
            listener?.onEditBatch(batch, holder.adapterPosition)
        }

        holder.imageButtonDeleteBatch.setOnClickListener {
            listener?.onDeleteBatch(batch, holder.adapterPosition)
        }

        holder.imageButtonManageEnrollments.setOnClickListener {
            listener?.onManageEnrollments(batch, holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return batchList.size
    }

    fun setBatches(newList: List<Batch>) {
        this.batchList.clear()
        this.batchList.addAll(newList) // Removed explicit cast to ArrayList
        notifyDataSetChanged()
    }

    class BatchViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewBatchName: TextView = itemView.findViewById(R.id.textViewBatchName)
        val textViewBatchDetails: TextView = itemView.findViewById(R.id.textViewBatchDetails)
        val textViewFeeStructure: TextView = itemView.findViewById(R.id.textViewBatchFeeStructure)
        val imageButtonEditBatch: ImageButton = itemView.findViewById(R.id.imageButtonEditBatch)
        val imageButtonDeleteBatch: ImageButton = itemView.findViewById(R.id.imageButtonDeleteBatch)
        val imageButtonManageEnrollments: ImageButton = itemView.findViewById(R.id.imageButtonManageEnrollments)
    }
}