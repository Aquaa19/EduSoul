// File: main/java/com/aquaa/edusoul/adapters/ParentFeePaymentAdapter.kt
package com.aquaa.edusoul.adapters

import ParentFeePaymentDetails
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import java.text.NumberFormat
import java.util.Locale
import android.util.TypedValue // Import TypedValue

class ParentFeePaymentAdapter(
    private val context: Context,
    private var paymentDetailsList: MutableList<ParentFeePaymentDetails>,
    private val listener: OnItemClickListener?
) : RecyclerView.Adapter<ParentFeePaymentAdapter.ParentFeePaymentViewHolder>() {

    interface OnItemClickListener {
        fun onFeePaymentCardClick(paymentDetails: ParentFeePaymentDetails)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ParentFeePaymentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_parent_fee_payment, parent, false)
        return ParentFeePaymentViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ParentFeePaymentViewHolder, position: Int) {
        val currentItem = paymentDetailsList[position]
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        holder.textViewPaymentStudentName.text = currentItem.studentName
        holder.textViewPaymentPeriod.text = currentItem.paymentPeriod
        holder.textViewPaymentDate.text = "Date: ${currentItem.paymentDate}"
        holder.textViewPaymentMethod.text = "Method: ${currentItem.paymentMethod}"
        holder.textViewAmountPaid.text = "Paid: ${currencyFormatter.format(currentItem.amountPaid)}"

        // FIX: Change condition to check if feeStructureId is not null or blank
        if (!currentItem.feeStructureTitle.isNullOrEmpty() && !currentItem.feeStructureId.isNullOrBlank()) {
            holder.textViewFeeStructureTitle.text = "Fee Plan: ${currentItem.feeStructureTitle}"
            holder.textViewFeeStructureTitle.visibility = View.VISIBLE
        } else {
            holder.textViewFeeStructureTitle.visibility = View.GONE
        }

        // FIX: Change condition to check if feeStructureId is not null or blank
        if (!currentItem.feeStructureId.isNullOrBlank() && (currentItem.feeStructureAmount ?: 0.0) > 0) {
            holder.textViewExpectedAmount.text = "Expected: ${currencyFormatter.format(currentItem.feeStructureAmount)}"
            holder.textViewExpectedAmount.visibility = View.VISIBLE
        } else {
            holder.textViewExpectedAmount.visibility = View.GONE
        }

        val status = currentItem.paymentStatus
        holder.textViewPaymentStatus.text = "Status: $status"

        // Resolve the textColorPrimary from the current theme dynamically
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val defaultTextColorPrimary = typedValue.data

        val statusColor: Int = when (status?.uppercase(Locale.US)) {
            "PAID" -> ContextCompat.getColor(context, R.color.green_paid)
            "OVERPAID" -> ContextCompat.getColor(context, R.color.orange_overpaid)
            "DUE", "OVERDUE" -> ContextCompat.getColor(context, R.color.red_due)
            "PARTIAL" -> ContextCompat.getColor(context, R.color.yellow_partial)
            else -> defaultTextColorPrimary // Use the resolved textColorPrimary
        }
        holder.textViewPaymentStatus.setTextColor(statusColor)

        if (!currentItem.paymentRemarks.isNullOrEmpty()) {
            holder.textViewPaymentRemarks.text = "Remarks: ${currentItem.paymentRemarks}"
            holder.textViewPaymentRemarks.visibility = View.VISIBLE
        } else {
            holder.textViewPaymentRemarks.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            listener?.onFeePaymentCardClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return paymentDetailsList.size
    }

    fun updateData(newPaymentDetailsList: List<ParentFeePaymentDetails>) {
        this.paymentDetailsList.clear()
        this.paymentDetailsList.addAll(newPaymentDetailsList)
        notifyDataSetChanged()
    }

    class ParentFeePaymentViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPaymentStudentName: TextView = itemView.findViewById(R.id.textViewPaymentStudentName)
        val textViewPaymentPeriod: TextView = itemView.findViewById(R.id.textViewPaymentPeriod)
        val textViewPaymentDate: TextView = itemView.findViewById(R.id.textViewPaymentDate)
        val textViewPaymentMethod: TextView = itemView.findViewById(R.id.textViewPaymentMethod)
        val textViewFeeStructureTitle: TextView = itemView.findViewById(R.id.textViewFeeStructureTitle)
        val textViewAmountPaid: TextView = itemView.findViewById(R.id.textViewAmountPaid)
        val textViewExpectedAmount: TextView = itemView.findViewById(R.id.textViewExpectedAmount)
        val textViewPaymentStatus: TextView = itemView.findViewById(R.id.textViewPaymentStatus)
        val textViewPaymentRemarks: TextView = itemView.findViewById(R.id.textViewPaymentRemarks)
    }
}
