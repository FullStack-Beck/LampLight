package com.example.bibletest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import android.widget.TextView
import android.view.Gravity

class ThemeAdapter(
    private val themes: List<AppTheme>,
    private val onThemeSelected: (AppTheme) -> Unit,
    private val onAddTheme: () -> Unit,
    private val onDeleteTheme: ((AppTheme) -> Unit)? = null
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    override fun getItemCount(): Int = themes.size + 1 // themes + add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {

        val card = CardView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(180, 180).apply {
                marginEnd = 16
            }
            radius = 20f
            cardElevation = 6f
        }

        val text = TextView(parent.context).apply {
            gravity = Gravity.CENTER
            textSize = 14f
            setPadding(12, 12, 12, 12)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        card.addView(text)

        return ThemeViewHolder(card, text)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {

        val card = holder.card
        val text = holder.textView

        if (position < themes.size) {
            val theme = themes[position]

            text.text = theme.name
            text.setTextColor(theme.textColor)
            card.setCardBackgroundColor(theme.backgroundColor)

            card.setOnClickListener { onThemeSelected(theme) }

            card.setOnLongClickListener {
                if (onDeleteTheme != null && !isPrebuiltTheme(theme)) {
                    onDeleteTheme.invoke(theme)
                    true
                } else false
            }

        } else {
            text.text = "+"
            text.setTextColor(0xFF000000.toInt())
            card.setCardBackgroundColor(0xFFCCCCCC.toInt())

            card.setOnClickListener { onAddTheme() }
            card.setOnLongClickListener(null)
        }
    }

    class ThemeViewHolder(
        val card: CardView,
        val textView: TextView
    ) : RecyclerView.ViewHolder(card)

    private fun isPrebuiltTheme(theme: AppTheme): Boolean {
        return theme.name == "Light" || theme.name == "Dark"
    }
}
