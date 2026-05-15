package com.example.bibletest

object VersionManager {

    private val versions = mutableListOf<BibleVersion>()

    private var current: BibleVersion? = null

    fun register(version: BibleVersion) {
        versions.add(version)
        if (current == null) current = version
    }

    fun getVersions(): List<BibleVersion> = versions

    fun getCurrent(): BibleVersion = current!!

    fun setCurrent(version: BibleVersion) {
        current = version
    }
}