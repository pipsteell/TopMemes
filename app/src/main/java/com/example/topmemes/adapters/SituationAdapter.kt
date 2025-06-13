package com.example.topmemes.adapters

import Situation
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.topmemes.R

class SituationAdapter(
    private var situations: List<Situation>,
    private val onEditClick: (Situation) -> Unit,
    private val onDeleteClick: (Situation) -> Unit
) : RecyclerView.Adapter<SituationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvSituationText)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_situation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val situation = situations[position]
        holder.tvText.text = situation.text

        holder.btnEdit.setOnClickListener {
            onEditClick(situation)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(situation)
        }
    }

    override fun getItemCount() = situations.size

    fun updateSituations(newSituations: List<Situation>) {
        situations = newSituations
        notifyDataSetChanged()
    }


}