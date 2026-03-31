package com.choo.moviefinder.domain.model

data class PersonSearchItem(
    val id: Int,
    val name: String,
    val profilePath: String?,
    val knownForDepartment: String,
    val knownForTitles: String
)
