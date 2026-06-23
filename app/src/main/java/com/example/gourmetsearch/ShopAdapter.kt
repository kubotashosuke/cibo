package com.example.gourmetsearch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gourmetsearch.databinding.ListItemShopBinding

// () の中に「タップされたときの処理（onItemClick）」を追加して受け取れるようにします
class ShopAdapter(
    private val shopList: List<Shop>,
    private val onItemClick: (Shop) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(val binding: ListItemShopBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ListItemShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return shopList.size
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]

        holder.binding.shopNameText.text = shop.name
        holder.binding.shopAccessText.text = shop.mobile_access

        holder.binding.shopImage.load(shop.photo.mobile.l) {
            crossfade(true)
        }

        // 【追加】リストの1行（root）がタップされたら、そのお店のデータ（shop）をListActivityに知らせる
        holder.binding.root.setOnClickListener {
            onItemClick(shop)
        }
    }
}