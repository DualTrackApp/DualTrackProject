package com.dualtrack.app.ui.forms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemCoachFormBinding
import com.google.firebase.firestore.FirebaseFirestore

class CoachFormsAdapter(
    private val items: List<CoachFormItem>
) : RecyclerView.Adapter<CoachFormsAdapter.FormViewHolder>() {

    inner class FormViewHolder(val b: ItemCoachFormBinding) :
        RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val binding = ItemCoachFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FormViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        val item = items[position]

        holder.b.tvAthleteEmail.text = item.athleteEmail
        holder.b.tvFormType.text = item.formType
        holder.b.tvStatus.text = item.status

        holder.b.btnApprove.setOnClickListener {
            update(item.id, "approved", "Approved by coach")
        }

        holder.b.btnNeedsAttention.setOnClickListener {
            update(item.id, "needs_attention", "Needs attention")
        }
    }

    override fun getItemCount(): Int = items.size

    private fun update(formId: String, status: String, note: String) {
        FirebaseFirestore.getInstance()
            .collection("forms")
            .document(formId)
            .update(
                mapOf(
                    "status" to status,
                    "coachNote" to note
                )
            )
    }
}