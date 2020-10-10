package com.mohammadkk.mywebview.tab

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.mywebview.R

class TabRecyclerAdapter(private var context: Context, private var itemTabs: ArrayList<Tab>,private var onItemTabClick:OnItemTabClick) : RecyclerView.Adapter<TabRecyclerAdapter.ViewHolder>() {
    interface OnItemTabClick{
        fun onTabClick(index: Int)
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconWeb:ImageView = itemView.findViewById(R.id.iconWeb)
        val titleWeb: TextView = itemView.findViewById(R.id.titleWeb)
        val tabCount: CardView = itemView.findViewById(R.id.tabCount)
        fun click(index: Int,onItemTabClick: OnItemTabClick,) {
            tabCount.setOnClickListener {
                onItemTabClick.onTabClick(index)
                indexGet = index
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_list_tab,parent,false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.iconWeb.setImageBitmap(itemTabs[position].webView.favicon)
        holder.titleWeb.text = itemTabs[position].webView.title
        holder.click(position,onItemTabClick)
        if (indexGet == position) holder.tabCount.setCardBackgroundColor(Color.GRAY) else holder.tabCount.setCardBackgroundColor(Color.WHITE)
    }
    override fun getItemCount(): Int {
        return itemTabs.size
    }
    companion object{
        var indexGet = 0
    }
}