package com.example.eventhou.ui.favourites

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventhou.data.model.Event
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.util.dateToUtcDateTime
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate


/**
 * A simple [Fragment] subclass.
 * Use the [FavouritesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FavouritesFragment : Fragment() {
    private lateinit var mEventHandler: EventHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v: View = inflater.inflate(R.layout.fragment_favourites, container, false)

        val layoutIds = listOf(
            R.id.favoritesToday,
            R.id.favoritesTomorrow,
            R.id.favoritesWeek,
            R.id.favoritesMonth
        )
        val favoriteLayouts = layoutIds.map { v.findViewById<ConstraintLayout>(it) }

        // show date sorting labels
        val textLabels = favoriteLayouts.map { it.findViewById<TextView>(R.id.favorite_label) }
        textLabels[0].setText(R.string.today)
        textLabels[1].setText(R.string.tomorrow)
        textLabels[2].setText(R.string.this_week)
        textLabels[3].setText(R.string.next_weeks)

        // getting user favourites of the user from firebase
        val appContainer = (activity?.application as EventhouApplication).appContainer
        mEventHandler = appContainer.eventHandler
        lifecycleScope.launch(Dispatchers.IO) {
            val favouriteEvents = mEventHandler.getFavoriteEventsFromUser()

            // split favourites in 4 different lists depending on date
            var dateTime = LocalDate.now().atStartOfDay().plusDays(1)
            val (favoritesToday, rest1) = favouriteEvents.partition { dateToUtcDateTime(it.utcDate) < dateTime }
            dateTime = dateTime.plusDays(1)
            val (favoritesTomorrow, rest2) = rest1.partition { dateToUtcDateTime(it.utcDate) < dateTime }
            dateTime = dateTime.plusDays(5)
            val (favoritesWeek, favoritesMonth) = rest2.partition { dateToUtcDateTime(it.utcDate) < dateTime }
            val favoriteEventLists = listOf(
                favoritesToday.toMutableList(),
                favoritesTomorrow.toMutableList(),
                favoritesWeek.toMutableList(),
                favoritesMonth.toMutableList()
            )

            activity?.runOnUiThread {
                for (i in favoriteLayouts.indices) {
                    val favoriteRecyclerView =
                        favoriteLayouts[i].findViewById<RecyclerView>(R.id.recycler_view)
                    val noEventsView = favoriteLayouts[i].findViewById<TextView>(R.id.empty_view)
                    val loadFragment =
                        favoriteLayouts[i].findViewById<LinearLayout>(R.id.loadLayout)
                    favoriteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    val adapter =
                        FavouritesAdapter()
                    favoriteEventLists[i].sortBy { it.utcDate }
                    adapter.events = favoriteEventLists[i]
                    favoriteRecyclerView.adapter = adapter
                    favoriteRecyclerView.itemAnimator.apply {
                        if (this is DefaultItemAnimator) {
                            supportsChangeAnimations = false
                        }
                    }

                    // initialize itemTouchHelper for left and right swipe of favourites cards
                    val itemTouchHelperCallback =
                        ItemTouchHelperCallback(
                            mEventHandler,
                            requireActivity(),
                            adapter
                        )
                    val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                    itemTouchHelper.attachToRecyclerView(favoriteRecyclerView)
                    if (favoriteEventLists[i].isEmpty()) {
                        noEventsView.visibility = View.VISIBLE
                        favoriteRecyclerView.visibility = View.GONE
                    } else {
                        noEventsView.visibility = View.GONE
                        favoriteRecyclerView.visibility = View.VISIBLE
                    }
                    loadFragment.visibility = View.GONE
                }
            }
        }

        return v
    }


    companion object {
        fun newInstance() =
            FavouritesFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    /**
     * class handling the swipe callbacks of the itemTouchHelper
     */
    class ItemTouchHelperCallback(
        val mEventHandler: EventHandler,
        val activity: Activity,
        val adapter: FavouritesAdapter
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        val mapColor = ContextCompat.getColor(activity, R.color.blue)
        val deleteColor = ContextCompat.getColor(activity, R.color.red)
        val colorDrawableBackground = GradientDrawable()
        val mapIcon = ContextCompat.getDrawable(activity, R.drawable.ic_baseline_map_24)!!
        val deleteIcon = ContextCompat.getDrawable(activity, R.drawable.ic_baseline_delete_24)!!

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            viewHolder2: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        // delete favourite when card was swiped left, open google map service when swiped right
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDirection: Int) {
            val position = viewHolder.adapterPosition
            val eventList = adapter.events.toMutableList()
            val eventToRemove: Event = eventList[position]
            if (swipeDirection == ItemTouchHelper.LEFT) {
                mEventHandler.removeFavoriteEventFromUser(eventToRemove)
                eventList.removeAt(position)
                adapter.events = eventList
                Snackbar.make(
                    viewHolder.itemView,
                    "Event with ${eventToRemove.artistName} removed",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    eventList.add(position, eventToRemove)
                    adapter.events = eventList
                    mEventHandler.addFavoriteEventToUser(eventToRemove)
                }.show()
            } else if (swipeDirection == ItemTouchHelper.RIGHT) {
                val gmmIntentUri = Uri.parse("geo:0,0?q=${eventToRemove.venueName}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                activity.startActivity(mapIntent)
                adapter.events = eventList
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val iconMarginVertical =
                (viewHolder.itemView.height - deleteIcon.intrinsicHeight) / 2

            val icon: Drawable

            if (dX > 0) {
                colorDrawableBackground.setColor(mapColor)
                icon = mapIcon
                mapIcon.setBounds(
                    itemView.left + iconMarginVertical,
                    itemView.top + iconMarginVertical,
                    itemView.left + iconMarginVertical + icon.intrinsicWidth,
                    itemView.bottom - iconMarginVertical
                )
            } else {
                colorDrawableBackground.setColor(deleteColor)
                icon = deleteIcon
                deleteIcon.setBounds(
                    itemView.right - iconMarginVertical - deleteIcon.intrinsicWidth,
                    itemView.top + iconMarginVertical,
                    itemView.right - iconMarginVertical,
                    itemView.bottom - iconMarginVertical
                )
            }
            colorDrawableBackground.setBounds(
                itemView.left,
                itemView.top,
                itemView.right,
                itemView.bottom
            )

            colorDrawableBackground.cornerRadius = 30f
            icon.level = 0
            colorDrawableBackground.draw(c)
            icon.draw(c)

            c.save()
            if (dX > 0) {
                c.clipRect(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
            } else {
                c.clipRect(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
            }

            c.restore()
            super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
        }
    }
}
