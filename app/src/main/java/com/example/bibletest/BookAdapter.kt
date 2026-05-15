package com.example.bibletest

import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Represents different types of items in the list
sealed class BibleListItem {
    data class BookItem(val name: String) : BibleListItem()
    data class ChapterItem(val bookName: String, val chapter: Int) : BibleListItem()
    data class VerseItem(val bookName: String, val chapter: Int, val verseNumber: Int) : BibleListItem()
}

// Adapter now depends on repository instead of JSON
class BookAdapter(
    private val repository: BibleRepository,
    private val activity: MainActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedBooks = mutableSetOf<String>()
    private val expandedChapters = mutableSetOf<Pair<String, Int>>()

    private var items: List<BibleListItem> = buildListFromData()

    fun rebuildList() {
        items = buildListFromData()
        notifyDataSetChanged()
    }

    fun collapseAll() {
        expandedBooks.clear()
        expandedChapters.clear()
        rebuildList()
    }

    // ---------------- CORE CHANGE ----------------
    private fun buildListFromData(): List<BibleListItem> {
        val result = mutableListOf<BibleListItem>()

        val books = repository.getBooks()

        for (book in books) {
            result.add(BibleListItem.BookItem(book))

            if (expandedBooks.contains(book)) {
                val chapters = repository.getChapters(book)

                for (chapter in chapters) {
                    result.add(BibleListItem.ChapterItem(book, chapter))

                    if (expandedChapters.contains(book to chapter)) {
                        val verses = repository.getVerses(book, chapter)

                        for (v in verses) {
                            result.add(
                                BibleListItem.VerseItem(
                                    book,
                                    chapter,
                                    v.verse
                                )
                            )
                        }
                    }
                }
            }
        }

        return result
    }

    fun getBookPosition(bookName: String): Int {
        return items.indexOfFirst {
            it is BibleListItem.BookItem && it.name == bookName
        }
    }

    fun expandBookAt(bookName: String, chapter: Int) {
        expandedBooks.add(bookName)
        expandedChapters.add(bookName to chapter)
        rebuildList()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is BibleListItem.BookItem -> 0
            is BibleListItem.ChapterItem -> 1
            is BibleListItem.VerseItem -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        return when (viewType) {
            0 -> BookViewHolder(view)
            1 -> ChapterViewHolder(view)
            else -> VerseViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val theme = SettingsManager.getThemeObject(holder.itemView.context)

        when (val item = items[position]) {

            is BibleListItem.BookItem -> {
                val vh = holder as BookViewHolder
                vh.textView.text = item.name
                vh.textView.setTextColor(theme.textColor)
                holder.itemView.setBackgroundColor(theme.backgroundColor)

                holder.itemView.setOnClickListener {
                    if (expandedBooks.contains(item.name)) {
                        expandedBooks.remove(item.name)
                    } else {
                        expandedBooks.add(item.name)
                    }
                    rebuildList()
                }
            }

            is BibleListItem.ChapterItem -> {
                val vh = holder as ChapterViewHolder
                vh.textView.text =
                    holder.itemView.context.getString(R.string.chapter_title, item.chapter)

                vh.textView.setTextColor(theme.textColor)
                holder.itemView.setBackgroundColor(theme.backgroundColor)

                holder.itemView.setOnClickListener {
                    val key = item.bookName to item.chapter
                    if (expandedChapters.contains(key)) {
                        expandedChapters.remove(key)
                    } else {
                        expandedChapters.add(key)
                    }
                    rebuildList()
                }
            }

            is BibleListItem.VerseItem -> {
                val vh = holder as VerseViewHolder

                val isHighlighted =
                    activity.highlightedVerses.contains(
                        Triple(item.bookName, item.chapter, item.verseNumber)
                    )

                val spannable = SpannableString(item.verseNumber.toString())

                if (isHighlighted) {
                    spannable.setSpan(
                        UnderlineSpan(),
                        0,
                        spannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                vh.textView.text = spannable
                vh.textView.setTextColor(theme.textColor)
                holder.itemView.setBackgroundColor(theme.backgroundColor)

                holder.itemView.setOnClickListener {

                    activity.selectedVerse = item.verseNumber
                    activity.displayChapter(
                        item.bookName,
                        item.chapter,
                        item.verseNumber
                    )
                    activity.closeDrawer()
                }
            }
        }
    }

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class VerseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}