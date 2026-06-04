package com.arriva.touristguideapp

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.*

class RouteActivity : BaseActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var currentRouteOverlay: Polyline? = null
    
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var destName: String? = null

    private lateinit var pbLoading: View
    private lateinit var infoContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_route)

        destLat = intent.getDoubleExtra("destLat", 0.0)
        destLng = intent.getDoubleExtra("destLng", 0.0)
        destName = intent.getStringExtra("destName")

        pbLoading = findViewById(R.id.pbRouteLoading)
        infoContainer = findViewById(R.id.routeInfoContainer)

        findViewById<android.widget.TextView>(R.id.tvDestName).text = destName ?: "Route"
        findViewById<View>(R.id.btnRouteBack).setOnClickListener { finish() }

        map = findViewById(R.id.routeMap)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        initLocation()
        
        findViewById<View>(R.id.btnRecenter).setOnClickListener {
            locationOverlay.myLocation?.let { map.controller.animateTo(it) }
        }

        findViewById<View>(R.id.btnStartNav).setOnClickListener {
            Toast.makeText(this, "Safe travels to $destName!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initLocation() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        
        pbLoading.visibility = View.VISIBLE
        infoContainer.visibility = View.INVISIBLE

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                val myLocation = locationOverlay.myLocation
                if (myLocation != null) {
                    calculateRoute(myLocation, GeoPoint(destLat, destLng))
                    updateCurrentLocationText(myLocation)
                } else {
                    pbLoading.visibility = View.GONE
                    infoContainer.visibility = View.VISIBLE
                    Toast.makeText(this, "Could not determine your location", Toast.LENGTH_LONG).show()
                }
            }
        }
        map.overlays.add(locationOverlay)

        // Add destination marker
        val destMarker = Marker(map)
        destMarker.position = GeoPoint(destLat, destLng)
        destMarker.title = destName
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        destMarker.icon = ContextCompat.getDrawable(this, R.drawable.ic_map_marker_selected)
        map.overlays.add(destMarker)
    }

    private fun calculateRoute(start: GeoPoint, end: GeoPoint) {
        Thread {
            try {
                val roadManager: RoadManager = OSRMRoadManager(this, packageName)
                (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)

                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(start)
                waypoints.add(end)

                val road = roadManager.getRoad(waypoints)

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    pbLoading.visibility = View.GONE
                    infoContainer.visibility = View.VISIBLE

                    if (road.mStatus != Road.STATUS_OK) {
                        Toast.makeText(this, "Error calculating route", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    if (currentRouteOverlay != null) {
                        map.overlays.remove(currentRouteOverlay)
                    }

                    currentRouteOverlay = RoadManager.buildRoadOverlay(road)
                    currentRouteOverlay?.outlinePaint?.color = ContextCompat.getColor(this, R.color.color_primary)
                    currentRouteOverlay?.outlinePaint?.strokeWidth = 14f
                    currentRouteOverlay?.outlinePaint?.strokeCap = android.graphics.Paint.Cap.ROUND
                    
                    map.overlays.add(currentRouteOverlay)
                    
                    findViewById<android.widget.TextView>(R.id.tvRouteDist).text = String.format(Locale.getDefault(), "%.1f km", road.mLength)
                    val mins = (road.mDuration / 60).toInt()
                    findViewById<android.widget.TextView>(R.id.tvRouteTime).text = String.format(Locale.getDefault(), "%d min", mins)
                    
                    val bb = BoundingBox.fromGeoPoints(road.mRouteHigh)
                    map.zoomToBoundingBox(bb, true, 200)
                    
                    map.invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    pbLoading.visibility = View.GONE
                    infoContainer.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun updateCurrentLocationText(point: GeoPoint) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                findViewById<android.widget.TextView>(R.id.tvCurrentLoc).text = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
