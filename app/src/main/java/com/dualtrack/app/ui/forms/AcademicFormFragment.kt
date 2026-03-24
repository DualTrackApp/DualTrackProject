package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.ArrayAdapter

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.dualtrack.app.databinding.FragmentAcademicFormBinding

import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore



class AcademicFormFragment : Fragment() {



    private var _b: FragmentAcademicFormBinding? = null

    private val b get() = _b!!



    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()



    private var requestId: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        requestId = arguments?.getString("requestId").orEmpty()

    }



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentAcademicFormBinding.inflate(inflater, container, false)



        setupSpinners()

        loadExistingRequestIfNeeded()



        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnSubmitAcademic.setOnClickListener { submit() }



        return b.root

    }



    private fun setupSpinners() {

        val standingOptions = listOf(

            "Select current standing",

            "Excellent",

            "Good",

            "Needs Improvement",

            "At Risk"

        )



        val attendanceOptions = listOf(

            "Select attendance status",

            "Excellent",

            "Good",

            "Inconsistent",

            "Poor"

        )



        b.spStanding.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            standingOptions

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }



        b.spAttendance.adapter = ArrayAdapter(

            requireContext(),

            android.R.layout.simple_spinner_item,

            attendanceOptions

        ).also {

            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        }

    }



    private fun loadExistingRequestIfNeeded() {

        if (requestId.isBlank()) return



        db.collection("forms").document(requestId)

            .get()

            .addOnSuccessListener { doc ->

                if (!doc.exists()) return@addOnSuccessListener



                val status = doc.getString("status").orEmpty()

                val coachNote = doc.getString("coachNote").orEmpty()



                if (status == "submitted" || status == "approved" || status == "needs_attention") {

                    b.btnSubmitAcademic.isEnabled = false

                    b.btnSubmitAcademic.text = when (status) {

                        "submitted" -> "Already Submitted"

                        "approved" -> "Approved"

                        "needs_attention" -> "Needs Attention"

                        else -> "Already Submitted"

                    }



                    if (coachNote.isNotBlank()) {

                        Toast.makeText(

                            requireContext(),

                            "Coach note: $coachNote",

                            Toast.LENGTH_LONG

                        ).show()

                    }

                }

            }

    }



    private fun submit() {

        val currentGpa = b.etGpa.text.toString().trim()

        val classes = b.etClasses.text.toString().trim()

        val strongestClass = b.etStrongestClass.text.toString().trim()

        val weakestClass = b.etWeakestClass.text.toString().trim()

        val missingAssignments = b.etMissingAssignments.text.toString().trim()

        val currentStanding = b.spStanding.selectedItem?.toString()?.trim().orEmpty()

        val attendanceStatus = b.spAttendance.selectedItem?.toString()?.trim().orEmpty()

        val academicConcerns = b.etConcerns.text.toString().trim()

        val supportNeeded = b.etSupportNeeded.text.toString().trim()

        val professorCommunication = b.etProfessorCommunication.text.toString().trim()



        if (classes.isBlank() || currentGpa.isBlank()) {

            Toast.makeText(requireContext(), "Please complete the required academic fields.", Toast.LENGTH_SHORT).show()

            return

        }



        if (currentStanding == "Select current standing" || attendanceStatus == "Select attendance status") {

            Toast.makeText(requireContext(), "Please select standing and attendance status.", Toast.LENGTH_SHORT).show()

            return

        }



        val user = auth.currentUser ?: run {

            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()

            return

        }



        b.btnSubmitAcademic.isEnabled = false

        b.btnSubmitAcademic.text = "Submitting..."



        val answers = mapOf(

            "currentGpa" to currentGpa,

            "classesAndSchedule" to classes,

            "strongestClass" to strongestClass,

            "weakestClass" to weakestClass,

            "missingAssignments" to missingAssignments,

            "currentStanding" to currentStanding,

            "attendanceStatus" to attendanceStatus,

            "academicConcerns" to academicConcerns,

            "supportNeeded" to supportNeeded,

            "professorCommunication" to professorCommunication

        )



        if (requestId.isNotBlank()) {

            db.collection("forms").document(requestId)

                .update(

                    mapOf(

                        "data" to answers,

                        "status" to "submitted",

                        "submittedAt" to Timestamp.now()

                    )

                )

                .addOnSuccessListener {

                    Toast.makeText(requireContext(), "Academic check submitted ✓", Toast.LENGTH_SHORT).show()

                    findNavController().navigateUp()

                }

                .addOnFailureListener {

                    b.btnSubmitAcademic.isEnabled = true

                    b.btnSubmitAcademic.text = "Submit Academic Check"

                    Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()

                }

            return

        }



        db.collection("users").document(user.uid)

            .get()

            .addOnSuccessListener { snap ->

                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"

                val teamName = snap.getString("teamName") ?: ""



                val data = hashMapOf(

                    "formType" to "academic",

                    "userId" to user.uid,

                    "userEmail" to user.email,

                    "teamId" to teamId,

                    "teamName" to teamName,

                    "createdAt" to Timestamp.now(),

                    "status" to "submitted",

                    "coachNote" to "",

                    "data" to answers

                )



                db.collection("forms")

                    .add(data)

                    .addOnSuccessListener {

                        Toast.makeText(requireContext(), "Academic check submitted ✓", Toast.LENGTH_SHORT).show()

                        findNavController().navigateUp()

                    }

                    .addOnFailureListener {

                        b.btnSubmitAcademic.isEnabled = true

                        b.btnSubmitAcademic.text = "Submit Academic Check"

                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()

                    }

            }

            .addOnFailureListener {

                b.btnSubmitAcademic.isEnabled = true

                b.btnSubmitAcademic.text = "Submit Academic Check"

                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()

            }

    }



    override fun onDestroyView() {

        super.onDestroyView()

        _b = null

    }

}