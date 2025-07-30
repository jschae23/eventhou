package com.example.eventhou.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.eventhou.R
import com.example.eventhou.data.model.Event
import com.example.eventhou.databinding.CardViewEventBinding

class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var events: List<Event>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EventViewHolder(
        DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.card_view_event,
            parent,
            false
        )
    )

    override fun getItemCount() = events?.size ?: 0

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        events?.let {
            holder.binding.event = it[position]
            holder.binding.executePendingBindings()
        }
    }

    fun setEvents(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }

    inner class EventViewHolder(val binding: CardViewEventBinding) :
        RecyclerView.ViewHolder(binding.root)

}
