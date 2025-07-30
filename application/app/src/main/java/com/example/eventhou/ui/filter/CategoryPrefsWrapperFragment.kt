package com.example.eventhou.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.eventhou.R


class CategoryPrefsWrapperFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter_categories, container, false)

        // call a new instance of CategoryPrefFragment
        val fragmentGenre =
            CategoryPrefsFragment.newInstance()
        childFragmentManager.beginTransaction().replace(
            R.id.preferences_container,
            fragmentGenre,
            fragmentGenre.javaClass.getSimpleName()
        ).commit()


        return view
    }

    companion object {
        fun newInstance() =
            CategoryPrefsWrapperFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
