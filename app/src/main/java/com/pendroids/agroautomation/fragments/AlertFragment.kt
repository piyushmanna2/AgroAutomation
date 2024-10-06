package com.pendroids.agroautomation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.pendroids.agroautomation.adapter.AlertAdapter
import com.pendroids.agroautomation.databinding.FragmentAlertBinding
import com.pendroids.agroautomation.model.AlertDataClass

class AlertFragment : Fragment() {

    private var _binding: FragmentAlertBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertAdapter: AlertAdapter
    private val alertList = mutableListOf<AlertDataClass>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        alertAdapter = AlertAdapter(alertList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertAdapter
        }

        // Fetch data from Firestore
        fetchAlertsFromFirestore()
    }

    private fun fetchAlertsFromFirestore() {
        val db = FirebaseFirestore.getInstance()

        db.collection("alerts")
            .get()
            .addOnSuccessListener { documents ->
                alertList.clear()
                for (document in documents) {
                    val alert = document.toObject(AlertDataClass::class.java)
                    alertList.add(alert)
                }
                alertAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to fetch data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
