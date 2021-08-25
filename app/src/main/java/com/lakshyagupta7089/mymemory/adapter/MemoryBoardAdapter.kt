package com.lakshyagupta7089.mymemory.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.lakshyagupta7089.mymemory.R
import com.lakshyagupta7089.mymemory.models.BoardSize
import com.lakshyagupta7089.mymemory.models.MemoryCard
import com.squareup.picasso.Picasso

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
)
    : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
    }

    interface CardClickListener {
        fun onCardClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)

        val cardSideLength = kotlin.math.min(cardWidth, cardHeight)

        val view = LayoutInflater.from(context).inflate(
            R.layout.memory_card,
            parent,
            false
        )

        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength

        layoutParams.setMargins(
            MARGIN_SIZE,
            MARGIN_SIZE,
            MARGIN_SIZE,
            MARGIN_SIZE
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = boardSize.numCards

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard = cards[position]

            if (memoryCard.isFaceUp) {
                if (memoryCard.imageUrl != null) {
                    Picasso
                        .get()
                        .load(memoryCard.imageUrl)
                        .placeholder(R.drawable.ic_image)
                        .into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.id)
                }
            } else {
                imageButton.setImageResource(R.drawable.bamboo)
            }

            imageButton.alpha = if (memoryCard.isMatched) {
                0.4F
            } else {
                1.0F
            }

            val colorStateList = if (memoryCard.isMatched) ContextCompat.getColorStateList(
                context,
                R.color.color_gray
            ) else null

            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener {
                cardClickListener.onCardClick(position)
            }
        }
    }
}
