package com.aquaa.edusoul.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton // Import ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Batch
import java.util.ArrayList

class BatchSelectionAdapter(
    private var batches: MutableList<Batch>,
    private var assignedBatchIds: MutableList<String>,
    private val listener: OnBatchAssignmentActionListener?, // Changed listener type
    private val homeworkId: String // Add homeworkId to adapter constructor
) : RecyclerView.Adapter<BatchSelectionAdapter.BatchViewHolder>() {

    private val selectedBatchIds = mutableSetOf<String>()

    init {
        selectedBatchIds.addAll(assignedBatchIds)
    }

    interface OnBatchAssignmentActionListener {
        fun onAssignBatch(homeworkId: String, batchId: String)
        fun onRemoveBatchAssignment(homeworkId: String, batchId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_selection, parent, false)
        return BatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        val batch = batches[position]
        holder.bind(batch, homeworkId, selectedBatchIds, listener) // Pass homeworkId to bind method
    }

    override fun getItemCount(): Int = batches.size

    fun setData(newBatches: List<Batch>, newAssignedBatchIds: List<String>) {
        this.batches.clear()
        this.batches.addAll(newBatches)
        this.assignedBatchIds.clear()
        this.assignedBatchIds.addAll(newAssignedBatchIds)
        this.selectedBatchIds.clear()
        this.selectedBatchIds.addAll(newAssignedBatchIds)
        notifyDataSetChanged()
    }

    fun getSelectedBatchIds(): Set<String> {
        return selectedBatchIds.filter { !assignedBatchIds.contains(it) }.toSet() // Only return newly selected batches for assignment
    }

    inner class BatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxBatch)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewBatchName)
        private val textViewAssignedStatus: TextView = itemView.findViewById(R.id.textViewAssignedStatus)
        private val buttonRemoveAssignment: ImageButton = itemView.findViewById(R.id.buttonRemoveAssignment)

        fun bind(
            batch: Batch,
            currentHomeworkId: String, // Renamed to avoid conflict
            selectedBatchIdsSet: MutableSet<String>, // Passed directly
            listener: OnBatchAssignmentActionListener?
        ) {
            nameTextView.text = batch.batchName
            val isAssigned = assignedBatchIds.contains(batch.id)

            // Control checkbox state and enabled status
            checkBox.isChecked = isAssigned || selectedBatchIdsSet.contains(batch.id)
            checkBox.isEnabled = !isAssigned // Disable checkbox if already assigned from DB

            // Handle visibility and actions for "Assigned" status
            if (isAssigned) {
                textViewAssignedStatus.visibility = View.VISIBLE
                buttonRemoveAssignment.visibility = View.VISIBLE
                // If it's already assigned, the checkbox reflects that, and it's disabled.
                // The primary action becomes "Remove".
                checkBox.setOnCheckedChangeListener(null) // Clear listener to prevent accidental state changes
            } else {
                textViewAssignedStatus.visibility = View.GONE
                buttonRemoveAssignment.visibility = View.GONE
                // If not assigned, the checkbox is enabled and its state determines if it will be assigned.
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedBatchIdsSet.add(batch.id)
                    } else {
                        selectedBatchIdsSet.remove(batch.id)
                    }
                }
            }

            // Set listener for the remove button
            buttonRemoveAssignment.setOnClickListener {
                listener?.onRemoveBatchAssignment(currentHomeworkId, batch.id)
            }
        }
    }
}