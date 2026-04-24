package com.dualtrack.app.ui.updates

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.R
import com.google.android.material.card.MaterialCardView

class TeamUpdatesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TeamUpdate>()

    fun submit(newItems: List<TeamUpdate>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == "header") 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            HeaderViewHolder(createHeaderView(parent.context))
        } else {
            UpdateViewHolder(createCardView(parent.context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder) holder.bind(item)
        if (holder is UpdateViewHolder) holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    private fun createHeaderView(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 8)
                bottomMargin = dp(context, 10)
            }
            setTextColor(ContextCompat.getColor(context, R.color.dt_white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun createCardView(context: Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 14)
            }
            radius = dp(context, 18).toFloat()
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.dt_white_60)
            setCardBackgroundColor(Color.parseColor("#143A8A"))
            cardElevation = dp(context, 3).toFloat()
            useCompatPadding = true
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }

        val badge = TextView(context).apply {
            id = View.generateViewId()
            tag = "badge"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6))
            gravity = Gravity.CENTER
        }

        val title = TextView(context).apply {
            id = View.generateViewId()
            tag = "title"
            setTextColor(ContextCompat.getColor(context, R.color.dt_white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(context, 12), 0, 0)
        }

        val subtitle = TextView(context).apply {
            id = View.generateViewId()
            tag = "subtitle"
            setTextColor(ContextCompat.getColor(context, R.color.dt_white_60))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(context, 8), 0, 0)
            setLineSpacing(0f, 1.1f)
        }

        container.addView(badge)
        container.addView(title)
        container.addView(subtitle)
        card.addView(container)
        return card
    }

    inner class HeaderViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: TeamUpdate) {
            textView.text = item.title
        }
    }

    inner class UpdateViewHolder(private val card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        private val badge: TextView = findTagged(card, "badge")
        private val title: TextView = findTagged(card, "title")
        private val subtitle: TextView = findTagged(card, "subtitle")

        fun bind(item: TeamUpdate) {
            title.text = item.title

            val fullSubtitle = item.subtitle.orEmpty()
            val cleanedSubtitle = fullSubtitle
                .replace("IN PROGRESS • ", "")
                .replace("UPCOMING • ", "")
                .replace("PAST • ", "")

            subtitle.text = cleanedSubtitle

            when {
                item.type == "announcement" && fullSubtitle.startsWith("IN PROGRESS") -> {
                    badge.text = "ANNOUNCEMENT IN PROGRESS"
                    badge.setTextColor(Color.parseColor("#0F2C6E"))
                    badge.setBackgroundColor(Color.parseColor("#FFD166"))
                }

                item.type == "announcement" && fullSubtitle.startsWith("UPCOMING") -> {
                    badge.text = "UPCOMING ANNOUNCEMENT"
                    badge.setTextColor(Color.parseColor("#0F2C6E"))
                    badge.setBackgroundColor(Color.parseColor("#FFD166"))
                }

                item.type == "announcement" -> {
                    badge.text = "PAST ANNOUNCEMENT"
                    badge.setTextColor(Color.parseColor("#0F2C6E"))
                    badge.setBackgroundColor(Color.parseColor("#FFD166"))
                }

                fullSubtitle.startsWith("IN PROGRESS") -> {
                    badge.text = "EVENT IN PROGRESS"
                    badge.setTextColor(Color.parseColor("#0E3B1F"))
                    badge.setBackgroundColor(Color.parseColor("#8FD694"))
                }

                fullSubtitle.startsWith("UPCOMING") -> {
                    badge.text = "UPCOMING EVENT"
                    badge.setTextColor(Color.parseColor("#0E3B1F"))
                    badge.setBackgroundColor(Color.parseColor("#8FD694"))
                }

                else -> {
                    badge.text = "PAST EVENT"
                    badge.setTextColor(Color.WHITE)
                    badge.setBackgroundColor(Color.parseColor("#C94C4C"))
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : View> findTagged(root: View, tag: String): T {
        if (root.tag == tag) return root as T
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = tryFindTagged<T>(root.getChildAt(i), tag)
                if (found != null) return found
            }
        }
        throw IllegalStateException("View with tag $tag not found")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : View> tryFindTagged(root: View, tag: String): T? {
        if (root.tag == tag) return root as T
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = tryFindTagged<T>(root.getChildAt(i), tag)
                if (found != null) return found
            }
        }
        return null
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}