package com.lakshyagupta7089.mymemory.models

import com.lakshyagupta7089.mymemory.utils.DEFAULT_ICON

class MemoryGame(
    private val boardSize: BoardSize,
    private val customImages: List<String>?
) {
    val cards: List<MemoryCard>
    var numPairFound = 0

    private var numCardFlip = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if (customImages == null) {
            val choseImages = DEFAULT_ICON.shuffled().take(boardSize.getNumPairs())
            val randomizeImages = (choseImages + choseImages).shuffled()

            cards = randomizeImages.map {
                MemoryCard(it)
            }
        } else {
            val randomizeImages = (customImages + customImages).shuffled()
            cards = randomizeImages.map {
                MemoryCard(it.hashCode(), it)
            }
        }
    }

    fun flipCard(position: Int): Boolean {
        numCardFlip++
        val card = cards[position]

        var foundMatch = false

        if (indexOfSingleSelectedCard == null) {
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)

            indexOfSingleSelectedCard = null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].id != cards[position2].id) {
            return false
        }

        cards[position1].isMatched = true
        cards[position2].isMatched = true

        numPairFound++

        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairFound == boardSize.getNumPairs()
    }

    fun isCardIsFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlip / 2
    }
}