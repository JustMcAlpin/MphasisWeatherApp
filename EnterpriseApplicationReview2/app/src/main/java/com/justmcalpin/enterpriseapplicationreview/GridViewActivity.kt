package com.justmcalpin.enterpriseapplicationreview

import android.os.Bundle
import android.widget.GridView
import androidx.appcompat.app.AppCompatActivity

class GridViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_view)

        val gridView: GridView = findViewById(R.id.gridView)

        // Set up your adapter and populate the grid view with data
        val categories = arrayOf(
            "Refurbished", "Arts & crafts", "Cars & auto", "Bags & accessories",
            "Beauty", "Phones & accessories", "Office & tech", "Electronics",
            "Health & wellness", "Home & garden", "Home essentials", "Jewelry & watches",
            "Kids' & baby clothing", "Men's clothing", "Office & school supplies"
        )

        val images = arrayOf(
            R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished,
            R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished,
            R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished,
            R.drawable.refurbished, R.drawable.refurbished, R.drawable.refurbished
        )

        val adapter = GridViewAdapter(this, categories, images)
        gridView.adapter = adapter
    }
}