package com.example.eventhou.ui.tutorial


import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.example.eventhou.R

/**
 * Activity for App Tutorial
 */
class TutorialActivity : AppCompatActivity() {

    lateinit var toolbar: ActionBar
    lateinit var manager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        toolbar = supportActionBar!!
        toolbar.hide()
        manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(
            R.id.activity_tutorial,
            TutorialStartpageFragment()
        ).addToBackStack(null).commit()

        /*
         * without the extra button interaction the next button on the tutorial startpage doesn't
         * work a second time because of backstack problems for nested fragments
         */
        val button = findViewById<Button>(R.id.next)
        button.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.activity_tutorial,
                    TutorialGifFragment()
                ).addToBackStack(null)
                .commit()
        }
    }
}

