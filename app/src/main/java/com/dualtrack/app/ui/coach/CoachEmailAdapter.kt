package com.dualtrack.app.ui.coach

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemEmailRowBinding

class CoachEmailAdapter(
    private val onClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<CoachEmailAdapter.VH>() {

    private val items: MutableList<String> = mutableListOf()

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
        val email = items[position]
        holder.b.tvEmail.text = email

        holder.itemView.setOnClickListener {
            onClick?.invoke(email)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
