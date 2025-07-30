package com.example.eventhou.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.data.model.Event
import com.facebook.drawee.backends.pipeline.Fresco
import com.yuyakaido.android.cardstackview.*
import kotlinx.coroutines.launch


/**
 * Fragment for SwipeCards.
 */
class SwipeFragment : Fragment(), CardStackListener {
    /**
     * The initial event stack (to be swiped). Needed to keep reference to the right event.
     */
    private var allEvents = mutableListOf<Event>()

    /**
     * The updated event stack (updates upcoming event stack in MainActivity).
     */
    private var updatedList = mutableListOf<Event>()

    private val adapter = EventAdapter()
    private lateinit var layoutManager: CardStackLayoutManager
    private lateinit var stackview: CardStackView
    private lateinit var mEventHandler: EventHandler

    /**
     * current position of card in stack
     */
    private var pos: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (requireActivity().application as EventhouApplication).appContainer
        mEventHandler = appContainer.eventHandler
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Fresco.initialize(activity)
        layoutManager = CardStackLayoutManager(activity, this).apply {
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
            setDirections(Direction.FREEDOM)
        }
        stackview = view.findViewById(R.id.stack_view)
        stackview.layoutManager = layoutManager
        stackview.adapter = adapter
        stackview.itemAnimator.apply {
            if (this is DefaultItemAnimator) {
                supportsChangeAnimations = false
            }
        }
        adapter.setEvents(allEvents)
    }

    companion object {
        fun newInstance(events: List<Event>) =
            SwipeFragment().apply {
                arguments = Bundle().apply {
                    allEvents.addAll(events)
                    updatedList.addAll(allEvents)
                    pos = 0
                }
            }
    }

    override fun onCardDisappeared(view: View?, position: Int) {
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {

    }

    override fun onCardSwiped(direction: Direction?) {
        if (Direction.HORIZONTAL.contains(direction)) {
            if (direction == Direction.Right) {
                // boost event and add to favourites
                mEventHandler.boostEvent(allEvents[pos], requireActivity())
            } else if (direction == Direction.Left) {
                // degrade event score
                mEventHandler.degradeEvent(allEvents[pos], requireActivity())
            }
            updatedList.removeAt(0)
            mEventHandler.filteredEvents = updatedList.toMutableList()
        } else {
            if (direction == Direction.Top) {
                lifecycleScope.launch {
                    // show event in browser
                    val uri: Uri = Uri.parse(allEvents[pos].eventUrl)

                    val customTabsIntent = context?.let {
                        ContextCompat.getColor(
                            it, R.color.mainColor
                        )
                    }?.let { CustomTabsIntent.Builder().setToolbarColor(it).build() };

                    customTabsIntent?.intent?.setPackage("com.android.chrome")
                    customTabsIntent?.launchUrl(requireActivity(), uri)
                }
            }
            // rewind or smooth scroll don't work for some reason
            stackview.scrollToPosition(pos)
        }
    }

    override fun onCardCanceled() {

    }

    override fun onCardAppeared(view: View?, position: Int) {
        // get current card position in stack
        pos = position
    }

    override fun onCardRewound() {

    }
}