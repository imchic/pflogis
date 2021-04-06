package kr.co.pflogistics

import android.content.SearchRecentSuggestionsProvider

class SearchHistoryProvider : SearchRecentSuggestionsProvider() {
    companion object {
        val AUTHORITY: String = SearchHistoryProvider::class.java.name
        const val MODE = DATABASE_MODE_QUERIES
    }

    init {
        setupSuggestions(AUTHORITY, MODE)
    }
}