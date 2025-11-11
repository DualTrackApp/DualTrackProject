package com.dualtrack.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dualtrack.app.databinding.FragmentHomeCoachBinding

class CoachHomeFragment : Fragment() {
    private var _b: FragmentHomeCoachBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentHomeCoachBinding.inflate(inflater, container, false)
        return b.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
