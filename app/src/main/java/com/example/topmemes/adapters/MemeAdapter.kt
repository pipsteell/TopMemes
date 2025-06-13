package com.example.topmemes.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.topmemes.R

class MemeAdapter(
    private var memes:  List<Bitmap>,
    private val onSelectionModeChanged: (Boolean) -> Unit,
    private val onSelectedCountChanged: (Int) -> Unit
) : RecyclerView.Adapter<MemeAdapter.MemeViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    private var isSelectionMode = false

    inner class MemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memeImage: ImageView = itemView.findViewById(R.id.memeImage)
        val selectionIndicator: ImageView = itemView.findViewById(R.id.selectionIndicator)
        val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)

        init {
            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(adapterPosition)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    onSelectionModeChanged(true)
                }
                toggleSelection(adapterPosition)
                true
            }
        }

        private fun toggleSelection(position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
                if (selectedItems.isEmpty()) {
                    isSelectionMode = false
                    onSelectionModeChanged(false)
                }
            } else {
                selectedItems.add(position)
            }
            updateItemView(position)
            onSelectedCountChanged(selectedItems.size)
        }

        private fun updateItemView(position: Int) {
            val isSelected = selectedItems.contains(position)
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meme, parent, false)
        return MemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemeViewHolder, position: Int) {
        holder.memeImage.setImageBitmap(memes[position])

        // Обновляем состояние выделения
        holder.selectionIndicator.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE
        holder.selectionOverlay.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = memes.size

    fun getSelectedPositions(): Set<Int> = selectedItems.toSet()

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionModeChanged(false)
        onSelectedCountChanged(0)
    }

    fun updateMemes(newMemes: List<Bitmap>) {
        memes = newMemes
        clearSelection()
    }
}