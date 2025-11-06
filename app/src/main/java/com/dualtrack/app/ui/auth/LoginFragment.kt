package com.dualtrack.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentLoginBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnLogin.setOnClickListener { findNavController().navigate(R.id.homeFragment) }
        b.btnRegisterNav.setOnClickListener { findNavController().navigate(R.id.registerFragment) }
        b.btnForgotPasswordNav.setOnClickListener { findNavController().navigate(R.id.forgotPasswordFragment) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
