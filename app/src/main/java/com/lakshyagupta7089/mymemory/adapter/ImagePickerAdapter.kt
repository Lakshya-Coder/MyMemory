package com.lakshyagupta7089.mymemory.adapter

import android.content.Context
import android.media.Image
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.lakshyagupta7089.mymemory.R
import com.lakshyagupta7089.mymemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {
    interface ImageClickListener {
        fun onPlaceholderClicked()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImagePickerAdapter.ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(
            R.layout.card_image,
            parent,
            false
        )

        val cardWidth = parent.width / boardSize.getWidth() - (2 * 10)
        val cardHeight = parent.height / boardSize.getHeight()  - (2 * 10)
        val cardSideLength = min(cardWidth, cardHeight)

        val marginLayoutInflater = view.findViewById<ImageView>(
            R.id.ivCustomeImage
        ).layoutParams as ViewGroup.MarginLayoutParams

        marginLayoutInflater.width = cardSideLength
        marginLayoutInflater.height = cardSideLength

        marginLayoutInflater.setMargins(
            10,
            10,
            10,
            10
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagePickerAdapter.ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getNumPairs()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomeImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImage.setOnClickListener {
                imageClickListener.onPlaceholderClicked()
            }
        }

    }
}
