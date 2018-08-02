package com.tenclouds.particlesrefreshlayoutsample.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tenclouds.particlesrefreshlayoutsample.R
import com.tenclouds.particlesrefreshlayoutsample.adapter.item.Plant
import com.tenclouds.particlesrefreshlayoutsample.adapter.view_holder.PlantViewHolder

class PlantsAdapter :
        RecyclerView.Adapter<PlantViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var plantsList: List<Plant>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.vh_plant, parent, false)
                    .let { PlantViewHolder(it) }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) =
            if (plantsList != null && position < plantsList?.size ?: 0) holder.bind(plantsList!![position])
            else Unit

    override fun getItemCount() = plantsList?.size ?: 0
}

