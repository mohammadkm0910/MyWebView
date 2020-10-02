package com.mohammadkk.mywebview.tab

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.mywebview.R
import com.mohammadkk.mywebview.utils.Services
import com.mohammadkk.mywebview.utils.Services.isTargetList

class TabRecyclerAdapter(private var context: Context, private var itemTabs: ArrayList<Tab>,private var onItemTabClick:OnItemTabClick) : RecyclerView.Adapter<TabRecyclerAdapter.ViewHolder>() {
    interface OnItemTabClick{
        fun onTabClick(index: Int)
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconWeb:ImageView = itemView.findViewById(R.id.iconWeb)
        val titleWeb: TextView = itemView.findViewById(R.id.titleWeb)
        private val tabCount: CardView = itemView.findViewById(R.id.tabCount)
        fun click(index: Int,onItemTabClick: OnItemTabClick) {
            tabCount.setOnClickListener {
                onItemTabClick.onTabClick(index)
                isTargetList = false
            }
            if (!isTargetList) tabCount.setCardBackgroundColor(Color.GRAY) else tabCount.setCardBackgroundColor(Color.WHITE)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_list_tab,parent,false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.iconWeb.setImageBitmap(itemTabs[position].webView.favicon)
        holder.titleWeb.text = itemTabs[position].webView.title
        isTargetList = true
        holder.click(position,onItemTabClick)
    }
    override fun getItemCount(): Int {
        return itemTabs.size
    }
}