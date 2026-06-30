package com.example.gourmetsearch

object SharedData {
    var mapShopList: List<Shop> = emptyList()
    var listShopList: List<Shop> = emptyList()

    // フィルタ状態（マップ・リスト共通）
    var filterGenre: String = ""       // ジャンル名の部分一致
    var filterName: String = ""        // 店名の部分一致
    var filterBudgetCode: String = ""  // 予算コード ("" = 全て)

    fun applyFilter(shops: List<Shop>): List<Shop> {
        return shops.filter { shop ->
            val genreOk = filterGenre.isEmpty() || shop.genre.name.contains(filterGenre, ignoreCase = true)
            val nameOk  = filterName.isEmpty()  || shop.name.contains(filterName, ignoreCase = true)
            val budgetOk = filterBudgetCode.isEmpty() || shop.budget?.code == filterBudgetCode
            genreOk && nameOk && budgetOk
        }
    }

    fun hasActiveFilter(): Boolean =
        filterGenre.isNotEmpty() || filterName.isNotEmpty() || filterBudgetCode.isNotEmpty()

    fun clearFilter() {
        filterGenre = ""
        filterName = ""
        filterBudgetCode = ""
    }
}
