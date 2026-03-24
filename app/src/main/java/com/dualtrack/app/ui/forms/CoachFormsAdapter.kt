package com.dualtrack.app.ui.forms



import android.view.LayoutInflater

import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.dualtrack.app.databinding.ItemCoachFormBinding



class CoachFormsAdapter(

    private val items: List<CoachFormItem>,

    private val onItemClick: (CoachFormItem) -> Unit

) : RecyclerView.Adapter<CoachFormsAdapter.FormViewHolder>() {



    inner class FormViewHolder(val b: ItemCoachFormBinding) :

        RecyclerView.ViewHolder(b.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {

        val binding = ItemCoachFormBinding.inflate(

            LayoutInflater.from(parent.context),

            parent,

            false

        )

        return FormViewHolder(binding)

    }



    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {

        val item = items[position]



        holder.b.tvAthleteEmail.text = item.athleteEmail

        holder.b.tvFormType.text = prettifyFormType(item.formType)

        holder.b.tvStatus.text = "Status: ${prettifyStatus(item.status)}"

        holder.b.tvDueDate.text =

            if (item.dueDate.isBlank()) "Due: —" else "Due: ${item.dueDate}"

        holder.b.tvInstructions.text =

            if (item.requestInstructions.isBlank()) "No instructions"

            else item.requestInstructions



        holder.b.root.setOnClickListener {

            onItemClick(item)

        }

    }



    override fun getItemCount(): Int = items.size



    private fun prettifyFormType(value: String): String {

        return when (value) {

            "academic" -> "Academic Check"

            "wellness" -> "Wellness Check"

            "atRisk" -> "At-Risk Alert"

            "absence" -> "Absence Form"

            "injury" -> "Injury Report"

            else -> value.replaceFirstChar { it.uppercase() }

        }

    }



    private fun prettifyStatus(value: String): String {

        return when (value) {

            "requested" -> "Requested"

            "submitted" -> "Submitted"

            "approved" -> "Approved"

            "needs_attention" -> "Needs Attention"

            "pending" -> "Pending"

            else -> value.replace("_", " ")

        }

    }

}