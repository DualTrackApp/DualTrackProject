package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.LinearLayout

import android.widget.TextView

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.dualtrack.app.R

import com.dualtrack.app.databinding.FragmentCoachFormDetailBinding

import com.google.firebase.firestore.FirebaseFirestore



class CoachFormDetailFragment : Fragment() {



    private var _b: FragmentCoachFormDetailBinding? = null

    private val b get() = _b!!



    private val db = FirebaseFirestore.getInstance()



    private var formId: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        formId = arguments?.getString("formId").orEmpty()

    }



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentCoachFormDetailBinding.inflate(inflater, container, false)

        return b.root

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)



        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnApprove.setOnClickListener { updateStatus("approved", "Approved by coach") }

        b.btnNeedsAttention.setOnClickListener { updateStatus("needs_attention", "Needs attention") }



        if (formId.isBlank()) {

            Toast.makeText(requireContext(), "Missing form id.", Toast.LENGTH_SHORT).show()

            findNavController().navigateUp()

            return

        }



        loadForm()

    }



    private fun loadForm() {

        db.collection("forms").document(formId)

            .get()

            .addOnSuccessListener { doc ->

                if (!doc.exists()) {

                    Toast.makeText(requireContext(), "Form not found.", Toast.LENGTH_SHORT).show()

                    findNavController().navigateUp()

                    return@addOnSuccessListener

                }



                val athleteEmail = doc.getString("userEmail").orEmpty()

                val formType = doc.getString("formType").orEmpty()

                val status = doc.getString("status").orEmpty()

                val dueDate = doc.getString("dueDate").orEmpty()

                val instructions = doc.getString("requestInstructions").orEmpty()

                val coachNote = doc.getString("coachNote").orEmpty()



                b.tvAthleteEmail.text = athleteEmail

                b.tvFormType.text = prettifyFormType(formType)

                b.tvStatus.text = "Status: ${prettifyStatus(status)}"

                b.tvDueDate.text = if (dueDate.isBlank()) "Due: —" else "Due: $dueDate"

                b.tvInstructions.text = if (instructions.isBlank()) "Instructions: —" else "Instructions: $instructions"

                b.etCoachNote.setText(coachNote)



                b.layoutAnswers.removeAllViews()



                val dataMap = doc.get("data") as? Map<*, *>

                if (dataMap.isNullOrEmpty()) {

                    addAnswerRow("Submission", "No submitted answers yet")

                } else {

                    dataMap.forEach { (key, value) ->

                        addAnswerRow(prettifyKey(key.toString()), value?.toString().orEmpty())

                    }

                }



                val canReview = status != "requested"

                b.btnApprove.isEnabled = canReview

                b.btnNeedsAttention.isEnabled = canReview



                if (!canReview) {

                    Toast.makeText(

                        requireContext(),

                        "This form was sent, but the athlete has not submitted it yet.",

                        Toast.LENGTH_LONG

                    ).show()

                }

            }

            .addOnFailureListener { e ->

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()

            }

    }



    private fun addAnswerRow(label: String, value: String) {

        val container = LinearLayout(requireContext()).apply {

            orientation = LinearLayout.VERTICAL

            val pad = (10 * resources.displayMetrics.density).toInt()

            setPadding(0, pad, 0, pad)

        }



        val titleView = TextView(requireContext()).apply {

            text = label

            textSize = 15f

            setTextColor(resources.getColor(android.R.color.white, null))

        }



        val valueView = TextView(requireContext()).apply {

            text = if (value.isBlank()) "—" else value

            textSize = 14f

            setTextColor(resources.getColor(android.R.color.white, null))

        }



        container.addView(titleView)

        container.addView(valueView)

        b.layoutAnswers.addView(container)

    }



    private fun updateStatus(status: String, defaultNote: String) {

        val note = b.etCoachNote.text.toString().trim().ifBlank { defaultNote }



        db.collection("forms").document(formId)

            .update(

                mapOf(

                    "status" to status,

                    "coachNote" to note,

                    "athleteStatusSeen" to false

                )

            )

            .addOnSuccessListener {

                Toast.makeText(requireContext(), "Updated ✓", Toast.LENGTH_SHORT).show()

                loadForm()

            }

            .addOnFailureListener { e ->

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()

            }

    }



    private fun prettifyFormType(value: String): String {

        return when (value) {

            "academic" -> "Academic Check"

            "wellness" -> "Wellness Check"

            "atRisk" -> "At-Risk Alert"

            "absence" -> "Absence Form"

            "injury" -> "Injury Report"

            else -> value.replaceFirstChar { it.uppercase() }

        }

    }



    private fun prettifyStatus(value: String): String {

        return when (value) {

            "requested" -> "Requested"

            "submitted" -> "Submitted"

            "approved" -> "Approved"

            "needs_attention" -> "Needs Attention"

            "pending" -> "Pending"

            else -> value.replace("_", " ")

        }

    }



    private fun prettifyKey(value: String): String {

        return value

            .replace(Regex("([a-z])([A-Z])"), "$1 $2")

            .replaceFirstChar { it.uppercase() }

    }



    override fun onDestroyView() {

        _b = null

        super.onDestroyView()

    }

}

