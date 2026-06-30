package com.example.gourmetsearch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmetsearch.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mapLibreMap: MapLibreMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var symbolManager: SymbolManager? = null

    private val symbolShopMap = mutableMapOf<Long, List<Shop>>()

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    private var mapRange: Int = 3
    private var mapRadiusMeters: Double = 1000.0
    private var listRange: Int = 3

    private val apiService = Retrofit.Builder()
        .baseUrl("https://webservice.recruit.co.jp/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HotpepperApiService::class.java)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            mapLibreMap?.style?.let { enableLocationAndMoveCamera(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SharedData.clearFilter()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap(savedInstanceState)
        setupListMode()
        setupBottomNavigation()
        setupMapChipGroup()
        setupListChipGroup()
        setupFilterButtons()
    }

    private fun setupFilterButtons() {
        // マップモード用FAB
        binding.mapFilterFab.setOnClickListener { showFilterBottomSheet(isMap = true) }

        // リストモード用ボタン
        binding.listFilterButton.setOnClickListener { showFilterBottomSheet(isMap = false) }

        // フィルター適用中チップのバツでクリア
        binding.listFilterActiveChip.setOnCloseIconClickListener {
            SharedData.clearFilter()
            updateFilterActiveChip()
            updateListAdapter(SharedData.listShopList)
        }
        binding.mapFilterActiveChip.setOnCloseIconClickListener {
            SharedData.clearFilter()
            updateFilterActiveChip()
            showClusteredMarkers(SharedData.mapShopList)
        }
        binding.listEmptyClearFilterButton.setOnClickListener {
            SharedData.clearFilter()
            updateListAdapter(SharedData.listShopList)
        }
    }

    // ホットペッパーAPIの予算コードと表示名の定義
    private val budgetOptions = listOf(
        "" to "すべて",
        "B009" to "〜500円",
        "B010" to "501〜1000円",
        "B011" to "1001〜1500円",
        "B001" to "1501〜2000円",
        "B002" to "2001〜3000円",
        "B003" to "3001〜4000円",
        "B008" to "4001〜5000円",
        "B004" to "5001〜7000円",
        "B005" to "7001〜10000円",
        "B006" to "10001〜15000円",
        "B012" to "15001〜20000円",
        "B013" to "20001円〜"
    )

    private fun showFilterBottomSheet(isMap: Boolean) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filter, null)
        dialog.setContentView(view)

        val nameEdit    = view.findViewById<EditText>(R.id.filterNameEdit)
        val genreEdit   = view.findViewById<EditText>(R.id.filterGenreEdit)
        val budgetSpinner = view.findViewById<Spinner>(R.id.filterBudgetSpinner)
        val applyButton = view.findViewById<Button>(R.id.filterApplyButton)
        val clearButton = view.findViewById<Button>(R.id.filterClearButton)

        // 現在のフィルタ値を反映
        nameEdit.setText(SharedData.filterName)
        genreEdit.setText(SharedData.filterGenre)

        val budgetLabels = budgetOptions.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, budgetLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        budgetSpinner.adapter = adapter
        val currentIndex = budgetOptions.indexOfFirst { it.first == SharedData.filterBudgetCode }.coerceAtLeast(0)
        budgetSpinner.setSelection(currentIndex)

        applyButton.setOnClickListener {
            SharedData.filterName        = nameEdit.text.toString().trim()
            SharedData.filterGenre       = genreEdit.text.toString().trim()
            SharedData.filterBudgetCode  = budgetOptions[budgetSpinner.selectedItemPosition].first
            dialog.dismiss()
            if (isMap) {
                updateFilterActiveChip()
                applyMapFilterAndShow(SharedData.mapShopList)
            } else {
                updateFilterActiveChip()
                updateListAdapter(SharedData.listShopList)
            }
        }

        clearButton.setOnClickListener {
            SharedData.clearFilter()
            dialog.dismiss()
            if (isMap) {
                updateFilterActiveChip()
                showClusteredMarkers(SharedData.mapShopList)
            } else {
                updateFilterActiveChip()
                updateListAdapter(SharedData.listShopList)
            }
        }

        dialog.show()
    }

    private fun updateFilterActiveChip() {
        val active = SharedData.hasActiveFilter()
        binding.listFilterActiveChip.visibility = if (active) View.VISIBLE else View.GONE
        binding.mapFilterActiveChip.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    showMode("map")
                    true
                }
                R.id.nav_list -> {
                    showMode("list")
                    if (SharedData.listShopList.isEmpty() && currentLat != 0.0) {
                        searchForList(currentLat, currentLng, listRange)
                    } else {
                        updateListAdapter(SharedData.listShopList)
                    }
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_map
    }

    private fun showMode(mode: String) {
        binding.mapView.visibility = if (mode == "map") View.VISIBLE else View.GONE
        binding.mapChipCard.visibility = if (mode == "map") View.VISIBLE else View.GONE
        binding.listContainer.visibility = if (mode == "list") View.VISIBLE else View.GONE
    }

    private fun setupMapChipGroup() {
        binding.rangeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val (radius, range) = chipToRange(checkedIds[0])
            if (mapRadiusMeters != radius) {
                mapRadiusMeters = radius
                mapRange = range
                updateCircleOnMap()
                if (currentLat != 0.0) searchForMap(currentLat, currentLng, mapRange)
            }
        }
    }

    private fun setupListChipGroup() {
        binding.listRangeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val (_, range) = chipToRange(checkedIds[0])
            listRange = range
            if (currentLat != 0.0) searchForList(currentLat, currentLng, listRange)
        }
    }

    private fun chipToRange(chipId: Int): Pair<Double, Int> {
        return when (chipId) {
            R.id.chip300, R.id.listChip300 -> 300.0 to 1
            R.id.chip500, R.id.listChip500 -> 500.0 to 2
            R.id.chip2000, R.id.listChip2000 -> 2000.0 to 4
            R.id.chip3000, R.id.listChip3000 -> 3000.0 to 5
            else -> 1000.0 to 3
        }
    }

    private fun searchForMap(lat: Double, lng: Double, range: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.searchRestaurants(BuildConfig.HOTPEPPER_API_KEY, lat, lng, range)
                SharedData.mapShopList = response.results.shop
                updateFilterActiveChip()
                applyMapFilterAndShow(response.results.shop)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun searchForList(lat: Double, lng: Double, range: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.searchRestaurants(BuildConfig.HOTPEPPER_API_KEY, lat, lng, range)
                SharedData.listShopList = response.results.shop
                updateListAdapter(response.results.shop)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupListMode() {
        binding.listRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateListAdapter(shopList: List<Shop>) {
        val filtered = SharedData.applyFilter(shopList)
        val adapter = ShopAdapter(filtered) { selectedShop -> openDetail(selectedShop) }
        binding.listRecyclerView.adapter = adapter
        updateFilterActiveChip()

        val showEmptyHint = filtered.isEmpty() && shopList.isNotEmpty() && SharedData.hasActiveFilter()
        binding.listEmptyHint.visibility = if (showEmptyHint) View.VISIBLE else View.GONE
    }

    private fun openDetail(shop: Shop) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("SHOP_DATA_JSON", Gson().toJson(shop))
        startActivity(intent)
    }

    private fun showClusterShopsBottomSheet(shops: List<Shop>) {
        val dialog = BottomSheetDialog(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(16), 0, dpToPx(8))
        }

        val title = TextView(this).apply {
            text = "近くのお店（${shops.size}件）"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dpToPx(20), 0, dpToPx(20), dpToPx(12))
        }
        container.addView(title)

        val recyclerView = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            adapter = ShopAdapter(shops) { selectedShop ->
                dialog.dismiss()
                openDetail(selectedShop)
            }
        }
        container.addView(recyclerView)

        dialog.setContentView(container)
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
            map.setStyle(styleUrl) { style ->
                symbolManager = SymbolManager(binding.mapView, map, style).apply {
                    iconAllowOverlap = true
                }
                symbolManager?.addClickListener { symbol ->
                    val cluster = symbolShopMap[symbol.id]
                    if (cluster != null) {
                        if (cluster.size == 1) {
                            openDetail(cluster[0])
                        } else {
                            showClusterShopsBottomSheet(cluster)
                        }
                    }
                    true
                }

                style.addSource(GeoJsonSource("circle-source"))
                style.addLayerBelow(
                    FillLayer("circle-layer", "circle-source").withProperties(
                        PropertyFactory.fillColor(Color.parseColor("#4DBDBDBD")),
                        PropertyFactory.fillOutlineColor(Color.parseColor("#9E9E9E"))
                    ), "com.mapbox.annotations.points"
                )
                checkPermissionAndSetupLocation(style)
            }
        }
    }

    private fun updateCircleOnMap() {
        mapLibreMap?.style?.getSourceAs<GeoJsonSource>("circle-source")
            ?.setGeoJson(createCirclePolygon(currentLat, currentLng, mapRadiusMeters))
        val zoom = when (mapRange) {
            1 -> 16.0; 2 -> 15.5; 3 -> 14.5; 4 -> 13.5; else -> 12.8
        }
        mapLibreMap?.animateCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(LatLng(currentLat, currentLng)).zoom(zoom).build()
        ), 500)
    }

    private fun createCirclePolygon(lat: Double, lng: Double, radius: Double): Feature {
        val points = mutableListOf<Point>()
        for (i in 0..360 step 5) {
            val rad = Math.toRadians(i.toDouble())
            val d = radius / 6378137.0
            val lat2 = Math.asin(Math.sin(Math.toRadians(lat)) * Math.cos(d) + Math.cos(Math.toRadians(lat)) * Math.sin(d) * Math.cos(rad))
            val lng2 = Math.toRadians(lng) + Math.atan2(Math.sin(rad) * Math.sin(d) * Math.cos(Math.toRadians(lat)), Math.cos(d) - Math.sin(Math.toRadians(lat)) * Math.sin(lat2))
            points.add(Point.fromLngLat(Math.toDegrees(lng2), Math.toDegrees(lat2)))
        }
        return Feature.fromGeometry(Polygon.fromLngLats(listOf(points)))
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6378137.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun clusterShops(shopList: List<Shop>): List<List<Shop>> {
        val thresholdMeters = (mapRadiusMeters / 20.0).coerceIn(20.0, 200.0)

        val clusters = mutableListOf<MutableList<Shop>>()
        for (shop in shopList) {
            var addedTo: MutableList<Shop>? = null
            for (cluster in clusters) {
                val rep = cluster.first()
                if (distanceMeters(rep.lat, rep.lng, shop.lat, shop.lng) < thresholdMeters) {
                    addedTo = cluster
                    break
                }
            }
            if (addedTo != null) {
                addedTo.add(shop)
            } else {
                clusters.add(mutableListOf(shop))
            }
        }
        return clusters
    }

    private fun applyMapFilterAndShow(allShops: List<Shop>) {
        val filtered = SharedData.applyFilter(allShops)
        if (filtered.isEmpty() && allShops.isNotEmpty() && SharedData.hasActiveFilter()) {
            Toast.makeText(this, "条件に合うお店が見つかりません", Toast.LENGTH_SHORT).show()
        }
        showClusteredMarkers(filtered)
    }

    private fun showClusteredMarkers(shopList: List<Shop>) {
        symbolManager?.deleteAll()
        symbolShopMap.clear()
        val style = mapLibreMap?.style ?: return

        val clusters = clusterShops(shopList)

        clusters.forEachIndexed { index, cluster ->
            val bitmap: Bitmap
            val lat: Double
            val lng: Double

            if (cluster.size == 1) {
                val shop = cluster[0]
                bitmap = createGenrePinBitmap(shop)
                lat = shop.lat
                lng = shop.lng
            } else {
                bitmap = createClusterBitmap(cluster.size)
                lat = cluster.map { it.lat }.average()
                lng = cluster.map { it.lng }.average()
            }

            val id = "pin_$index"
            style.addImage(id, bitmap)
            val symbol = symbolManager?.create(
                SymbolOptions().withLatLng(LatLng(lat, lng)).withIconImage(id)
            )
            if (symbol != null) {
                symbolShopMap[symbol.id] = cluster
            }
        }
    }

    private fun createGenrePinBitmap(shop: Shop): Bitmap {
        val pinView = layoutInflater.inflate(R.layout.pin_custom, null)
        val iconView = pinView.findViewById<ImageView>(R.id.pinIcon)

        val (iconRes, tintColor) = when {
            shop.genre.name.contains("カフェ") -> R.drawable.ic_genre_cafe to R.color.md_tertiary
            shop.genre.name.contains("居酒屋") -> R.drawable.ic_genre_izakaya to R.color.md_primary
            shop.genre.name.contains("焼肉") -> R.drawable.ic_genre_yakiniku to R.color.md_primary
            shop.genre.name.contains("イタリアン") -> R.drawable.ic_genre_italian to R.color.md_secondary
            shop.genre.name.contains("和食") -> R.drawable.ic_genre_washoku to R.color.md_secondary
            shop.genre.name.contains("中華") -> R.drawable.ic_genre_chinese to R.color.md_primary
            shop.genre.name.contains("バー") -> R.drawable.ic_genre_bar to R.color.md_tertiary
            else -> R.drawable.ic_genre_genelic to R.color.md_on_surface_variant
        }

        iconView.setImageResource(iconRes)
        iconView.setColorFilter(ContextCompat.getColor(this, tintColor))

        pinView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        pinView.layout(0, 0, pinView.measuredWidth, pinView.measuredHeight)
        val bmp = Bitmap.createBitmap(
            if (pinView.measuredWidth > 0) pinView.measuredWidth else 100,
            if (pinView.measuredHeight > 0) pinView.measuredHeight else 100,
            Bitmap.Config.ARGB_8888
        )
        pinView.draw(Canvas(bmp))
        return bmp
    }

    private fun createClusterBitmap(count: Int): Bitmap {
        val size = 130
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(this@MainActivity, R.color.md_primary)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, circlePaint)

        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, strokePaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 44f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(count.toString(), size / 2f, textY, textPaint)

        return bitmap
    }

    private fun checkPermissionAndSetupLocation(style: Style) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationAndMoveCamera(style)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationAndMoveCamera(style: Style) {
        mapLibreMap?.locationComponent?.apply {
            activateLocationComponent(LocationComponentActivationOptions.builder(this@MainActivity, style).build())
            isLocationComponentEnabled = true
            renderMode = RenderMode.COMPASS
        }

        var resolved = false
        val timeoutHandler = android.os.Handler(mainLooper)
        val timeoutRunnable = Runnable {
            if (!resolved) {
                resolved = true
                fallbackToLastLocation()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 8000L)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (!resolved) {
                    resolved = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    if (loc != null) {
                        onLocationObtained(loc.latitude, loc.longitude)
                    } else {
                        fallbackToLastLocation()
                    }
                }
            }
            .addOnFailureListener {
                if (!resolved) {
                    resolved = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    fallbackToLastLocation()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationObtained(loc.latitude, loc.longitude)
                } else {
                    Toast.makeText(
                        this,
                        "現在地を取得できませんでした。GPS設定をご確認ください",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "現在地を取得できませんでした。GPS設定をご確認ください",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun onLocationObtained(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
        updateCircleOnMap()
        searchForMap(currentLat, currentLng, mapRange)
        searchForList(currentLat, currentLng, listRange)
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.mapView.onSaveInstanceState(outState) }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); binding.mapView.onDestroy() }
}