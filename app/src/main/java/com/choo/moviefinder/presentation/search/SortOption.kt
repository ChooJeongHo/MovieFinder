package com.choo.moviefinder.presentation.search

enum class SortOption(val apiValue: String) {
    POPULARITY_DESC("popularity.desc"),
    VOTE_AVERAGE_DESC("vote_average.desc"),
    RELEASE_DATE_DESC("release_date.desc"),
    REVENUE_DESC("revenue.desc")
}
