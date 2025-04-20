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
import org.osmdroid.views.overlay.Marker

// Defaule position, for example, New York City
private const val DEFAULT_LATITUDE_ON_MAP_VIEW = 40.7128
private const val DEFAULT_LONGITUDE_ON_MAP_VIEW = -74.0060
private const val DEFAULT_MAT_VIEW_ZOOM_LEVEL = 17.0

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

        // Default position
        setPositionMarker(DEFAULT_LATITUDE_ON_MAP_VIEW, DEFAULT_LONGITUDE_ON_MAP_VIEW,
            DEFAULT_MAT_VIEW_ZOOM_LEVEL
        )

        // Set current position
        binding.buttonCurrentPosition.setOnClickListener {
            LocationTrackingService.GlobalVariables.latitude?.let { it1 -> LocationTrackingService.GlobalVariables.longitude?.let { it2 ->
                setPositionMarker(it1,
                    it2, DEFAULT_MAT_VIEW_ZOOM_LEVEL)
            } }
        }

        binding.buttonNextFragment.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Set the map's center to a specific coordinate
    private fun setPositionMarker(latitude: Double, longitude: Double, zoomLevel: Double) {
        val geoPoint = GeoPoint(latitude, longitude)
        val marker = Marker(map)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "You are here"
        map.overlays.add(marker)

        val mapController = map.controller as MapController
        mapController.setZoom(zoomLevel.toInt())
        mapController.setCenter(geoPoint)

        map.invalidate() // Refresh the map
    }
}