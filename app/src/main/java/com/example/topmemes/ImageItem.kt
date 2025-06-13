package com.example.topmemes.data

import android.graphics.Bitmap
import java.util.Date

data class ImageItem(
    val id: Int,         // Уникальный идентификатор
    val title: String,   // Название/описание мема
    val bitmap: Bitmap,   // Изображение
    val timestamp: Date = Date() // Время создания (опционально)
)
