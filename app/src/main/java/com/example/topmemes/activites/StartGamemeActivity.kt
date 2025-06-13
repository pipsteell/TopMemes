package com.example.topmemes.activites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmemes.R
import com.example.topmemes.databinding.ActivityGameRoomBinding
class StartGamemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameRoomBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_room)
        binding = ActivityGameRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.returnBtn.setOnClickListener {
            finish()
        }
    }


}