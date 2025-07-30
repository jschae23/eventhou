package com.example.eventhou.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.user.UserDataSource
import com.example.eventhou.data.user.UserRepository
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.data.events.EventHistory
import com.example.eventhou.ui.login.LoginActivity
import com.example.eventhou.ui.tutorial.TutorialActivity
import com.facebook.drawee.view.SimpleDraweeView


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {

    private lateinit var mEventHandler: EventHandler
    lateinit var date: String

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
        val v = inflater.inflate(R.layout.fragment_profile, container, false)
        val logout: Button = v.findViewById(R.id.logout)
        val clearEventHistoryButton: Button = v.findViewById(R.id.clearEventHistory)
        val tutorial: Button = v.findViewById((R.id.tutorial))
        val appContainerOne = (requireActivity().application as EventhouApplication).appContainer
        mEventHandler = appContainerOne.eventHandler

        // Logout Button Handler
        logout.setOnClickListener {
            activity?.let {
                val loginRepository =
                    UserRepository(
                        dataSource = UserDataSource()
                    )
                loginRepository.logout();

                val intent = Intent(it, LoginActivity::class.java)
                it.startActivity(intent)
                it.finish()
            }
        }

        // Clear History Button Handler
        clearEventHistoryButton.setOnClickListener {
            activity?.let {
                EventHistory(it).removeAll()
                mEventHandler.applyFilter(requireActivity())
                Toast.makeText(
                    context,
                    "Your event history was cleared.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Tutorial Button Handler
        tutorial.setOnClickListener {
            activity?.let {
                val intent = Intent(it, TutorialActivity::class.java)
                it.startActivity(intent)
            }
        }

        // Adapts the User Data
        fun changeData(userRepository: UserRepository) {
            val currentUser = userRepository.getLoggedInUser()
            currentUser?.let {
                val displayNameTextView: TextView = v.findViewById(R.id.displayName)
                val displayNameLabel: TextView = v.findViewById(R.id.displayNameLabel)
                val displayName = currentUser.displayName
                if (displayName.isNullOrEmpty()) {
                    displayNameLabel.visibility = View.GONE
                    displayNameTextView.visibility = View.GONE
                } else {
                    displayNameLabel.visibility = View.VISIBLE
                    displayNameTextView.visibility = View.VISIBLE
                    displayNameTextView.text = displayName
                }

                val email: TextView = v.findViewById(R.id.email)
                email.text = currentUser.email

                // Set Profile Picture
                currentUser.photoUrl?.let {
                    val profilePictureImageView: SimpleDraweeView =
                        v.findViewById(R.id.profilePicture)
                    profilePictureImageView.setImageURI(currentUser.photoUrl)
                }
            }
        }

        val appContainer = (activity?.application as EventhouApplication).appContainer
        changeData(appContainer.userRepository)

        return v
    }

    companion object {
        fun newInstance() =
            ProfileFragment().apply {
            }
    }
}