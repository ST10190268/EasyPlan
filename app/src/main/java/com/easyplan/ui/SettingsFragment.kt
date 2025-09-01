package com.easyplan.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.easyplan.R
import com.easyplan.util.ThemeUtils
import com.google.android.material.chip.Chip

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Language chips
        view.findViewById<Chip>(R.id.chipEnglish).setOnClickListener {
            Toast.makeText(requireContext(), "English selected", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Chip>(R.id.chipAfrikaans).setOnClickListener {
            Toast.makeText(requireContext(), "Afrikaans coming soon", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Chip>(R.id.chipZulu).setOnClickListener {
            Toast.makeText(requireContext(), "Zulu coming soon", Toast.LENGTH_SHORT).show()
        }

        // Theme chips
        view.findViewById<Chip>(R.id.chipThemeSystem).setOnClickListener {
            ThemeUtils.saveTheme(requireContext(), ThemeUtils.ThemeMode.SYSTEM)
            Toast.makeText(requireContext(), "Using system theme", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Chip>(R.id.chipThemeLight).setOnClickListener {
            ThemeUtils.saveTheme(requireContext(), ThemeUtils.ThemeMode.LIGHT)
            Toast.makeText(requireContext(), "Light theme applied", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Chip>(R.id.chipThemeDark).setOnClickListener {
            ThemeUtils.saveTheme(requireContext(), ThemeUtils.ThemeMode.DARK)
            Toast.makeText(requireContext(), "Dark theme applied", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}

