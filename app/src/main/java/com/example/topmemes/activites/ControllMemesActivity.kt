package com.example.topmemes.activites

import com.example.topmemes.helpers.DatabaseHelper
import android.Manifest
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.topmemes.adapters.MemeAdapter
import com.example.topmemes.data.ImageItem
import com.example.topmemes.databinding.ActivityAddMemesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ControllMemesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemesBinding
    private lateinit var dbHelper: DatabaseHelper
    private val PERMISSION_REQUEST_CODE = 100
    private val PICK_IMAGE_REQUEST = 1

    private val memes = mutableListOf<ImageItem>()
    private lateinit var adapter: MemeAdapter

    // Контракт для выбора изображения
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                if (intent.clipData != null) {
                    handleMultipleImages(intent.clipData!!)
                } else {
                    intent.data?.let { uri ->
                        handleSelectedImage(uri)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        setupUI()
        loadMemesFromDatabase()
    }

    private fun setupUI() {
        adapter = MemeAdapter(
            memes = memes.mapNotNull { it.bitmap },
            onSelectionModeChanged = { isSelectionMode ->
                binding.btnSelectImage.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                binding.btnDelete.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                binding.btnCancelSelection.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            },
            onSelectedCountChanged = { count ->
                binding.btnDelete.text = "Удалить ($count)"
            }
        )

        binding.memesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ControllMemesActivity, 2)
            adapter = this@ControllMemesActivity.adapter
        }

        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.btnDelete.setOnClickListener {
            deleteSelectedMemes()
        }

        binding.btnCancelSelection.setOnClickListener {
            adapter.clearSelection()
        }

        binding.returnBtn.setOnClickListener {
            finish()
        }
    }

    private fun loadMemesFromDatabase() {
        try {
            val memeList = dbHelper.getAllMemes()
            memes.clear()
            memes.addAll(memeList)
            adapter.updateMemes(memes.map { it.bitmap })
        } catch (e: Exception) {
            showToast("Ошибка загрузки: ${e.message}")
            Log.e("LoadMemes", "Error", e)
        }
    }

    private fun deleteSelectedMemes() {
        val selectedPositions = adapter.getSelectedPositions()
        if (selectedPositions.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Удаление мемов")
            .setMessage("Удалить ${selectedPositions.size} мемов?")
            .setPositiveButton("Удалить") { _, _ ->
                try {
                    val idsToDelete = selectedPositions
                        .filter { it in memes.indices }
                        .map { memes[it].id }

                    val deletedCount = dbHelper.deleteMemes(idsToDelete)
                    if (deletedCount > 0) {
                        loadMemesFromDatabase()
                        showToast("Удалено: $deletedCount")
                    } else {
                        showToast("Ошибка удаления")
                    }
                } catch (e: Exception) {
                    showToast("Ошибка: ${e.message}")
                    Log.e("DeleteMemes", "Error", e)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkPermissionAndOpenGallery() {
        when {
            hasGalleryPermission() -> openGalleryForMultipleSelection()
            shouldShowPermissionRationale() -> showPermissionRationale()
            else -> requestGalleryPermission()
        }
    }

    private fun hasGalleryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Нужен доступ")
            .setMessage("Для выбора мемов разрешите доступ к галерее")
            .setPositiveButton("OK") { _, _ -> requestGalleryPermission() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun requestGalleryPermission() {
        ActivityCompat.requestPermissions(
            this,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGalleryForMultipleSelection()
            } else {
                showToast("Доступ отклонён")
            }
        }
    }


    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                if (intent.clipData != null) {
                    // Множественный выбор
                    handleMultipleImages(intent.clipData!!)
                } else {
                    // Единичный выбор
                    intent.data?.let { uri ->
                        handleSelectedImage(uri)
                    }
                }
            }
        }
    }



    private fun handleMultipleImages(clipData: ClipData) {
        val uris = mutableListOf<Uri>()
        for (i in 0 until clipData.itemCount) {
            clipData.getItemAt(i).uri?.let { uris.add(it) }
        }

        if (uris.isNotEmpty()) {
            saveImagesToDatabase(uris)
        }
    }


    private fun saveImagesToDatabase(uris: List<Uri>) {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Сохранение мемов")
            setMessage("Обработка ${uris.size} изображений...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                uris.forEach { uri ->
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream) ?: run {
                            Log.e("ImageLoad", "Failed to decode stream")
                            return@forEach
                        }
                        val compressedBitmap = compressBitmap(bitmap) ?: return@forEach

                        val title = "Мем ${System.currentTimeMillis()}"
                        dbHelper.addMeme(title, compressedBitmap)
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    loadMemesFromDatabase()
                    Toast.makeText(
                        this@ControllMemesActivity,
                        "Добавлено ${uris.size} мемов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ControllMemesActivity,
                        "Ошибка при добавлении: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openGalleryForMultipleSelection() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10) // Лимит выбора
            }
        }

        try {
            pickMultipleImagesLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            showAlternativeImagePickOptions()
        }
    }


    private fun handleSelectedImage(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val compressedBitmap = compressBitmap(bitmap) ?: return

                val title = "Мем ${System.currentTimeMillis()}"
                val id = dbHelper.addMeme(title, compressedBitmap)

                if (id != -1L) {
                    loadMemesFromDatabase()
                    showToast("Мем добавлен!")
                } else {
                    showToast("Ошибка сохранения")
                }
            }
        } catch (e: IOException) {
            showToast("Ошибка загрузки")
            Log.e("ImageLoad", "Error", e)
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            val maxSize = 1024
            val (width, height) = calculateNewDimensions(bitmap, maxSize)
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } catch (e: Exception) {
            Log.e("CompressBitmap", "Error", e)
            null
        }
    }

    private fun calculateNewDimensions(bitmap: Bitmap, maxSize: Int): Pair<Int, Int> {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (ratio > 1) {
            maxSize to (maxSize / ratio).toInt()
        } else {
            (maxSize * ratio).toInt() to maxSize
        }
    }

    private fun showAlternativeImagePickOptions() {
        val options = listOf(
            "Файловый менеджер" to Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            },
            "Google Фото" to Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.google.android.apps.photos")
                type = "image/*"
            }
        )

        val availableOptions = options.filter { resolveActivity(it.second) }

        if (availableOptions.isEmpty()) {
            showToast("Установите приложение для выбора изображений")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите приложение")
            .setItems(availableOptions.map { it.first }.toTypedArray()) { _, which ->
                galleryLauncher.launch(availableOptions[which].second)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun resolveActivity(intent: Intent): Boolean {
        return intent.resolveActivity(packageManager) != null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}