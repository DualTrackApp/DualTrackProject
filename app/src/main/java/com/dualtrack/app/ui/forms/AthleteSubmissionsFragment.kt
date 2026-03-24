package com.dualtrack.app.ui.forms



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import androidx.fragment.app.Fragment

import androidx.recyclerview.widget.LinearLayoutManager

import com.dualtrack.app.databinding.FragmentAthleteSubmissionsBinding

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.Query



class AthleteSubmissionsFragment : Fragment() {



    private var _b: FragmentAthleteSubmissionsBinding? = null

    private val b get() = _b!!



    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentAthleteSubmissionsBinding.inflate(inflater, container, false)



        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())



        b.btnBack.setOnClickListener {

            requireActivity().onBackPressedDispatcher.onBackPressed()

        }



        loadMyForms()



        return b.root

    }



    private fun loadMyForms() {

        val user = auth.currentUser ?: return



        db.collection("forms")

            .whereEqualTo("userId", user.uid)

            .orderBy("createdAt", Query.Direction.DESCENDING)

            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null || _b == null) return@addSnapshotListener



                val items = snapshot.documents.map { doc ->

                    FormItem(

                        id = doc.id,

                        formType = doc.getString("formType") ?: "",

                        status = doc.getString("status") ?: "pending",

                        createdAt = doc.getTimestamp("createdAt")

                    )

                }



                val unseenReviewedDocs = snapshot.documents.filter { doc ->

                    val status = doc.getString("status").orEmpty()

                    val seen = doc.getBoolean("athleteStatusSeen") == true

                    (status == "approved" || status == "needs_attention") && !seen

                }



                val reviewedCount = unseenReviewedDocs.size



                b.tvReviewedNotice.visibility =

                    if (reviewedCount > 0) View.VISIBLE else View.GONE



                b.tvReviewedNotice.text = when (reviewedCount) {

                    1 -> "1 form has been reviewed. Check your submission statuses below."

                    else -> "$reviewedCount forms have been reviewed. Check your submission statuses below."

                }



                b.recyclerView.adapter = AthleteFormsAdapter(items)



                if (reviewedCount > 0) {

                    markReviewedFormsAsSeen(unseenReviewedDocs.map { it.id })

                }

            }

    }



    private fun markReviewedFormsAsSeen(docIds: List<String>) {

        docIds.forEach { docId ->

            db.collection("forms")

                .document(docId)

                .update("athleteStatusSeen", true)

        }

    }



    override fun onDestroyView() {

        super.onDestroyView()

        _b = null

    }

}
