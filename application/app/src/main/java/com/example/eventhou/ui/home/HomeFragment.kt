package com.example.eventhou.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.eventhou.data.model.Event
import com.example.eventhou.EventhouApplication
import com.example.eventhou.ui.filter.FilterActivity
import com.example.eventhou.R
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.data.events.IEventHandler

class HomeFragment : Fragment(), IEventHandler {

    private val ACTIVITY_RESULT_FILTER = 70

    private lateinit var eventHandler: EventHandler
    lateinit var filter: ImageView
    lateinit var date: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFragment()
        filter = view.findViewById(R.id.filter_btn)

        // start filter activity when filter is clicked
        filter.setOnClickListener {
            val intent = Intent(requireActivity(), FilterActivity::class.java)
            startActivityForResult(intent, ACTIVITY_RESULT_FILTER)
        }
    }

    companion object {
        fun newInstance() =
            HomeFragment().apply {
            }
    }

    /**
     * initialize a new loading fragment
     */
    private fun newLoadFragment() {
        val fragmentLoad =
            LoadFragement.newInstance()
        childFragmentManager.beginTransaction().replace(
            R.id.container,
            fragmentLoad,
            fragmentLoad.javaClass.simpleName
        ).commitAllowingStateLoss()
    }

    /**
     * initialize a new swipe fragment
     * passing the filtered events
     */
    private fun newSwipeFragment() {
        val fragmentSwipe =
            SwipeFragment.newInstance(
                eventHandler.filteredEvents
            )
        childFragmentManager.beginTransaction().replace(
            R.id.container,
            fragmentSwipe,
            fragmentSwipe.javaClass.simpleName
        ).commitAllowingStateLoss()
    }

    /**
     * initializes new fragment showing that no events are available
     */
    private fun newNoEventsFragment() {
        val fragmentNoEvents =
            NoEventsFragment.newInstance()
        childFragmentManager.beginTransaction().replace(
            R.id.container,
            fragmentNoEvents,
            fragmentNoEvents.javaClass.simpleName
        ).commitAllowingStateLoss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val appContainer = (requireActivity().application as EventhouApplication).appContainer
        eventHandler = appContainer.eventHandler
        eventHandler.eventHandles.add(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_RESULT_FILTER) {
            newLoadFragment()
            // load new events with filter
            val activity = requireActivity()
            eventHandler.categoryFilterPrefs = EventHandler.getCategoryFilterPrefs(activity)
            eventHandler.processEvents(activity)
        }
    }

    /**
     * updating fragment depending on event loading state and available events
     */
    private fun updateFragment() {
        if (eventHandler.isJobRunning) {
            newLoadFragment()
        } else {
            if (eventHandler.filteredEvents.isEmpty()) {
                newNoEventsFragment()
            } else {
                newSwipeFragment()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        eventHandler.eventHandles.remove(this)
    }

    override fun onEventsLoading() {
        newLoadFragment()
    }

    /**
     * showing SwipeFragment or NoEventsFragment if there are no more events available
     */
    override fun onEventsLoaded(updatedEvents: List<Event>) {
        if (updatedEvents.isEmpty()) {
            newNoEventsFragment()
        } else {
            newSwipeFragment()
        }
    }

    override fun onEventsUpdated(updatedEvents: List<Event>) {
        // Avoid loading swipe fragment on every events update
        if (updatedEvents.isEmpty()) {
            newNoEventsFragment()
        }
    }
}