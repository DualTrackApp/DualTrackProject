package com.dualtrack.app.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.R
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

            tvTitle.maxLines = 2
            tvSubtitle.maxLines = 2
            tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.dt_white))
            tvSubtitle.setTextColor(ContextCompat.getColor(root.context, R.color.dt_white_60))

            val isPhotoCard = card.imageResId != null

            if (isPhotoCard) {
                ivIcon.setImageResource(card.imageResId!!)
                ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                ivIcon.setPadding(0, 0, 0, 0)
                ivIcon.imageTintList = null
                ivIcon.backgroundTintList = null
                viewImageOverlay.visibility = View.VISIBLE
                tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            } else {
                ivIcon.setImageResource(card.iconResId ?: android.R.drawable.ic_menu_gallery)
                ivIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                ivIcon.setPadding(dp(6), dp(6), dp(6), dp(6))
                ivIcon.setBackgroundColor(Color.parseColor("#1AFFFFFF"))
                ivIcon.imageTintList = ColorStateList.valueOf(iconTintFor(card.title))
                viewImageOverlay.visibility = View.GONE
                tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }

            root.setOnClickListener { onClick(card) }
        }

        private fun iconTintFor(title: String): Int {
            return when (title) {
                "Announcements" -> Color.parseColor("#FFD166")
                "Academic Check" -> Color.parseColor("#8ECAE6")
                "Wellness Check" -> Color.parseColor("#90EE90")
                "At-Risk Alert" -> Color.parseColor("#FF8C42")
                "No requested forms" -> Color.parseColor("#BDBDBD")
                "Absence Form" -> Color.parseColor("#8ECAE6")
                "My Submissions" -> Color.parseColor("#FFD166")
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" -> Color.parseColor("#90EE90")
                else -> Color.parseColor("#FFFFFF")
            }
        }

        private fun dp(value: Int): Int {
            return (value * binding.root.resources.displayMetrics.density).toInt()
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
