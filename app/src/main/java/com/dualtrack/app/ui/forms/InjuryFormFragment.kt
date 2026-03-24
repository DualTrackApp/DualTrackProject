package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.ArrayAdapter

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.dualtrack.app.databinding.FragmentInjuryFormBinding

import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore



class InjuryFormFragment : Fragment() {



    private var _b: FragmentInjuryFormBinding? = null

    private val b get() = _b!!



    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentInjuryFormBinding.inflate(inflater, container, false)



        setupSpinners()



        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnSubmitInjury.setOnClickListener { submit() }



        return b.root

    }



    private fun setupSpinners() {

        val injuryTypes = listOf(

            "Select injury type",

            "Sprain",

            "Strain",

            "Fracture",

            "Concussion",

            "Bruise",

            "Dislocation",

            "Other"

        )



        val severityLevels = listOf(

            "Select severity",

            "Mild",

            "Moderate",

            "Severe"

        )



        val medicalAttention = listOf(

            "Medical attention received?",

            "Yes",

            "No"

        )



        b.spInjuryType.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            injuryTypes

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }



        b.spSeverity.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            severityLevels

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }



        b.spMedicalAttention.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            medicalAttention

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }

    }



    private fun submit() {

        val injuryType = b.spInjuryType.selectedItem?.toString()?.trim().orEmpty()

        val bodyPart = b.etBodyPart.text.toString().trim()

        val injuryDate = b.etInjuryDate.text.toString().trim()

        val injuryLocation = b.etInjuryLocation.text.toString().trim()

        val howOccurred = b.etHowOccurred.text.toString().trim()

        val severity = b.spSeverity.selectedItem?.toString()?.trim().orEmpty()

        val painLevel = b.etPainLevel.text.toString().trim()

        val medical = b.spMedicalAttention.selectedItem?.toString()?.trim().orEmpty()

        val clearedBy = b.etClearedBy.text.toString().trim()

        val returnTimeline = b.etReturnTimeline.text.toString().trim()

        val notes = b.etNotes.text.toString().trim()



        if (injuryType == "Select injury type" || severity == "Select severity") {

            Toast.makeText(requireContext(), "Please select injury type and severity.", Toast.LENGTH_SHORT).show()

            return

        }



        if (bodyPart.isBlank() || injuryDate.isBlank() || injuryLocation.isBlank() || howOccurred.isBlank()) {

            Toast.makeText(requireContext(), "Please complete the required injury details.", Toast.LENGTH_SHORT).show()

            return

        }



        if (medical == "Medical attention received?") {

            Toast.makeText(requireContext(), "Please select whether medical attention was received.", Toast.LENGTH_SHORT).show()

            return

        }



        val user = auth.currentUser ?: run {

            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()

            return

        }



        b.btnSubmitInjury.isEnabled = false

        b.btnSubmitInjury.text = "Submitting..."



        db.collection("users").document(user.uid)

            .get()

            .addOnSuccessListener { snap ->

                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"

                val teamName = snap.getString("teamName") ?: ""



                val data = hashMapOf(

                    "formType" to "injury",

                    "userId" to user.uid,

                    "userEmail" to user.email,

                    "teamId" to teamId,

                    "teamName" to teamName,

                    "createdAt" to Timestamp.now(),

                    "status" to "pending",

                    "coachNote" to "",

                    "data" to mapOf(

                        "injuryType" to injuryType,

                        "bodyPart" to bodyPart,

                        "injuryDate" to injuryDate,

                        "injuryLocation" to injuryLocation,

                        "howOccurred" to howOccurred,

                        "severity" to severity,

                        "painLevel" to painLevel,

                        "medicalAttention" to medical,

                        "clearedBy" to clearedBy,

                        "returnTimeline" to returnTimeline,

                        "additionalNotes" to notes

                    )

                )



                db.collection("forms")

                    .add(data)

                    .addOnSuccessListener {

                        Toast.makeText(requireContext(), "Injury report submitted ✓", Toast.LENGTH_SHORT).show()

                        findNavController().navigateUp()

                    }

                    .addOnFailureListener {

                        b.btnSubmitInjury.isEnabled = true

                        b.btnSubmitInjury.text = "Submit Injury Report"

                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()

                    }

            }

            .addOnFailureListener {

                b.btnSubmitInjury.isEnabled = true

                b.btnSubmitInjury.text = "Submit Injury Report"

                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()

            }

    }



    override fun onDestroyView() {

        super.onDestroyView()

        _b = null

    }

}

