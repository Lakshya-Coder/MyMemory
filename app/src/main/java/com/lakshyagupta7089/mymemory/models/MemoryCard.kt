package com.lakshyagupta7089.mymemory.models

data class MemoryCard(
    val id: Int,
    val imageUrl: String? = null,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false,
)