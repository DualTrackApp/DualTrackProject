package com.dualtrack.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.R

class DayEventsAdapter : ListAdapter<CalendarEvent, DayEventsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_event, parent, false)
        return VH(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val tvTitle: TextView = root.findViewById(R.id.tvTitle)
        private val tvDetails: TextView = root.findViewById(R.id.tvDetails)

        fun bind(item: CalendarEvent) {
            tvTitle.text = item.title
            tvDetails.text = item.details
        }
    }
}
