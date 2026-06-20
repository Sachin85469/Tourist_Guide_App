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
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Switch
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.concurrent.fixedRateTimer

class RouteActivity : BaseActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var currentRouteOverlay: Polyline? = null
    
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var destName: String? = null

    private lateinit var pbLoading: View
    private lateinit var infoContainer: View
    private lateinit var btnModeBike: com.google.android.material.button.MaterialButton
    private lateinit var btnModeCar: com.google.android.material.button.MaterialButton
    private lateinit var btnModeWalk: com.google.android.material.button.MaterialButton
    private lateinit var tvEtaBike: android.widget.TextView
    private lateinit var tvEtaCar: android.widget.TextView
    private lateinit var tvEtaWalk: android.widget.TextView
    private lateinit var tvEtaSelected: android.widget.TextView
    private lateinit var btnStartNav: MaterialButton
    private lateinit var switchTts: SwitchMaterial
    private var tts: TextToSpeech? = null
    private var isNavigating = false
    private var navTimer: Timer? = null

    private enum class TravelMode { BIKE, CAR, WALK }
    private var selectedMode = TravelMode.CAR

    // Speeds in km/h
    private val SPEED_BIKE = 35.0
    private val SPEED_CAR = 45.0
    private val SPEED_WALK = 5.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_route)

        destLat = intent.getDoubleExtra("destLat", 0.0)
        destLng = intent.getDoubleExtra("destLng", 0.0)
        destName = intent.getStringExtra("destName")

        pbLoading = findViewById(R.id.pbRouteLoading)
        infoContainer = findViewById(R.id.routeInfoContainer)

        btnModeBike = findViewById(R.id.btnModeBike)
        btnModeCar = findViewById(R.id.btnModeCar)
        btnModeWalk = findViewById(R.id.btnModeWalk)
        tvEtaBike = findViewById(R.id.tvEtaBike)
        tvEtaCar = findViewById(R.id.tvEtaCar)
        tvEtaWalk = findViewById(R.id.tvEtaWalk)
        tvEtaSelected = findViewById(R.id.tvEtaSelected)
        btnStartNav = findViewById(R.id.btnStartNav)
        switchTts = findViewById(R.id.switchTts)

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

        btnModeBike.setOnClickListener { selectMode(TravelMode.BIKE) }
        btnModeCar.setOnClickListener { selectMode(TravelMode.CAR) }
        btnModeWalk.setOnClickListener { selectMode(TravelMode.WALK) }

        btnStartNav.setOnClickListener {
            startNavigation()
        }

        switchTts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) initTts()
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
                    
                    val distanceKm = road.mLength
                    val distanceText = String.format(Locale.getDefault(), "%.1f km", distanceKm)
                    findViewById<android.widget.TextView>(R.id.tvRouteDist).text = distanceText
                    findViewById<android.widget.TextView>(R.id.tvRouteDistSelected).text = distanceText

                    // ETA calculations based on average speeds (independent of road manager)
                    val etaBike = calculateEtaMinutes(distanceKm, SPEED_BIKE)
                    val etaCar = calculateEtaMinutes(distanceKm, SPEED_CAR)
                    val etaWalk = calculateEtaMinutes(distanceKm, SPEED_WALK)

                    tvEtaBike.text = String.format(Locale.getDefault(), "Bike: %d min", etaBike)
                    tvEtaCar.text = String.format(Locale.getDefault(), "Car: %d min", etaCar)
                    tvEtaWalk.text = String.format(Locale.getDefault(), "Walk: %d min", etaWalk)

                    // Show selected ETA
                    tvEtaSelected.text = String.format(Locale.getDefault(), "%d min", when (selectedMode) {
                        TravelMode.BIKE -> etaBike
                        TravelMode.CAR -> etaCar
                        TravelMode.WALK -> etaWalk
                    })
                    
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

    private fun calculateEtaMinutes(distanceKm: Double, speedKmh: Double): Int {
        if (speedKmh <= 0.0) return 0
        val hours = distanceKm / speedKmh
        return Math.max(1, Math.round(hours * 60).toInt())
    }

    private fun selectMode(mode: TravelMode) {
        selectedMode = mode
        // update styles
        fun reset(btn: com.google.android.material.button.MaterialButton) {
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            btn.setTextColor(ContextCompat.getColor(this, R.color.color_primary))
            btn.iconTint = null
        }

        reset(btnModeBike); reset(btnModeCar); reset(btnModeWalk)

        val selectedBtn = when (mode) {
            TravelMode.BIKE -> btnModeBike
            TravelMode.CAR -> btnModeCar
            TravelMode.WALK -> btnModeWalk
        }

        selectedBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary))
        selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
        // update displayed ETA
        val bike = tvEtaBike.text.toString().substringAfterLast(" ").replace("min","").trim().toIntOrNull() ?: 0
        val car = tvEtaCar.text.toString().substringAfterLast(" ").replace("min","").trim().toIntOrNull() ?: 0
        val walk = tvEtaWalk.text.toString().substringAfterLast(" ").replace("min","").trim().toIntOrNull() ?: 0
        val selectedMinutes = when (mode) {
            TravelMode.BIKE -> bike
            TravelMode.CAR -> car
            TravelMode.WALK -> walk
        }
        tvEtaSelected.text = String.format(Locale.getDefault(), "%d min", selectedMinutes)
    }

    private fun startNavigation() {
        if (destLat == 0.0 && destLng == 0.0) return
        // Try Google Maps external intent
        try {
            packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            val uri = Uri.parse("geo:$destLat,$destLng?q=$destLat,$destLng(${Uri.encode(destName ?: "Destination")})")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to in-app navigation
            Toast.makeText(this, "Starting in-app navigation", Toast.LENGTH_SHORT).show()
            isNavigating = true
            // start periodic updates to recalc route/eta
            navTimer?.cancel()
            navTimer = fixedRateTimer("nav_timer", true, 0L, 5000L) {
                runOnUiThread {
                    val myLoc = locationOverlay.myLocation
                    if (myLoc != null) {
                        calculateRoute(GeoPoint(myLoc.latitude, myLoc.longitude), GeoPoint(destLat, destLng))
                        // speak progress if tts enabled
                        if (switchTts.isChecked) {
                            speak("Continuing navigation. ${tvEtaSelected.text}")
                        }
                        // check arrival
                        val remaining = haversine(myLoc.latitude, myLoc.longitude, destLat, destLng)
                        if (remaining < 0.05) {
                            speak("You have arrived at your destination")
                            Toast.makeText(this@RouteActivity, "Arrived", Toast.LENGTH_SHORT).show()
                            isNavigating = false
                            navTimer?.cancel()
                        }
                    }
                }
            }
        }
    }

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    private fun speak(text: String) {
        try {
            if (tts == null) initTts()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_tts")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        return R * c
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
