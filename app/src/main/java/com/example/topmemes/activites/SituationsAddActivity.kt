package com.example.topmemes.activites

import Situation
import SituationDatabaseHelper
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.topmemes.R
import com.example.topmemes.adapters.SituationAdapter
import com.example.topmemes.databinding.ActivityAddSituationsBinding

class SituationsAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSituationsBinding
    private lateinit var etSituation: EditText
    private lateinit var btnSave: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SituationAdapter
    private lateinit var dbHelper: SituationDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_situations)
        binding = ActivityAddSituationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etSituation = findViewById(R.id.etSituation)
        btnSave = findViewById(R.id.btnSave)
        recyclerView = findViewById(R.id.situationsRecyclerView)

        dbHelper = SituationDatabaseHelper(this)
        setupRecyclerView()

        btnSave.setOnClickListener {
            saveSituation()
        }

        binding.returnBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SituationAdapter(
            situations = dbHelper.getAllSituations(),
            onEditClick = { situation -> showEditDialog(situation) },
            onDeleteClick = { situation -> deleteSituation(situation) }
        )
        recyclerView.adapter = adapter
    }

    private fun saveSituation() {
        val text = etSituation.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show()
            return
        }

        val id = dbHelper.addSituation(text)
        if (id != -1L) {
            Toast.makeText(this, "Situation saved", Toast.LENGTH_SHORT).show()
            etSituation.text.clear()
            refreshSituations()
        } else {
            Toast.makeText(this, "Error saving situation", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshSituations() {
        if (::adapter.isInitialized) {
            adapter.updateSituations(dbHelper.getAllSituations())
        }
    }

    private fun showEditDialog(situation: Situation) {
        val editText = EditText(this).apply {
            setText(situation.text)
        }

        AlertDialog.Builder(this@SituationsAddActivity)
            .setTitle("Edit Situation")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val rowsAffected = dbHelper.updateSituation(situation.id, newText)
                    if (rowsAffected > 0) {
                        refreshSituations()
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSituation(situation: Situation) {
        AlertDialog.Builder(this)
            .setTitle("Delete Situation")
            .setMessage("Are you sure you want to delete this?")
            .setPositiveButton("Delete") { _, _ ->
                val rowsDeleted = dbHelper.deleteSituation(situation.id)
                if (rowsDeleted > 0) {
                    refreshSituations()
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}