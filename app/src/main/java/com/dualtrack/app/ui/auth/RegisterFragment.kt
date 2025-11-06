package com.dualtrack.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _b: FragmentRegisterBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentRegisterBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnContinueRegister.setOnClickListener { findNavController().navigate(R.id.loginFragment) }
        b.btnBackRegister.setOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
