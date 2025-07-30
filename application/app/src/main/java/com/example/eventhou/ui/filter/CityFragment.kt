package com.example.eventhou.ui.filter

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.eventhou.data.model.City
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.events.EventHandler

class CityFragment : Fragment() {

    /**
     * list of all available cities for filtering
     */
    var allcities = mutableSetOf<City>()
    lateinit var listView: ListView
    lateinit var arrayAdapter: ArrayAdapter<String>
    lateinit var mListener: OnFilterInteractionListener
    lateinit var mEventHandler: EventHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get list of cities from firebase
        val appContainer = (requireActivity().application as EventhouApplication).appContainer
        mEventHandler = appContainer.eventHandler
        allcities = mEventHandler.allCities
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_filter_cities, container, false)
        listView = view.findViewById(R.id.listView)
        view.isFocusableInTouchMode = true
        view.requestFocus()
        // returning to FilterOptionsFragment on back button clicked
        view.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                mListener.SelectionFinished()
                return@OnKeyListener true
            }
            false
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // create ListView with city names
        arrayAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                allcities.map { it.name })

        arrayAdapter.notifyDataSetChanged()
        listView.adapter = arrayAdapter

        // save new location in EventHandler and preferences when city was selected
        // and return to FilterOptions screen
        listView.setOnItemClickListener { parent, view, position, id ->
            mEventHandler.location =
                arrayAdapter.getItem(position) ?: EventHandler.FALLBACK_LOCATION
            val sharedPref = requireActivity().getSharedPreferences(
                getString(R.string.preference_filter_city),
                Context.MODE_PRIVATE
            ) ?: return@setOnItemClickListener
            sharedPref.edit().putString(EventHandler.PREFERENCE_LOCATION, mEventHandler.location)
                .apply()
            mListener.SelectionFinished()
        }
    }

    companion object {
        fun newInstance() =
            CityFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFilterInteractionListener) {
            mListener = context
        } else {
            throw ClassCastException(
                "$context must implement OnFragmentInteractionListener"
            )
        }
    }
}