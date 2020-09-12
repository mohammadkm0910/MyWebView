package com.mohammadkk.mywebview.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.mohammadkk.mywebview.R

class SpinnerIconAdapter(private var context: Context, private var icons: Array<Int>) : BaseAdapter(){
    override fun getCount(): Int {
        return icons.size
    }
    override fun getItem(position: Int): Any? {
        return null
    }
    override fun getItemId(position: Int): Long {
        return 0
    }
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.url_icon_spinner,parent,false)
        val imageViewUrlIcon = view.findViewById<ImageView>(R.id.imageViewUrlIcon)
        imageViewUrlIcon.setImageResource(icons[position])
        return view
    }
}