package com.dualtrack.app.ui.updates

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.R
import com.google.android.material.card.MaterialCardView

class TeamUpdatesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TeamUpdate>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CARD = 1
    }

    fun submit(newItems: List<TeamUpdate>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == "header") TYPE_HEADER else TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(createHeaderTextView(parent.context))
        } else {
            UpdateViewHolder(createCardView(parent.context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is UpdateViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun createHeaderTextView(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dp(context, 8)
                setMargins(0, margin, 0, dp(context, 8))
            }
            setTextColor(ContextCompat.getColor(context, R.color.dt_white))
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }
    }

    private fun createCardView(context: Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dp(context, 8)
                setMargins(0, margin, 0, margin)
            }

            radius = dp(context, 16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            setContentPadding(0, 0, 0, 0)
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.dt_white_60)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }

        val badge = TextView(context).apply {
            id = android.view.View.generateViewId()
            tag = "badge"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
            setTextColor(ContextCompat.getColor(context, R.color.dt_white))
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        val title = TextView(context).apply {
            id = android.view.View.generateViewId()
            tag = "title"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(context, 14), 0, 0)
            setTextColor(ContextCompat.getColor(context, R.color.dt_white))
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }

        val subtitle = TextView(context).apply {
            id = android.view.View.generateViewId()
            tag = "subtitle"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(context, 8), 0, 0)
            setTextColor(ContextCompat.getColor(context, R.color.dt_white_60))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(0f, 1.1f)
        }

        container.addView(badge)
        container.addView(title)
        container.addView(subtitle)
        card.addView(container)

        return card
    }

    private fun badgeColor(context: Context, text: String): Int {
        return when {
            text.contains("PAST EVENT", ignoreCase = true) ->
                ContextCompat.getColor(context, android.R.color.holo_red_light)

            text.contains("UPCOMING EVENT", ignoreCase = true) ->
                ContextCompat.getColor(context, android.R.color.holo_green_light)

            text.contains("ANNOUNCEMENT IN PROGRESS", ignoreCase = true) ->
                ContextCompat.getColor(context, android.R.color.holo_green_light)

            text.contains("PAST ANNOUNCEMENT", ignoreCase = true) ->
                ContextCompat.getColor(context, android.R.color.holo_orange_light)

            text.contains("ANNOUNCEMENT", ignoreCase = true) ->
                ContextCompat.getColor(context, android.R.color.holo_orange_light)

            else ->
                ContextCompat.getColor(context, android.R.color.darker_gray)
        }
    }

    private fun extractBadge(text: String): Pair<String, String> {
        val parts = text.split("•", limit = 2).map { it.trim() }
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            "" to text
        }
    }

    inner class HeaderViewHolder(private val textView: TextView) :
        RecyclerView.ViewHolder(textView) {
        fun bind(item: TeamUpdate) {
            textView.text = item.title
        }
    }

    inner class UpdateViewHolder(private val card: MaterialCardView) :
        RecyclerView.ViewHolder(card) {

        private val badge: TextView = findTaggedView(card, "badge")
        private val title: TextView = findTaggedView(card, "title")
        private val subtitle: TextView = findTaggedView(card, "subtitle")

        fun bind(item: TeamUpdate) {
            title.text = item.title

            val rawSubtitle = item.subtitle.orEmpty()
            val (badgeText, bodyText) = extractBadge(rawSubtitle)

            if (badgeText.isBlank()) {
                badge.text = item.type.uppercase()
                badge.setBackgroundColor(badgeColor(card.context, item.type))
                subtitle.text = rawSubtitle
            } else {
                badge.text = badgeText
                badge.setBackgroundColor(badgeColor(card.context, badgeText))
                subtitle.text = bodyText
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findTaggedView(root: ViewGroup, tag: String): T {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child.tag == tag) return child as T
            if (child is ViewGroup) {
                try {
                    return findTaggedView(child, tag)
                } catch (_: Exception) {
                }
            }
        }
        throw IllegalStateException("View with tag '$tag' not found")
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
