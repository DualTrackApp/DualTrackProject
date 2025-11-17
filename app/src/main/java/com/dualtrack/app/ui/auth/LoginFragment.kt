package com.dualtrack.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
        val roles = listOf("Athlete", "Coach")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spRoleLogin.adapter = roleAdapter

        b.btnLogin.setOnClickListener {
            val selectedRole = b.spRoleLogin.selectedItem.toString()

            if (selectedRole == "Athlete") {
                findNavController().navigate(R.id.action_login_to_athleteHome)
            } else {
                findNavController().navigate(R.id.action_login_to_coachHome)
            }
        }

        b.btnRegisterNav.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        b.btnForgotPasswordNav.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
