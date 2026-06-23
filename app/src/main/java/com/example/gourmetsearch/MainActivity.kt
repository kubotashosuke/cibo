package com.example.gourmetsearch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gourmetsearch.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentShopList: List<Shop> = emptyList()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://webservice.recruit.co.jp/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(HotpepperApiService::class.java)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            getLocation()
        } else {
            binding.locationText.text = "権限拒否"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.getLocationButton.setOnClickListener {
            checkPermissionAndGetLocation()
        }

        binding.searchButton.setOnClickListener {
            if (currentShopList.isNotEmpty()) {
                val intent = Intent(this, ListActivity::class.java)
                val jsonString = Gson().toJson(currentShopList)
                intent.putExtra("SHOP_LIST_JSON", jsonString)
                startActivity(intent)
            } else {
                binding.locationText.text = "先に現在地を取得してください"
            }
        }
    }

    private fun checkPermissionAndGetLocation() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            getLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        binding.locationText.text = "現在地を取得中..."
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    binding.locationText.text = "東京（新宿）でテスト検索中..."
                    val tokyoLat = 35.690270
                    val tokyoLng = 139.700049
                    searchRestaurants(tokyoLat, tokyoLng)
                } else {
                    binding.locationText.text = "取得失敗"
                }
            }
    }

    private fun searchRestaurants(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val response = apiService.searchRestaurants(
                    apiKey = BuildConfig.HotPepper_API_KEY,
                    lat = lat,
                    lng = lng,
                    range = 3
                )

                val shopList = response.results.shop
                if (shopList.isNotEmpty()) {
                    currentShopList = shopList
                    binding.locationText.text = "近くに ${shopList.size} 件のお店が見つかりました！\n検索ボタンを押してください"
                } else {
                    binding.locationText.text = "近くにお店が見つかりませんでした"
                }

            } catch (e: Exception) {
                binding.locationText.text = "通信エラー: ${e.message}"
            }
        }
    }
}