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
import com.dualtrack.app.databinding.FragmentRegisterBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RegisterFragment : Fragment() {

    private var _b: FragmentRegisterBinding? = null
    private val b get() = _b!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentRegisterBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roles = listOf("Athlete", "Coach")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spRoleRegister.adapter = roleAdapter

        b.btnContinueRegister.setOnClickListener { registerUser() }

        b.btnBackRegister.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun registerUser() {
        val email = b.etEmailRegister.text.toString().trim()
        val password = b.etPasswordRegister.text.toString().trim()
        val confirmPassword = b.etConfirmPasswordRegister.text.toString().trim()
        val role = b.spRoleRegister.selectedItem?.toString() ?: ""

        if (email.isEmpty()) {
            b.etEmailRegister.error = "Email is required"
            return
        }
        if (!isValidVSUEmail(email)) {
            b.etEmailRegister.error = "Use your VSU email (@vsu.edu or @students.vsu.edu)"
            return
        }

        if (password.isEmpty()) {
            b.etPasswordRegister.error = "Password is required"
            return
        }
        if (!isStrongPassword(password)) {
            b.etPasswordRegister.error =
                "Password must be 8+ chars with upper, lower, number, and symbol"
            return
        }

        if (confirmPassword.isEmpty()) {
            b.etConfirmPasswordRegister.error = "Confirm your password"
            return
        }
        if (confirmPassword != password) {
            b.etConfirmPasswordRegister.error = "Passwords do not match"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser

                // (Your existing pattern) store the chosen role in displayName
                if (user != null && role.isNotBlank()) {
                    val updates = UserProfileChangeRequest.Builder()
                        .setDisplayName(role)
                        .build()
                    user.updateProfile(updates)
                }

                createUserDoc(
                    uid = user?.uid,
                    email = email,
                    role = role
                ) { ok ->
                    if (!ok) return@createUserDoc

                    if (role.equals("Coach", ignoreCase = true)) {
                        findNavController().navigate(R.id.action_register_to_coachHome)
                    } else {
                        findNavController().navigate(R.id.action_register_to_athleteHome)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUserDoc(
        uid: String?,
        email: String,
        role: String,
        onDone: (Boolean) -> Unit
    ) {
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Registration error: missing user ID.", Toast.LENGTH_SHORT).show()
            onDone(false)
            return
        }

        val data = hashMapOf<String, Any>(
            "userId" to uid,
            "email" to email,
            "role" to role.lowercase(),
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Firestore write failed: ${it.message}", Toast.LENGTH_SHORT).show()
                onDone(false)
            }
    }

    private fun isValidVSUEmail(email: String): Boolean {
        return email.endsWith("@vsu.edu", ignoreCase = true) ||
                email.endsWith("@students.vsu.edu", ignoreCase = true)
    }

    private fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (c in password) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                !c.isLetterOrDigit() -> hasSymbol = true
            }
        }

        return hasUpper && hasLower && hasDigit && hasSymbol
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}