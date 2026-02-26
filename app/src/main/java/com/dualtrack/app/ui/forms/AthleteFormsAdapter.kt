package com.dualtrack.app.ui.forms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemAthleteFormBinding

class AthleteFormsAdapter(
    private val items: List<FormItem>
) : RecyclerView.Adapter<AthleteFormsAdapter.FormViewHolder>() {

    inner class FormViewHolder(val b: ItemAthleteFormBinding) :
        RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val binding = ItemAthleteFormBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FormViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        val item = items[position]
        holder.b.tvFormType.text = item.formType
        holder.b.tvStatus.text = item.status
        holder.b.tvDate.text = item.createdAt
    }

    override fun getItemCount(): Int = items.size
}