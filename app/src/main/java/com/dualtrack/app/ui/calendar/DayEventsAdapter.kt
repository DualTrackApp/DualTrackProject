package com.dualtrack.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemDayEventBinding

class DayEventsAdapter(
    private val onClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<DayEventsAdapter.VH>() {

    private var items: List<CalendarEvent> = emptyList()

    inner class VH(val b: ItemDayEventBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDayEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        val titleText = buildString {
            if (item.time.isNotBlank()) append("${item.time} - ")
            append(item.title)
        }

        holder.b.tvTitle.text = titleText
        holder.b.tvDetails.text =
            if (item.details.isNotBlank()) item.details else "No additional details"

        holder.b.root.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<CalendarEvent>) {
        items = list
        notifyDataSetChanged()
    }
}
