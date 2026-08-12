package com.coder.app.features.search.domain

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SearchResult(
    @SerialName("title") val title: String,
    @SerialName("url") val url: String,
    @SerialName("snippet") val snippet: String
)
