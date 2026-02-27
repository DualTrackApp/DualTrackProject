package com.dualtrack.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentHomeCoachBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class CoachHomeFragment : Fragment() {

    private var _b: FragmentHomeCoachBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeCoachBinding.inflate(inflater, container, false)

        b.btnReviewForms.setOnClickListener {
            findNavController().navigate(R.id.coachFormsFragment)
        }

        b.btnReviewForms.setOnLongClickListener {
            showCoachMenu()
            true
        }

        return b.root
    }

    private fun showCoachMenu() {
        val options = arrayOf("Sign out")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Coach")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> signOut()
                }
            }
            .show()
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
        try {
            findNavController().navigate(R.id.loginFragment)
        } catch (_: Exception) {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}