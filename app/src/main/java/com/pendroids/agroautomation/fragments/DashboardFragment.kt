package com.pendroids.agroautomation.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.pendroids.agroautomation.R
import com.pendroids.agroautomation.databinding.FragmentDashboardBinding
import com.pendroids.agroautomation.model.DashboardDataClass
import com.pendroids.agroautomation.model.ImageDataClass

/**
 * DashboardFragment: Displays the dashboard with real-time agricultural data.
 * This fragment handles location permissions, location updates, and UI updates from Firestore.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val DOCUMENT_ID = "jbgqdGbHOhuMmoYkE7bi"
        private const val TAG = "DashboardFragment"
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                getCurrentLocation()
            }
            else -> {
                showSnackbar("Location permission is required for optimal functionality.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLocationServices()
        checkLocationInDatabase()
        fetchDashboardData()
    }

    /**
     * Initializes location services.
     */
    private fun initLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    /**
     * Checks if location data is already available in the database.
     */
    private fun checkLocationInDatabase() {
        firestore.collection("stats").document(DOCUMENT_ID).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val latitude = documentSnapshot.getDouble("lat") ?: 0.0
                    val longitude = documentSnapshot.getDouble("long") ?: 0.0
                    if (latitude == 0.0 && longitude == 0.0) {
                        getCurrentLocation()
                    }
                } else {
                    getCurrentLocation()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching location", e)
                showSnackbar("Failed to check location data. Please try again.")
            }
    }

    /**
     * Retrieves the current location if permissions are granted and location is enabled.
     */
    private fun getCurrentLocation() {
        when {
            checkLocationPermission() -> {
                if (isLocationEnabled()) {
                    requestLocationUpdate()
                } else {
                    promptEnableLocation()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showSnackbar("Location permission is required for optimal functionality.")
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    /**
     * Checks if location permissions are granted.
     */
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if location services are enabled on the device.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Requests a single location update from the FusedLocationProviderClient.
     */
    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                storeLocationToDatabase(it.latitude, it.longitude)
            }
        }
    }

    /**
     * Prompts the user to enable location services.
     */
    private fun promptEnableLocation() {
        showSnackbar("Please enable location services for optimal functionality.") {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    /**
     * Requests location permissions from the user.
     */
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Stores the location data in the Firestore database.
     */
    private fun storeLocationToDatabase(latitude: Double, longitude: Double) {
        val location = mapOf(
            "lat" to latitude,
            "long" to longitude
        )
        firestore.collection("stats").document(DOCUMENT_ID)
            .update(location)
            .addOnSuccessListener {
                showToast("Location updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error writing document", e)
                showSnackbar("Failed to update location. Please try again.")
            }
    }

    /**
     * Fetches dashboard data from Firestore and updates the UI.
     */
    private fun fetchDashboardData() {
        firestore.collection("stats").document(DOCUMENT_ID).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val data = documentSnapshot.toObject(DashboardDataClass::class.java)
                    data?.let {
                        displayData(it)
                        getImageLink(documentSnapshot.id)
                    }
                } else {
                    Log.e(TAG, "Document does not exist.")
                    showSnackbar("Failed to fetch dashboard data. Please try again.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching data", e)
                showSnackbar("Failed to fetch dashboard data. Please try again.")
            }
    }

    /**
     * Retrieves the image link from Firestore based on the document ID.
     */
    private fun getImageLink(docId: String) {
        firestore.collection("images")
            .whereEqualTo("docId", docId)
            .whereEqualTo("latest", true)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val data = document.toObject(ImageDataClass::class.java)
                    data?.link?.let { displayImage(it) }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching image data", e)
                showSnackbar("Failed to load image. Please try again.")
            }
    }

    /**
     * Displays the dashboard data in the UI.
     */
    private fun displayData(data: DashboardDataClass) {
        binding.apply {
             diseaseStatus.text = data.DiseaseStatus
            solarRadiation.text = "${data.SolarRadiation} watt"
            amountofwater.text = "${data.AmountOfWater} ltr"
            temperature.text = "${data.Temperature}°C"
            humidity.text = "${data.Humidity}%"
            pestsStatus.text = data.PestsStatus
            precipitation.text = "${data.Precipitation} mm"
        }
    }

    /**
     * Loads and displays the plant image using Glide.
     */
    private fun displayImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .centerInside()
            .placeholder(R.drawable.plant)
            .error(R.drawable.plant)
            .into(binding.plantImage)
    }

    /**
     * Displays a Snackbar with the given message and an optional action.
     */
    private fun showSnackbar(message: String, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        action?.let {
            snackbar.setAction("Settings") { it() }
        }
        snackbar.show()
    }

    /**
     * Displays a short duration Toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
