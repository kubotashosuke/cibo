package com.example.gourmetsearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ShopAdapter(
    private val shopList: List<Shop>,
    private val onItemClick: (Shop) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopImage: ImageView = view.findViewById(R.id.shopImage)
        val shopNameText: TextView = view.findViewById(R.id.shopNameText)
        val shopGenreText: TextView = view.findViewById(R.id.shopGenreText)
        val shopAccessText: TextView = view.findViewById(R.id.shopAccessText)
        val shopBudgetText: TextView = view.findViewById(R.id.shopBudgetText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]
        holder.shopNameText.text = shop.name
        holder.shopGenreText.text = shop.genre.name
        holder.shopAccessText.text = shop.mobile_access

        val budgetLabel = shop.budget?.name?.takeIf { it.isNotEmpty() }
            ?: shop.budget?.average?.takeIf { it.isNotEmpty() }
        holder.shopBudgetText.text = budgetLabel ?: ""
        holder.shopBudgetText.visibility = if (budgetLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

        holder.shopImage.load(shop.photo.mobile.l) { crossfade(true) }
        holder.itemView.setOnClickListener { onItemClick(shop) }
    }

    override fun getItemCount(): Int = shopList.size
}
