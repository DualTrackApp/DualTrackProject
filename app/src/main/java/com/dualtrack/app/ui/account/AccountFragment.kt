package com.dualtrack.app.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    private var _b: FragmentAccountBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAccountBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.tvAccountEmail.text = auth.currentUser?.email ?: "Not signed in"

        b.btnSignOut.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.loginFragment)
        }

        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}