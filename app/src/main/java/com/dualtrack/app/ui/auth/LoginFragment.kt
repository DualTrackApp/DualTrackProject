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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginFragment : Fragment() {

    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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
        val selectedRole = b.spRoleLogin.selectedItem?.toString() ?: ""

        if (email.isEmpty()) {
            b.etEmail.error = "Email is required"
            return
        }
        if (!isValidVSUEmail(email)) {
            b.etEmail.error = "Use your VSU email (@vsu.edu or @students.vsu.edu)"
            return
        }
        if (password.isEmpty()) {
            b.etPassword.error = "Password is required"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                val storedRole = user?.displayName

                if (storedRole.isNullOrBlank() && selectedRole.isNotBlank()) {
                    val updates = UserProfileChangeRequest.Builder()
                        .setDisplayName(selectedRole)
                        .build()
                    user?.updateProfile(updates)
                }

                val finalRole = if (!storedRole.isNullOrBlank()) storedRole else selectedRole

                if (finalRole.isNullOrBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Unable to determine your role. Please contact support.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                if (!finalRole.equals(selectedRole, ignoreCase = true)) {
                    Toast.makeText(
                        requireContext(),
                        "This account is registered as $finalRole. Switch role to log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                createOrUpdateUserDoc(
                    uid = user?.uid,
                    email = email,
                    role = finalRole
                ) { ok ->
                    if (!ok) return@createOrUpdateUserDoc

                    if (finalRole.equals("Coach", ignoreCase = true)) {
                        findNavController().navigate(R.id.action_login_to_coachHome)
                    } else {
                        findNavController().navigate(R.id.action_login_to_athleteHome)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun createOrUpdateUserDoc(uid: String?, email: String, role: String, onDone: (Boolean) -> Unit) {
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Login error: missing user ID.", Toast.LENGTH_SHORT).show()
            onDone(false)
            return
        }

        val data = hashMapOf(
            "userId" to uid,
            "email" to email,
            "role" to role.lowercase(),
            "updatedAt" to Timestamp.now()
        )

        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    data["createdAt"] = Timestamp.now()
                }
                userRef.set(data, SetOptions.merge())
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Firestore write failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        onDone(false)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Firestore read failed: ${it.message}", Toast.LENGTH_SHORT).show()
                onDone(false)
            }
    }

    private fun isValidVSUEmail(email: String): Boolean {
        return email.endsWith("@vsu.edu", ignoreCase = true) ||
                email.endsWith("@students.vsu.edu", ignoreCase = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

