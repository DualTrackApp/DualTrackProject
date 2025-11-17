package com.dualtrack.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.databinding.ItemHomeSquareCardBinding

class HomeCardAdapter(
    private val items: List<HomeCard>,
    private val onClick: (HomeCard) -> Unit
) : RecyclerView.Adapter<HomeCardAdapter.HomeCardViewHolder>() {

    inner class HomeCardViewHolder(
        val binding: ItemHomeSquareCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: HomeCard) = with(binding) {
            tvTitle.text = card.title

            if (card.subtitle.isNullOrBlank()) {
                tvSubtitle.visibility = View.GONE
            } else {
                tvSubtitle.visibility = View.VISIBLE
                tvSubtitle.text = card.subtitle
            }

            if (card.iconResId != null) {
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageResource(card.iconResId)
            } else {
                ivIcon.visibility = View.GONE
            }

            root.setOnClickListener { onClick(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHomeSquareCardBinding.inflate(inflater, parent, false)
        return HomeCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
