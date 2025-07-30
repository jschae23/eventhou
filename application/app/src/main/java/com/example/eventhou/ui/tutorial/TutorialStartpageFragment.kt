package com.example.eventhou.ui.tutorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.eventhou.R

/**
 * Startpage of the App Tutorial as Fragment
 */
class TutorialStartpageFragment : Fragment() {
    lateinit var button: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v: View = inflater.inflate(R.layout.fragment_tutorial_startpage, container, false)
        button = v.findViewById(R.id.next)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button.setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.tutorial_startpage,
                    TutorialGifFragment()
                ).addToBackStack(null)
                .commit()
        }
    }
}