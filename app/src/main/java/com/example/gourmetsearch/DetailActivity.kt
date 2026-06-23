package com.example.gourmetsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import coil.load
import com.example.gourmetsearch.databinding.ActivityDetailBinding
import com.google.gson.Gson

class DetailActivity : ComponentActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. ListActivityから渡されたデータ（JSON）を受け取る
        val jsonString = intent.getStringExtra("SHOP_DATA_JSON")
        if (jsonString != null) {
            // JSONを Shop クラスの形に戻す
            val shop = Gson().fromJson(jsonString, Shop::class.java)

            // 2. 画面のパーツにデータをセットする
            binding.detailNameText.text = shop.name
            binding.detailAddressText.text = shop.address
            binding.detailOpenText.text = shop.open

            // Coilを使って大きな画像を読み込む
            binding.detailImage.load(shop.photo.mobile.l) {
                crossfade(true)
            }
        }

        // 3. 戻るボタンの処理
        binding.backButton.setOnClickListener {
            finish() // 現在の画面（詳細画面）を閉じて、前の画面（一覧画面）に戻る
        }
    }
}