package com.dualtrack.app.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentAddEventBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddEventFragment : Fragment() {

    private var _b: FragmentAddEventBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAddEventBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        b.btnSave.setOnClickListener {
            saveEvent()
        }
    }

    private fun setupSpinners() {
        val categories = listOf("Class", "Practice", "Game", "Study Hall", "Meeting", "Assignment", "Other")
        val statuses = listOf("Upcoming", "Completed", "Missed")

        b.spCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        b.spStatus.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statuses
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun saveEvent() {
        val uid = auth.currentUser?.uid ?: return

        val rawDayMillis = arguments?.getLong("dayMillis") ?: 0L
        val dayMillis = normalizeDayMillis(rawDayMillis)

        val title = b.etTitle.text?.toString()?.trim().orEmpty()
        val time = b.etTime.text?.toString()?.trim().orEmpty()
        val details = b.etDetails.text?.toString()?.trim().orEmpty()
        val category = b.spCategory.selectedItem?.toString().orEmpty()
        val required = b.cbRequired.isChecked
        val status = b.spStatus.selectedItem?.toString().orEmpty()

        if (title.isBlank()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Missing title")
                .setMessage("Please enter an event title.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val data = hashMapOf(
            "title" to title,
            "time" to time,
            "details" to details,
            "category" to category,
            "required" to required,
            "status" to status,
            "dayMillis" to dayMillis,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .collection("events")
            .add(data)
            .addOnSuccessListener {
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Save failed")
                    .setMessage(e.message ?: "Unknown error")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    private fun normalizeDayMillis(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}




