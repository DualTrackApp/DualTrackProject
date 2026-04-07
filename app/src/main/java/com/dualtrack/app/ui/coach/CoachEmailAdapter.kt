package com.dualtrack.app.ui.coach

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
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
        val raw = items[position]
        val parts = raw.split("||")

        val topRaw = parts.getOrNull(0).orEmpty()
        val bottom = parts.getOrNull(1).orEmpty()

        val status = when {
            topRaw.endsWith(" - Green") -> "Green"
            topRaw.endsWith(" - Yellow") -> "Yellow"
            topRaw.endsWith(" - Red") -> "Red"
            else -> ""
        }

        val top = topRaw
            .removeSuffix(" - Green")
            .removeSuffix(" - Yellow")
            .removeSuffix(" - Red")
            .ifBlank { "Athlete" }

        holder.b.tvName.text = top
        holder.b.tvEmail.text = bottom.ifBlank { raw }

        if (status.isBlank() || top.startsWith("No ")) {
            holder.b.vStatusDot.visibility = View.GONE
        } else {
            holder.b.vStatusDot.visibility = View.VISIBLE
            holder.b.vStatusDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    when (status) {
                        "Green" -> Color.parseColor("#4CAF50")
                        "Yellow" -> Color.parseColor("#FFC107")
                        "Red" -> Color.parseColor("#F44336")
                        else -> Color.GRAY
                    }
                )
            }
        }

        holder.itemView.setOnClickListener {
            onClick?.invoke(raw)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}



