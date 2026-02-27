package com.dualtrack.app.ui.forms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemAthleteFormBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AthleteFormsAdapter(
    private val items: List<FormItem>
) : RecyclerView.Adapter<AthleteFormsAdapter.VH>() {

    inner class VH(val b: ItemAthleteFormBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAthleteFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvFormType.text = item.formType
        holder.b.tvStatus.text = item.status

        val ts = item.createdAt?.toDate()
        val formatted = if (ts != null) {
            SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US).format(ts)
        } else ""

        holder.b.tvCreatedAt.text = formatted
    }

    override fun getItemCount(): Int = items.size
}