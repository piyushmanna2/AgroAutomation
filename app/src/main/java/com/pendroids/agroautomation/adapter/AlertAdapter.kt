package com.pendroids.agroautomation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pendroids.agroautomation.databinding.ItemAlertsBinding
import com.pendroids.agroautomation.model.AlertDataClass

class AlertAdapter(private val alertList: List<AlertDataClass>) :
    RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    inner class AlertViewHolder(private val binding: ItemAlertsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(alert: AlertDataClass) {
            binding.alertTitle.text = alert.AlertMessage
            binding.alertTime.text = alert.AlertDateTime
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(alertList[position])
    }

    override fun getItemCount(): Int = alertList.size
}
