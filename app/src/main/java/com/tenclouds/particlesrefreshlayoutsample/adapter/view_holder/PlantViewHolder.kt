package com.tenclouds.particlesrefreshlayoutsample.adapter.view_holder

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.tenclouds.particlesrefreshlayoutsample.R
import com.tenclouds.particlesrefreshlayoutsample.adapter.item.Plant
import kotlinx.android.synthetic.main.vh_plant.view.*

class PlantViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

    fun bind(plant: Plant) {
        with(itemView) {
            name.text = plant.name
            description.text = plant.description
            price.text = itemView.context.getString(R.string.price).format(plant.price)
            photo.setImageDrawable(ContextCompat.getDrawable(itemView.context, plant.imageRes))
        }
    }
}