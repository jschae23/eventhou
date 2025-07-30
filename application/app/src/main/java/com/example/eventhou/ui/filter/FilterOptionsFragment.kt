package com.example.eventhou.ui.filter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.eventhou.R


interface OnFilterInteractionListener {
    fun FilterOptionChanged(option: String, view: View)
    fun SelectionFinished()
}

class FilterOptionsFragment : Fragment() {
    lateinit var mListener: OnFilterInteractionListener
    lateinit var cityBtn: LinearLayout
    lateinit var genreBtn: LinearLayout
    lateinit var dateBtn: LinearLayout
    private lateinit var date: String
    private lateinit var city: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filteroptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cityBtn = view.findViewById(R.id.ln_city)
        genreBtn = view.findViewById(R.id.ln_genre)
        dateBtn = view.findViewById(R.id.ln_date)
        cityBtn.setOnClickListener(clickListener)
        genreBtn.setOnClickListener(clickListener)
        dateBtn.setOnClickListener(clickListener)

        val categories = mutableListOf<String>()
        val currentDate: TextView = view.findViewById(R.id.current_date)
        currentDate.text = date
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_filter_categories),
            Context.MODE_PRIVATE
        ) ?: return

        val categorySlugs = resources.getStringArray(R.array.category_slugs)
        val subCategorySlugs = resources.obtainTypedArray(R.array.subcategory_slugs)
        val subCategoryLabels = resources.obtainTypedArray(R.array.subcategory_labels)
        sharedPref.all?.forEach {
            if (it.value == true) {
                val slugs = it.key.split('.')
                if (slugs.size > 1) {
                    val catIndex = categorySlugs.indexOf(slugs[0])
                    if (catIndex >= 0) {
                        val subSlugsId: Int = subCategorySlugs.getResourceId(catIndex, 0)
                        val subLabelsId: Int = subCategoryLabels.getResourceId(catIndex, 0)
                        if (subLabelsId > 0 && subLabelsId > 0) {
                            val subcategorySlugs = resources.getStringArray(subSlugsId)
                            val subcategoryLabels = resources.getStringArray(subLabelsId)
                            categories.add(subcategoryLabels[subcategorySlugs.indexOf(slugs[1])])
                        }
                    }
                }
            }
        }
        subCategorySlugs.recycle()
        subCategoryLabels.recycle()

        // display selected genres
        val currentGenres: TextView = view.findViewById(R.id.current_genres)
        if (categories.size == 0) {
            currentGenres.text = "all"
        } else {
            currentGenres.text = categories.joinToString()
        }
        // display selected city or current location
        val currentCity: TextView = view.findViewById(R.id.current_city)
        currentCity.text = city
    }

    // onclick listener for the three different filters
    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.ln_city -> mListener.FilterOptionChanged("City", cityBtn)
            R.id.ln_genre -> mListener.FilterOptionChanged("Genre", genreBtn)
            R.id.ln_date -> mListener.FilterOptionChanged("Date", dateBtn)
        }
    }

    companion object {
        fun newInstance(selectedcity: String, selecteddate: String) =
            FilterOptionsFragment().apply {
                arguments = Bundle().apply {
                    city = selectedcity
                    date = selecteddate
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
