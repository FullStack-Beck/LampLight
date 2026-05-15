package com.example.bibletest

import android.content.Context
import android.database.sqlite.SQLiteDatabase

interface BibleRepository {
    fun getBooks(): List<String>
    fun getChapters(book: String): List<Int>
    fun getVerses(book: String, chapter: Int): List<Verse>
    fun getVerseCount(bookName: String, chapter: Int): Int
}

data class BibleVersion(
    val name: String,      // "KJV", "NIV", "ESV"
    val shortName: String, // "kjv"
    val repository: BibleRepository
)


class JsonBibleRepository(
    private val data: BibleData
) : BibleRepository {

    override fun getBooks(): List<String> {
        return data.verses
            .map { it.bookName }
            .distinct()
    }

    override fun getChapters(bookName: String): List<Int> {
        return data.verses
            .filter { it.bookName == bookName }
            .map { it.chapter }
            .distinct()
            .sorted()
    }

    override fun getVerses(bookName: String, chapter: Int): List<Verse> {
        return data.verses
            .filter { it.bookName == bookName && it.chapter == chapter }
            .sortedBy { it.verse }
    }

    override fun getVerseCount(bookName: String, chapter: Int): Int {
        return getVerses(bookName, chapter).size
    }
}

class SqliteBibleRepository(
    private val context: Context,
    private val dbName: String
) : BibleRepository {

    init {
        copyDatabaseIfNeeded(context, dbName)
    }

    private val db: SQLiteDatabase by lazy {
        context.assets.open(dbName).use { }
        // NOTE: if it's in assets, you will COPY it to filesDir first (we'll fix this later)
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    override fun getBooks(): List<String> {
        val cursor = db.rawQuery(
            "SELECT DISTINCT book FROM ${tableName()} ORDER BY book_id",
            null
        )

        val list = mutableListOf<String>()
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list
    }

    private fun getBookId(book: String): Int {
        val cursor = db.rawQuery(
            "SELECT book_id FROM ${tableName()} WHERE book = ? LIMIT 1",
            arrayOf(book)
        )

        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return id
    }

    override fun getChapters(book: String): List<Int> {
        val bookId = getBookId(book)

        val cursor = db.rawQuery(
            """
        SELECT DISTINCT chapter
        FROM ${tableName()}
        WHERE book_id = ?
        ORDER BY chapter
        """.trimIndent(),
            arrayOf(bookId.toString())
        )

        val list = mutableListOf<Int>()
        while (cursor.moveToNext()) {
            list.add(cursor.getInt(0))
        }
        cursor.close()
        return list
    }

    override fun getVerses(book: String, chapter: Int): List<Verse> {
        val bookId = getBookId(book)

        val cursor = db.rawQuery(
            """
        SELECT book, chapter, verse, text
        FROM ${tableName()}
        WHERE book_id = ? AND chapter = ?
        ORDER BY verse
        """.trimIndent(),
            arrayOf(bookId.toString(), chapter.toString())
        )

        val list = mutableListOf<Verse>()

        while (cursor.moveToNext()) {
            list.add(
                Verse(
                    bookName = cursor.getString(0), // "Genesis"
                    book = bookId,
                    chapter = cursor.getInt(1),
                    verse = cursor.getInt(2),
                    text = cursor.getString(3)
                )
            )
        }

        cursor.close()
        return list
    }

    private fun tableName(): String {
        return dbName
            .replace("_bible.sqlite", "")
            .replace("_bible.db", "")
    }

    override fun getVerseCount(bookName: String, chapter: Int): Int {
        val cursor = db.rawQuery(
            """
        SELECT COUNT(*)
        FROM ${tableName()}
        WHERE book_id = ? AND chapter = ?
        """.trimIndent(),
            arrayOf(bookName, chapter.toString())
        )

        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    private fun dbNameWithoutExt(): String {
        return dbName.replace(".sqlite", "").replace(".db", "")
    }

    private fun copyDatabaseIfNeeded(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)

        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()

            context.assets.open(dbName).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}