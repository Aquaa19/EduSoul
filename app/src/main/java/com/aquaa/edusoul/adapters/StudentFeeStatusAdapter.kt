package com.aquaa.edusoul.adapters

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
import com.aquaa.edusoul.models.StudentFeeStatus
import com.aquaa.edusoul.models.FeePayment
import java.text.NumberFormat
import java.util.Locale
import android.util.TypedValue

/*
 * StudentFeeStatusAdapter: RecyclerView adapter for displaying student fee status.
 * Migrated to Kotlin.
 */
class StudentFeeStatusAdapter(
    private val context: Context,
    private var studentFeeStatusList: java.util.ArrayList<StudentFeeStatus>
) : RecyclerView.Adapter<StudentFeeStatusAdapter.StudentFeeStatusViewHolder>() {

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): StudentFeeStatusViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student_fee_status, parent, false)
        return StudentFeeStatusViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: StudentFeeStatusViewHolder, position: Int) {
        val status = studentFeeStatusList[position]

        holder.textViewStudentName.text = status.studentName
        holder.textViewBatchName.text = status.batchName
        holder.textViewAmountDue.text = currencyFormatter.format(status.totalAmountDue)
        holder.textViewAmountPaid.text = currencyFormatter.format(status.totalAmountPaid)

        if (status.lastPaymentDate != null && status.lastPaymentDate!!.isNotEmpty()) {
            holder.textViewLastPaymentDate.text = "Last Paid: ${status.lastPaymentDate}"
            holder.textViewLastPaymentDate.visibility = View.VISIBLE
        } else {
            holder.textViewLastPaymentDate.visibility = View.GONE
        }

        holder.textViewStatus.text = status.status

        // Directly set the color based on status, without relying on defaultTextColorPrimary fallback
        val statusColor: Int = when (status.status.uppercase(Locale.US)) {
            FeePayment.STATUS_PAID -> ContextCompat.getColor(context, R.color.green_paid)
            FeePayment.STATUS_PARTIALLY_PAID -> ContextCompat.getColor(context, R.color.yellow_partial)
            FeePayment.STATUS_DUE -> ContextCompat.getColor(context, R.color.red_due)
            FeePayment.STATUS_OVERPAID -> ContextCompat.getColor(context, R.color.orange_overpaid)
            else -> {
                // Fallback to a neutral color if status is unrecognized, e.g., a dark gray or a default from theme
                // Resolve the textColorPrimary from the current theme dynamically as a last resort
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                typedValue.data
            }
        }
        holder.textViewStatus.setTextColor(statusColor)
    }

    override fun getItemCount(): Int {
        return studentFeeStatusList.size
    }

    fun updateData(newStudentFeeStatusList: List<StudentFeeStatus>) {
        this.studentFeeStatusList.clear()
        this.studentFeeStatusList.addAll(newStudentFeeStatusList)
        notifyDataSetChanged()
    }

    class StudentFeeStatusViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentName)
        val textViewBatchName: TextView = itemView.findViewById(R.id.textViewStudentBatch)
        val textViewAmountDue: TextView = itemView.findViewById(R.id.textViewAmountDue)
        val textViewAmountPaid: TextView = itemView.findViewById(R.id.textViewAmountPaid)
        val textViewLastPaymentDate: TextView = itemView.findViewById(R.id.textViewLastPaymentDate)
        val textViewStatus: TextView = itemView.findViewById(R.id.textViewPaymentStatus)
    }
}
