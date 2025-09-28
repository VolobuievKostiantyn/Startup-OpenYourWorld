package com.example.openyourworld

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

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
//        binding.buttonCurrentPosition.setOnClickListener {
//            LocationTrackingService.GlobalVariables.latitude?.let { it1 -> LocationTrackingService.GlobalVariables.longitude?.let { it2 ->
//                setPositionMarker(it1,
//                    it2, DEFAULT_MAT_VIEW_ZOOM_LEVEL)
//            } }
            binding.buttonCurrentPosition.setOnClickListener {
                59.437096.let { it1 -> 24.745296.let { it2 ->
                    setPositionMarker(it1,
                        it2, DEFAULT_MAT_VIEW_ZOOM_LEVEL)
                } }

            // Mark area for two positions from the DB on map
            val radiusInMeters = 5.0
            val latitude = LocationTrackingService.GlobalVariables.latitude
            val longitude = LocationTrackingService.GlobalVariables.longitude
            Log.d(TAG, "latitude = " + latitude)
            Log.d(TAG, "longitude = " + longitude)

//            val point1 = GeoPoint(latitude!!, longitude!!)
//            drawTransparentGreenCircle(map, point1, radiusInMeters)

            drawTestRoute(radiusInMeters)
            // Todo: !!!!! hide all area and then unhide the way
        }

        binding.buttonNextFragment.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    private fun drawTestRoute(radiusInMeters: Double) {
        val coordinates = arrayOf(
            Pair(59.437096, 24.745296),
            Pair(59.437107, 24.745315),
            Pair(59.437118, 24.745334),
            Pair(59.437129, 24.745353),
            Pair(59.437140, 24.745372),
            Pair(59.437151, 24.745391),
            Pair(59.437162, 24.745410),
            Pair(59.437173, 24.745429),
            Pair(59.437184, 24.745448),
            Pair(59.437195, 24.745467),
            Pair(59.437206, 24.745486),
            Pair(59.437218, 24.745714),
            Pair(59.437230, 24.745700),
            Pair(59.437250, 24.745650),
            Pair(59.437280, 24.745580),
            Pair(59.437310, 24.745510),
            Pair(59.437340, 24.745440),
            Pair(59.437370, 24.745370),
            Pair(59.437400, 24.745300),
            Pair(59.437430, 24.745230),
            Pair(59.437460, 24.745160),
            Pair(59.437490, 24.745090),
            Pair(59.437520, 24.745020),
            Pair(59.437550, 24.744950),
            Pair(59.437580, 24.744880),
            Pair(59.437611, 24.744910),
            Pair(59.437641, 24.744900),
            Pair(59.437620, 24.744880),
            Pair(59.437590, 24.744850),
            Pair(59.437560, 24.744820),
            Pair(59.437530, 24.744790),
            Pair(59.437500, 24.744760),
            Pair(59.437470, 24.744730),
            Pair(59.437440, 24.744700),
            Pair(59.437410, 24.744670),
            Pair(59.437380, 24.744640),
            Pair(59.437350, 24.744610),
            Pair(59.437320, 24.744580),
            Pair(59.437290, 24.744550),
            Pair(59.437285, 24.744440),
            Pair(59.437270, 24.744460),
            Pair(59.437250, 24.744490),
            Pair(59.437230, 24.744520),
            Pair(59.437210, 24.744550),
            Pair(59.437190, 24.744580),
            Pair(59.437170, 24.744610),
            Pair(59.437150, 24.744640),
            Pair(59.437130, 24.744670),
            Pair(59.437110, 24.744700),
            Pair(59.437105, 24.744713)
        )

        // 2. Loop through all coordinates
        for ((lat, lon) in coordinates) {
            val point = GeoPoint(lat, lon)
            // Example: draw green dot for each coordinate
            drawTransparentGreenCircle(map, point, radiusInMeters)
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

    private fun drawTransparentGreenCircle(map: MapView, center: GeoPoint, radiusInMeters: Double) {
        val circle = Polygon()
        circle.points = Polygon.pointsAsCircle(center, radiusInMeters)

        // Set semi-transparent green fill
        circle.fillPaint.color = Color.argb(64, 0, 255, 0)
        circle.fillPaint.style = Paint.Style.FILL

        // Remove outline
        circle.strokeColor = Color.TRANSPARENT
        circle.strokeWidth = 0f

        map.overlays.add(circle)
        map.invalidate()
    }
}