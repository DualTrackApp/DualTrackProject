package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import androidx.recyclerview.widget.LinearLayoutManager

import com.dualtrack.app.R

import com.dualtrack.app.databinding.FragmentCoachFormsBinding

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.ListenerRegistration



class CoachFormsFragment : Fragment() {



    private var _b: FragmentCoachFormsBinding? = null

    private val b get() = _b!!



    private val db = FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()



    private var formsListener: ListenerRegistration? = null



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentCoachFormsBinding.inflate(inflater, container, false)



        b.btnBack.setOnClickListener { findNavController().navigateUp() }



        b.rvRequestedForms.layoutManager = LinearLayoutManager(requireContext())

        b.rvSubmittedForms.layoutManager = LinearLayoutManager(requireContext())



        b.rvRequestedForms.adapter = CoachFormsAdapter(emptyList()) { }

        b.rvSubmittedForms.adapter = CoachFormsAdapter(emptyList()) { }



        loadTeamForms()



        return b.root

    }



    private fun loadTeamForms() {

        val uid = auth.currentUser?.uid ?: return



        db.collection("users").document(uid)

            .get()

            .addOnSuccessListener { snap ->

                val teamId = snap.getString("teamId")?.trim().orEmpty()

                if (teamId.isBlank()) {

                    renderLists(emptyList(), emptyList())

                    return@addOnSuccessListener

                }



                formsListener?.remove()

                formsListener = db.collection("forms")

                    .whereEqualTo("teamId", teamId)

                    .addSnapshotListener { snapshot, e ->

                        if (_b == null) return@addSnapshotListener



                        if (e != null) {

                            Toast.makeText(

                                requireContext(),

                                "Error loading forms: ${e.message}",

                                Toast.LENGTH_LONG

                            ).show()

                            return@addSnapshotListener

                        }



                        val allItems = snapshot?.documents?.map { doc ->

                            CoachFormItem(

                                id = doc.id,

                                athleteEmail = doc.getString("userEmail") ?: "",

                                formType = doc.getString("formType") ?: "",

                                status = doc.getString("status") ?: "pending",

                                dueDate = doc.getString("dueDate") ?: "",

                                requestInstructions = doc.getString("requestInstructions") ?: "",

                                section = when (doc.getString("status")) {

                                    "requested" -> "requested"

                                    "submitted", "pending", "approved", "needs_attention" -> "submitted"

                                    else -> "submitted"

                                },

                                createdAtLabel = doc.getTimestamp("createdAt")?.toDate()?.toString() ?: ""

                            )

                        }.orEmpty()



                        val requestedItems = allItems

                            .filter { it.status == "requested" }

                            .sortedByDescending { it.createdAtLabel }



                        val submittedItems = allItems

                            .filter { it.status != "requested" }

                            .sortedByDescending { it.createdAtLabel }



                        renderLists(requestedItems, submittedItems)

                    }

            }

            .addOnFailureListener {

                if (_b == null) return@addOnFailureListener

                renderLists(emptyList(), emptyList())

            }

    }



    private fun renderLists(

        requestedItems: List<CoachFormItem>,

        submittedItems: List<CoachFormItem>

    ) {

        b.rvRequestedForms.adapter = CoachFormsAdapter(requestedItems) { item ->

            val args = Bundle().apply {

                putString("formId", item.id)

            }

            findNavController().navigate(R.id.coachFormDetailFragment, args)

        }



        b.rvSubmittedForms.adapter = CoachFormsAdapter(submittedItems) { item ->

            val args = Bundle().apply {

                putString("formId", item.id)

            }

            findNavController().navigate(R.id.coachFormDetailFragment, args)

        }

    }



    override fun onDestroyView() {

        formsListener?.remove()

        formsListener = null

        _b = null

        super.onDestroyView()

    }

}