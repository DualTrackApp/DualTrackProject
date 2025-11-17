package com.dualtrack.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val roles = listOf("Athlete", "Coach")
        val roleAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spRoleLogin.adapter = roleAdapter

        b.btnLogin.setOnClickListener { loginUser() }

        b.btnRegisterNav.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        b.btnForgotPasswordNav.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }
    }

    private fun loginUser() {
        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString().trim()
        val role = b.spRoleLogin.selectedItem?.toString() ?: ""

        if (email.isEmpty()) {
            b.etEmail.error = "Required"
            return
        }
        if (!email.endsWith("@vsu.edu")) {
            b.etEmail.error = "Use your VSU email"
            return
        }
        if (password.isEmpty()) {
            b.etPassword.error = "Required"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (role == "Coach") {
                    findNavController().navigate(R.id.action_login_to_coachHome)
                } else {
                    findNavController().navigate(R.id.action_login_to_athleteHome)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
