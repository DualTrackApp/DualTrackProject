package com.dualtrack.app.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentDayEventsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DayEventsFragment : Fragment() {

    private var _b: FragmentDayEventsBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var reg: ListenerRegistration? = null

    private val adapter = DayEventsAdapter { event ->
        val message = buildString {
            append(event.title)
            if (event.time.isNotBlank()) append("\nTime: ${event.time}")
            if (event.details.isNotBlank()) append("\nDetails: ${event.details}")
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private var dayMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentDayEventsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dayMillis = normalizeDayMillis(arguments?.getLong("dayMillis") ?: 0L)

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        b.btnAddEvent.setOnClickListener {
            val args = Bundle().apply { putLong("dayMillis", dayMillis) }
            findNavController().navigate(R.id.action_dayEvents_to_addEvent, args)
        }

        b.rvDayEvents.layoutManager = LinearLayoutManager(requireContext())
        b.rvDayEvents.adapter = adapter

        b.tvDayTitle.text = formatDayLabel(dayMillis)

        listenForEvents()
    }

    private fun listenForEvents() {
        val uid = auth.currentUser?.uid ?: return

        reg?.remove()
        reg = db.collection("users")
            .document(uid)
            .collection("events")
            .whereEqualTo("dayMillis", dayMillis)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    adapter.submitList(emptyList())
                    setEmpty(true)
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val details = d.getString("details") ?: ""
                    val time = d.getString("time") ?: ""
                    val dm = d.getLong("dayMillis") ?: dayMillis

                    CalendarEvent(
                        id = d.id,
                        title = title,
                        details = details,
                        time = time,
                        dayMillis = dm
                    )
                }.sortedBy { it.time }

                adapter.submitList(list)
                setEmpty(list.isEmpty())
            }
    }

    private fun setEmpty(isEmpty: Boolean) {
        b.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvDayEvents.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun formatDayLabel(ms: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove()
        reg = null
        _b = null
    }
}
