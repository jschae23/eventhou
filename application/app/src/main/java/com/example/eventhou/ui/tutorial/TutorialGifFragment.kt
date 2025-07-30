package com.example.eventhou.ui.tutorial

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.eventhou.ui.MainActivity
import com.example.eventhou.R
import pl.droidsonroids.gif.GifImageView

/**
 * Tutorial Fragment that shows different functionalities of the app  with a gif
 */
class TutorialGifFragment : Fragment() {
    lateinit var buttonNext: Button
    lateinit var buttonBack: Button
    lateinit var gifView: GifImageView
    lateinit var header: TextView
    var imageResource = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        imageResource = R.drawable.tutorial_swipe
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v: View = inflater.inflate(R.layout.fragment_tutorial_gif_view, container, false)
        buttonNext = v.findViewById(R.id.next)
        buttonBack = v.findViewById(R.id.back)
        gifView = v.findViewById(R.id.gifView)
        gifView.setImageResource(imageResource)
        header = v.findViewById(R.id.text)
        header.setText(R.string.tutorial_swipe)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // check which gif is shown at the moment and change to the next or previous one accordingly
        buttonNext.setOnClickListener {
            when (imageResource) {
                R.drawable.tutorial_swipe -> {
                    imageResource = R.drawable.tutorial_filter
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_filter)
                }
                R.drawable.tutorial_filter -> {
                    imageResource = R.drawable.tutorial_favourites
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_favourites)
                }
                R.drawable.tutorial_favourites -> {
                    imageResource = R.drawable.tutorial_profile
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_profile)
                    buttonNext.setText(R.string.button_done)
                }
                R.drawable.tutorial_profile -> {
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        buttonBack.setOnClickListener {
            when (imageResource) {
                R.drawable.tutorial_profile -> {
                    imageResource = R.drawable.tutorial_favourites
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_favourites)
                    buttonNext.setText(R.string.button_next)
                }
                R.drawable.tutorial_favourites -> {
                    imageResource = R.drawable.tutorial_filter
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_filter)
                }
                R.drawable.tutorial_filter -> {
                    imageResource = R.drawable.tutorial_swipe
                    gifView.setImageResource(imageResource)
                    header.setText(R.string.tutorial_swipe)
                }
                R.drawable.tutorial_swipe ->
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.tutorial_gif_view,
                            TutorialStartpageFragment()
                        ).addToBackStack(null)
                        .commit()
            }
        }
    }
}