package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.ArrayAdapter

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.dualtrack.app.databinding.FragmentWellnessFormBinding

import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore



class WellnessFormFragment : Fragment() {



    private var _b: FragmentWellnessFormBinding? = null

    private val b get() = _b!!



    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentWellnessFormBinding.inflate(inflater, container, false)



        setupSpinners()



        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnSubmitWellness.setOnClickListener { submit() }



        return b.root

    }



    private fun setupSpinners() {

        val sleepQualityOptions = listOf(

            "Select sleep quality",

            "Excellent",

            "Good",

            "Fair",

            "Poor"

        )



        val stressOptions = listOf(

            "Select stress level",

            "Low",

            "Moderate",

            "High"

        )



        b.spSleepQuality.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            sleepQualityOptions

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }



        b.spStressLevel.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            stressOptions

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }

    }



    private fun submit() {

        val mood = b.etMood.text.toString().trim()

        val energy = b.etEnergy.text.toString().trim()

        val sleepHours = b.etSleepHours.text.toString().trim()

        val sleepQuality = b.spSleepQuality.selectedItem?.toString()?.trim().orEmpty()

        val soreness = b.etSoreness.text.toString().trim()

        val hydration = b.etHydration.text.toString().trim()

        val stressLevel = b.spStressLevel.selectedItem?.toString()?.trim().orEmpty()

        val appetite = b.etAppetite.text.toString().trim()

        val concerns = b.etConcerns.text.toString().trim()



        if (mood.isBlank() || energy.isBlank() || sleepHours.isBlank()) {

            Toast.makeText(requireContext(), "Please complete the required wellness fields.", Toast.LENGTH_SHORT).show()

            return

        }



        if (sleepQuality == "Select sleep quality" || stressLevel == "Select stress level") {

            Toast.makeText(requireContext(), "Please select sleep quality and stress level.", Toast.LENGTH_SHORT).show()

            return

        }



        val user = auth.currentUser ?: run {

            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()

            return

        }



        b.btnSubmitWellness.isEnabled = false

        b.btnSubmitWellness.text = "Submitting..."



        db.collection("users").document(user.uid)

            .get()

            .addOnSuccessListener { snap ->

                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"

                val teamName = snap.getString("teamName") ?: ""



                val data = hashMapOf(

                    "formType" to "wellness",

                    "userId" to user.uid,

                    "userEmail" to user.email,

                    "teamId" to teamId,

                    "teamName" to teamName,

                    "createdAt" to Timestamp.now(),

                    "status" to "pending",

                    "coachNote" to "",

                    "data" to mapOf(

                        "mood" to mood,

                        "energyLevel" to energy,

                        "sleepHours" to sleepHours,

                        "sleepQuality" to sleepQuality,

                        "sorenessLevel" to soreness,

                        "hydrationStatus" to hydration,

                        "stressLevel" to stressLevel,

                        "appetiteStatus" to appetite,

                        "additionalConcerns" to concerns

                    )

                )



                db.collection("forms")

                    .add(data)

                    .addOnSuccessListener {

                        Toast.makeText(requireContext(), "Wellness check submitted ✓", Toast.LENGTH_SHORT).show()

                        findNavController().navigateUp()

                    }

                    .addOnFailureListener {

                        b.btnSubmitWellness.isEnabled = true

                        b.btnSubmitWellness.text = "Submit Wellness Check"

                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()

                    }

            }

            .addOnFailureListener {

                b.btnSubmitWellness.isEnabled = true

                b.btnSubmitWellness.text = "Submit Wellness Check"

                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()

            }

    }



    override fun onDestroyView() {

        super.onDestroyView()

        _b = null

    }

}

