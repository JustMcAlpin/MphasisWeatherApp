package com.justmcalpin.enterpriseapplicationreview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class GridViewAdapter(
    private val context: Context,
    private val categories: Array<String>,
    private val images: Array<Int>
) : BaseAdapter() {

    override fun getCount(): Int {
        return categories.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val gridViewAndroid: View

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            gridViewAndroid = inflater.inflate(R.layout.grid_item, null)
        } else {
            gridViewAndroid = convertView
        }

        val textViewAndroid: TextView = gridViewAndroid.findViewById(R.id.gridview_text)
        val imageViewAndroid: ImageView = gridViewAndroid.findViewById(R.id.gridview_image)
        textViewAndroid.text = categories[position]
        imageViewAndroid.setImageResource(images[position])
        return gridViewAndroid
    }
}
