package com.example.eventhou.ui.favourites

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.eventhou.R
import com.example.eventhou.data.model.Event
import com.example.eventhou.databinding.CardViewFavouritesBinding

class FavouritesAdapter : RecyclerView.Adapter<FavouritesAdapter.FavouritesViewHolder>() {

    var events: List<Event> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FavouritesViewHolder(
        DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.card_view_favourites,
            parent,
            false
        )
    )

    override fun getItemCount() = events.size

    override fun onBindViewHolder(holder: FavouritesViewHolder, position: Int) {
        events.let {
            holder.binding.event = it[position]
            val link = it[position].eventUrl
            // show event in browser when favourite card is clicked
            holder.binding.container.setOnClickListener { view ->
                val uri: Uri = Uri.parse(link)

                val customTabsIntent = holder.let {
                    ContextCompat.getColor(
                        view.context, R.color.mainColor
                    )
                }.let { CustomTabsIntent.Builder().setToolbarColor(it).build() }

                customTabsIntent.intent.setPackage("com.android.chrome")
                customTabsIntent.launchUrl(view.context, uri)
            }
            holder.binding.executePendingBindings()
        }
    }

    inner class FavouritesViewHolder(val binding: CardViewFavouritesBinding) :
        RecyclerView.ViewHolder(binding.root)

}
