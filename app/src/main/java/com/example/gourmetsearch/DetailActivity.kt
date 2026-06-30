package com.example.gourmetsearch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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

        val jsonString = intent.getStringExtra("SHOP_DATA_JSON")
        if (jsonString != null) {
            val shop = Gson().fromJson(jsonString, Shop::class.java)

            binding.detailNameText.text = shop.name
            binding.detailAddressText.text = shop.address
            binding.detailOpenText.text = shop.open

            // 予算表示（average があればそれを、なければ range 名称を表示）
            val budgetText = when {
                !shop.budget?.average.isNullOrEmpty() -> shop.budget!!.average
                !shop.budget?.name.isNullOrEmpty()    -> shop.budget!!.name
                else                                   -> "情報なし"
            }
            binding.detailBudgetText.text = budgetText

            binding.detailImage.load(shop.photo.pc.l) {
                crossfade(true)
                error(android.R.color.darker_gray)
            }

            binding.goHereButton.setOnClickListener {
                openWalkingNavigation(shop)
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun openWalkingNavigation(shop: Shop) {
        val navigationUri = Uri.parse("google.navigation:q=${shop.lat},${shop.lng}&mode=w")
        val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (navigationIntent.resolveActivity(packageManager) != null) {
            startActivity(navigationIntent)
            return
        }

        val geoUri = Uri.parse(
            "geo:${shop.lat},${shop.lng}?q=${shop.lat},${shop.lng}(${Uri.encode(shop.name)})"
        )
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)

        try {
            startActivity(Intent.createChooser(geoIntent, "地図アプリを選択"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "地図アプリが見つかりませんでした", Toast.LENGTH_SHORT).show()
        }
    }
}