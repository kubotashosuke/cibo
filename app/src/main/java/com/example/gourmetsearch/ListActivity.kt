package com.example.gourmetsearch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmetsearch.databinding.ActivityListBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListActivity : ComponentActivity() {

    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val jsonString = intent.getStringExtra("SHOP_LIST_JSON")
        if (jsonString != null) {
            val type = object : TypeToken<List<Shop>>() {}.type
            val shopList: List<Shop> = Gson().fromJson(jsonString, type)

            // 【変更】Adapterを作るときに、タップされたときの画面遷移ルールも一緒に渡す
            val adapter = ShopAdapter(shopList) { selectedShop ->
                val intent = Intent(this, DetailActivity::class.java)
                // タップされた1つのお店だけをJSONに変換して詳細画面へ渡す
                val shopJson = Gson().toJson(selectedShop)
                intent.putExtra("SHOP_DATA_JSON", shopJson)
                startActivity(intent)
            }
            binding.recyclerView.adapter = adapter
        }
    }
}