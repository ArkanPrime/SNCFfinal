package com.example.sncffinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrainAdapter(private val trainList: List<TrainItem>) :
    RecyclerView.Adapter<TrainAdapter.TrainViewHolder>() {

    class TrainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDepartureTime: TextView = view.findViewById(R.id.textDepartureTime)
        val textDepartureStation: TextView = view.findViewById(R.id.textDepartureStation)
        val textArrivalTime: TextView = view.findViewById(R.id.textArrivalTime)
        val textArrivalStation: TextView = view.findViewById(R.id.textArrivalStation)
        val textDuration: TextView = view.findViewById(R.id.textDuration)
        val textDirect: TextView = view.findViewById(R.id.textDirect)
        val textTrainType: TextView = view.findViewById(R.id.textTrainType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_train, parent, false)
        return TrainViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainViewHolder, position: Int) {
        val train = trainList[position]
        holder.textDepartureTime.text = train.departureTime
        holder.textDepartureStation.text = train.departureStation
        holder.textArrivalTime.text = train.arrivalTime
        holder.textArrivalStation.text = train.arrivalStation
        holder.textDuration.text = train.duration
        holder.textDirect.text = if (train.isDirect) "Direct" else "Correspondance"
        holder.textTrainType.text = train.type
    }

    override fun getItemCount() = trainList.size
}
