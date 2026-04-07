package com.dualtrack.app.ui.coach

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemEmailRowBinding

class CoachPlayerAdapter(
    private val onClick: (RosterAthlete) -> Unit
) : RecyclerView.Adapter<CoachPlayerAdapter.VH>() {

    private val items: MutableList<RosterAthlete> = mutableListOf()

    inner class VH(val b: ItemEmailRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEmailRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val athlete = items[position]
        holder.b.tvName.text = athlete.fullName()
        holder.b.tvEmail.text = athlete.email

        holder.itemView.setOnClickListener {
            onClick.invoke(athlete)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<RosterAthlete>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}


