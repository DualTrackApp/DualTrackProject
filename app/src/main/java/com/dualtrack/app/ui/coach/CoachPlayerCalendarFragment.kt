package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentCoachPlayerCalendarBinding
import com.dualtrack.app.ui.home.HomeCard
import com.dualtrack.app.ui.home.HomeCardAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CoachPlayerCalendarFragment : Fragment() {

    private var _b: FragmentCoachPlayerCalendarBinding? = null
    private val b get() = _b!!

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var playerUid: String = ""
    private var playerEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerUid = arguments?.getString("playerUid").orEmpty()
        playerEmail = arguments?.getString("playerEmail").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentCoachPlayerCalendarBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        b.tvPlayerTitle.text =
            if (playerEmail.isBlank()) "Player Weekly Calendar" else "$playerEmail Weekly Calendar"

        b.rvWeek.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        loadWeek()
    }

    private fun loadWeek() {
        if (playerUid.isBlank()) {
            renderWeek(emptyList())
            return
        }

        db.collection("users")
            .document(playerUid)
            .collection("events")
            .get()
            .addOnSuccessListener { qs ->
                renderWeek(qs.documents)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Could not load player calendar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                renderWeek(emptyList())
            }
    }

    private fun renderWeek(docs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val startCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val cards = labels.mapIndexed { index, label ->
            val dayCal = startCal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, index)
            val dayMillis = dayCal.timeInMillis

            val matching = docs.filter { doc ->
                val storedDayMillis = doc.getLong("dayMillis")
                storedDayMillis == dayMillis
            }.sortedBy { it.getString("time").orEmpty() }

            val subtitle = when {
                matching.isEmpty() -> "No events"
                matching.size == 1 -> {
                    val first = matching.first()
                    val time = first.getString("time").orEmpty()
                    val title = first.getString("title").orEmpty()
                    val details = first.getString("details").orEmpty()
                    buildString {
                        if (time.isNotBlank()) append("$time - ")
                        append(title)
                        if (details.isNotBlank()) append(" • $details")
                    }
                }
                else -> {
                    val first = matching.first()
                    val time = first.getString("time").orEmpty()
                    val title = first.getString("title").orEmpty()
                    buildString {
                        append("${matching.size} events")
                        if (title.isNotBlank()) {
                            append(" • ")
                            if (time.isNotBlank()) append("$time - ")
                            append(title)
                        }
                    }
                }
            }

            HomeCard(
                title = label,
                subtitle = subtitle
            )
        }

        b.rvWeek.adapter = HomeCardAdapter(cards) { card ->
            Toast.makeText(requireContext(), "${card.title}: ${card.subtitle}", Toast.LENGTH_LONG).show()
        }

        b.tvWeekRange.text = buildWeekRangeText(startCal.time)
    }

    private fun buildWeekRangeText(startDate: Date): String {
        val cal = Calendar.getInstance().apply {
            time = startDate
        }
        val endCal = cal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 6)

        val fmt = SimpleDateFormat("MMM d", Locale.US)
        return "${fmt.format(cal.time)} - ${fmt.format(endCal.time)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
