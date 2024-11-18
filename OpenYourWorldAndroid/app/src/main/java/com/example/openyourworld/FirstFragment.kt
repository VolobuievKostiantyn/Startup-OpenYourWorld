package com.example.openyourworld

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.openyourworld.databinding.FragmentFirstBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private var TAG: String = javaClass.simpleName
    private var _binding: FragmentFirstBinding? = null
    private lateinit var map: MapView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(TAG, "FirstFragment onViewCreated")

        // Load/initialize the osmdroid configuration
        Configuration.getInstance().load(requireContext().applicationContext, PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext))

        // Inflate and create the map
        map = view.findViewById(R.id.osmmap)
        map.setTileSource(TileSourceFactory.MAPNIK)

        // Find the ComposeView and set the content
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            // Get location updates here
            Log.d(TAG, "Getting location updates started")
            LocationTrackingService.scheduleWork(requireContext().applicationContext)
            Log.d(TAG, "Getting location updates ended")
        }

        //Todo: add current position button and set the map each time it is pressed
        setMapCenter(40.7128, -74.0060, 10.0) // For example, New York City with zoom level 10


        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Set the map's center to a specific coordinate
    private fun setMapCenter(latitude: Double, longitude: Double, zoomLevel: Double) {
        val mapController = map.controller as MapController
        mapController.setZoom(zoomLevel.toInt())  // Set initial zoom level
        mapController.setCenter(GeoPoint(latitude, longitude))
    }
}