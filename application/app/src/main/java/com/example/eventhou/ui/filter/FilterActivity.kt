package com.example.eventhou.ui.filter

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.util.dateRangeToLocalizedShort
import com.example.eventhou.util.dateToEpochMilli
import com.example.eventhou.util.epochMilliToDate
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilterActivity : AppCompatActivity(),
    OnFilterInteractionListener {
    lateinit var toolbar: ActionBar
    lateinit var mEventHandler: EventHandler
    lateinit var fragmentCity: CityFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        toolbar = supportActionBar!!
        toolbar.hide()

        val appContainer = (this.application as EventhouApplication).appContainer
        mEventHandler = appContainer.eventHandler

        // Query cities, if not already done in MainActivity
        if (mEventHandler.allCities.isEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                mEventHandler.queryCities()
            }
        }

        fragmentCity = CityFragment.newInstance()

        // call filterOptions fragment as first fragment
        if (savedInstanceState == null) {
            val fragmentOptions = FilterOptionsFragment.newInstance(
                getLocationFromPrefs(), // Do not use location from eventHandler here
                dateRangeToLocalizedShort(mEventHandler.startDate, mEventHandler.endDate)
            )
            supportFragmentManager.beginTransaction().replace(
                R.id.container_filter,
                fragmentOptions,
                fragmentOptions.javaClass.getSimpleName()
            ).commit()
        }
    }

    /**
     * get location from preferences
     * if no location available the fallback location is taken
     */
    fun getLocationFromPrefs(): String {
        // Preference location is used as event handler takes location which is compatible to firebase locations
        val sharedPref = this.getSharedPreferences(
            getString(R.string.preference_filter_city),
            Context.MODE_PRIVATE
        ) ?: return EventHandler.FALLBACK_LOCATION
        return sharedPref.getString(
            EventHandler.PREFERENCE_LOCATION,
            EventHandler.FALLBACK_LOCATION
        ).toString()
    }

    /**
     * event listener indicating if a selection in a filter has finished
     * a new fragmentOptions fragment with the updated values is called
     */
    override fun SelectionFinished() {
        val filtertOptions = FilterOptionsFragment.newInstance(
            getLocationFromPrefs(), // Do not use location from eventHandler here
            dateRangeToLocalizedShort(mEventHandler.startDate, mEventHandler.endDate)
        )
        supportFragmentManager.beginTransaction().replace(
            R.id.container_filter,
                filtertOptions,
                filtertOptions.javaClass.getSimpleName()
        ).commit()
    }

    /**
     * event listener if a filter in filteroptions was selected
     * calls the right fragment or shows date picker
     */
    override fun FilterOptionChanged(option: String, view: View) {
        when (option) {
            "City" -> {
                supportFragmentManager.beginTransaction().replace(
                    R.id.container_filter,
                    fragmentCity,
                    fragmentCity.javaClass.getSimpleName()
                ).commit()
            }
            "Genre" -> {
                val fragmentGenre = CategoryPrefsWrapperFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(
                    R.id.container_filter,
                    fragmentGenre,
                    fragmentGenre.javaClass.getSimpleName()
                ).addToBackStack(null)
                    .commit()
            }
            "Date" -> {
                // date picker dialog
                val builder = MaterialDatePicker.Builder.dateRangePicker()
                builder.setTitleText("Select date range")
                builder.setSelection(
                    androidx.core.util.Pair(
                        dateToEpochMilli(mEventHandler.startDate),
                        dateToEpochMilli(mEventHandler.endDate)
                    )
                )
                val picker = builder.build()
                picker.addOnPositiveButtonClickListener {
                    mEventHandler.startDate = epochMilliToDate(it.first ?: 0)
                    mEventHandler.endDate = epochMilliToDate(it.second ?: 0)
                    val dateText: TextView = findViewById(R.id.current_date)
                    dateText.text =
                        dateRangeToLocalizedShort(mEventHandler.startDate, mEventHandler.endDate)
                }
                // picker.isCancelable = false // To prevent from accidentally pressing back
                picker.show(supportFragmentManager, picker.toString())
            }
        }
    }
}
