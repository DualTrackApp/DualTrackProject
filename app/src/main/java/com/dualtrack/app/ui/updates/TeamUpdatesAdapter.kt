package com.dualtrack.app.ui.updates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemTeamUpdateBinding

class TeamUpdatesAdapter : RecyclerView.Adapter<TeamUpdatesAdapter.VH>() {

    private var items: List<TeamUpdate> = emptyList()

    fun submit(list: List<TeamUpdate>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemTeamUpdateBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TeamUpdate) {
            b.tvTitle.text = item.title
            b.tvSubtitle.text = item.subtitle
            b.tvType.text = if (item.type == "event") "EVENT" else "ANNOUNCEMENT"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTeamUpdateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}