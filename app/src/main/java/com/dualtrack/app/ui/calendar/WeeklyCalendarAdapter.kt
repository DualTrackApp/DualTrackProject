package com.dualtrack.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemHomeSquareCardBinding

class WeeklyCalendarAdapter(
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<WeeklyCalendarAdapter.VH>() {

    private val days = listOf(
        DayUi(1, "Mon", "Add events"),
        DayUi(2, "Tue", "Add events"),
        DayUi(3, "Wed", "Add events"),
        DayUi(4, "Thu", "Add events"),
        DayUi(5, "Fri", "Add events"),
        DayUi(6, "Sat", "Add events"),
        DayUi(7, "Sun", "Add events")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHomeSquareCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onDayClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    class VH(
        private val b: ItemHomeSquareCardBinding,
        private val onDayClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: DayUi) {
            b.tvTitle.text = item.title
            b.tvSubtitle.text = item.subtitle
            b.root.setOnClickListener { onDayClick(item.isoDay) }
        }
    }

    data class DayUi(
        val isoDay: Int,
        val title: String,
        val subtitle: String
    )
}
