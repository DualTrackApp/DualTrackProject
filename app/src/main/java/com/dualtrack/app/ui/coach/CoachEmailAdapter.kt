package com.dualtrack.app.ui.coach

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemEmailRowBinding

class CoachEmailAdapter(
    private val items: MutableList<String> = mutableListOf()
) : RecyclerView.Adapter<CoachEmailAdapter.VH>() {

    class VH(val b: ItemEmailRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemEmailRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvEmail.text = items[position]
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
