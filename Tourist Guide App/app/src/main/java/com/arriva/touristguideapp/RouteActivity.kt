package com.arriva.touristguideapp

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
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
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class RouteActivity : BaseActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 801
        private const val LOCATION_INTERVAL_MS = 1_200L
        private const val LOCATION_FASTEST_INTERVAL_MS = 750L
        private const val LOCATION_MIN_DISTANCE_METERS = 2f
        private const val GPS_LOSS_TIMEOUT_MS = 18_000L
        private const val GEOCODE_INTERVAL_MS = 20_000L
        private const val ROUTE_RECALCULATION_COOLDOWN_MS = 12_000L
        private const val ARRIVAL_DISTANCE_METERS = 35.0
        private const val DEFAULT_OFF_ROUTE_DISTANCE_METERS = 65.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentRouteOverlay: Polyline? = null
    private var navigationUserMarker: Marker? = null
    private var userMarkerAnimator: ValueAnimator? = null
    private var cameraAnimator: ValueAnimator? = null
    private var routeAnimator: ValueAnimator? = null

    private var destLat = 0.0
    private var destLng = 0.0
    private var destName: String? = null
    private var autoStartNavigation = false

    private lateinit var pbLoading: View
    private lateinit var infoContainer: View
    private lateinit var btnModeBike: MaterialButton
    private lateinit var btnModeCar: MaterialButton
    private lateinit var btnModeWalk: MaterialButton
    private lateinit var tvEtaBike: TextView
    private lateinit var tvEtaCar: TextView
    private lateinit var tvEtaWalk: TextView
    private lateinit var tvEtaSelected: TextView
    private lateinit var tvRouteDistance: TextView
    private lateinit var tvRouteDistanceSelected: TextView
    private lateinit var tvRouteTime: TextView
    private lateinit var tvCurrentLocation: TextView
    private lateinit var btnStartNav: MaterialButton
    private lateinit var switchTts: SwitchMaterial
    private lateinit var navigationGuidanceCard: View
    private lateinit var nightMapTint: View
    private lateinit var btnNightMode: ImageView
    private lateinit var ivManeuverArrow: ImageView
    private lateinit var tvManeuverDistance: TextView
    private lateinit var tvNextManeuver: TextView
    private lateinit var tvNavigationSpeed: TextView

    private var tts: TextToSpeech? = null
    private var isNavigating = false
    private var navigationStartPending = false
    private var locationUpdatesRequested = false
    private var routeRequestInFlight = false
    private var navigationSession = 0
    private var lastRouteRecalculationAt = 0L
    private var lastLocationUpdateAt = 0L
    private var lastNavigationLocation: Location? = null
    private var markerPosition: GeoPoint? = null
    private var activeRoutePoints: List<GeoPoint> = emptyList()
    private var lastRemainingMeters = 0.0
    private var smoothedSpeedMetersPerSecond = 0.0
    private var gpsLossShown = false
    private var networkLossShown = false
    private var lastGeocodeAt = 0L
    private var geocodeInFlight = false
    private var isNightNavigationMode = false
    private var lastSpokenManeuverKey: String? = null
    private var lastSpokenManeuverAt = 0L

    private enum class TravelMode { BIKE, CAR, WALK }
    private var selectedMode = TravelMode.CAR

    private val navigationHandler = Handler(Looper.getMainLooper())
    private val gpsWatchdog = object : Runnable {
        override fun run() {
            if (!isNavigating) return
            if (System.currentTimeMillis() - lastLocationUpdateAt > GPS_LOSS_TIMEOUT_MS && !gpsLossShown) {
                gpsLossShown = true
                tvCurrentLocation.text = "GPS signal lost. Waiting for a location fix…"
                Toast.makeText(this@RouteActivity, "GPS signal lost. Navigation will resume automatically.", Toast.LENGTH_LONG).show()
            }
            navigationHandler.postDelayed(this, GPS_LOSS_TIMEOUT_MS / 3)
        }
    }

    private val navigationLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onNavigationLocation)
        }

        override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
            if (isNavigating && !availability.isLocationAvailable && !gpsLossShown) {
                gpsLossShown = true
                tvCurrentLocation.text = "GPS signal unavailable. Waiting to reconnect…"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_route)

        destLat = intent.getDoubleExtra("destLat", 0.0)
        destLng = intent.getDoubleExtra("destLng", 0.0)
        destName = intent.getStringExtra("destName")
        autoStartNavigation = intent.getBooleanExtra("startNavigation", false)

        bindViews()
        findViewById<TextView>(R.id.tvDestName).text = destName ?: "Route"
        findViewById<View>(R.id.btnRouteBack).setOnClickListener { finish() }

        map = findViewById(R.id.routeMap)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        setupLocationOverlay()
        addDestinationMarker()

        findViewById<View>(R.id.btnRecenter).setOnClickListener {
            if (isNavigating) {
                lastNavigationLocation?.let { centerMapForNavigation(it, resolveHeading(it)) }
            } else {
                locationOverlay.myLocation?.let { map.controller.animateTo(it) }
            }
        }

        btnModeBike.setOnClickListener { selectMode(TravelMode.BIKE) }
        btnModeCar.setOnClickListener { selectMode(TravelMode.CAR) }
        btnModeWalk.setOnClickListener { selectMode(TravelMode.WALK) }
        btnStartNav.setOnClickListener {
            if (isNavigating) stopNavigation(showMessage = true) else requestNavigationStart()
        }
        switchTts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                initTts()
                if (isNavigating) speak("Voice guidance enabled. ${tvNextManeuver.text}")
            }
        }
        btnNightMode.setOnClickListener { applyNightNavigationMode(!isNightNavigationMode) }

        if (autoStartNavigation) {
            requestNavigationStart()
        } else if (hasLocationPermission()) {
            requestPreviewRoute()
        } else {
            pbLoading.visibility = View.GONE
            infoContainer.visibility = View.VISIBLE
        }
    }

    private fun bindViews() {
        pbLoading = findViewById(R.id.pbRouteLoading)
        infoContainer = findViewById(R.id.routeInfoContainer)
        btnModeBike = findViewById(R.id.btnModeBike)
        btnModeCar = findViewById(R.id.btnModeCar)
        btnModeWalk = findViewById(R.id.btnModeWalk)
        tvEtaBike = findViewById(R.id.tvEtaBike)
        tvEtaCar = findViewById(R.id.tvEtaCar)
        tvEtaWalk = findViewById(R.id.tvEtaWalk)
        tvEtaSelected = findViewById(R.id.tvEtaSelected)
        tvRouteDistance = findViewById(R.id.tvRouteDist)
        tvRouteDistanceSelected = findViewById(R.id.tvRouteDistSelected)
        tvRouteTime = findViewById(R.id.tvRouteTime)
        tvCurrentLocation = findViewById(R.id.tvCurrentLoc)
        btnStartNav = findViewById(R.id.btnStartNav)
        switchTts = findViewById(R.id.switchTts)
        navigationGuidanceCard = findViewById(R.id.navigationGuidanceCard)
        nightMapTint = findViewById(R.id.nightMapTint)
        btnNightMode = findViewById(R.id.btnNightMode)
        ivManeuverArrow = findViewById(R.id.ivManeuverArrow)
        tvManeuverDistance = findViewById(R.id.tvManeuverDistance)
        tvNextManeuver = findViewById(R.id.tvNextManeuver)
        tvNavigationSpeed = findViewById(R.id.tvNavigationSpeed)
    }

    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        map.overlays.add(locationOverlay)
        if (hasLocationPermission()) {
            locationOverlay.enableMyLocation()
        }
    }

    private fun addDestinationMarker() {
        if (!hasValidDestination()) return
        val destinationMarker = Marker(map)
        destinationMarker.position = GeoPoint(destLat, destLng)
        destinationMarker.title = destName ?: "Destination"
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        destinationMarker.icon = ContextCompat.getDrawable(this, R.drawable.ic_map_marker_selected)
        map.overlays.add(destinationMarker)
    }

    private fun requestPreviewRoute() {
        if (!hasValidDestination()) return
        @Suppress("MissingPermission")
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                calculateRoute(GeoPoint(location.latitude, location.longitude), fromNavigation = false)
            } else {
                locationOverlay.enableMyLocation()
                locationOverlay.runOnFirstFix {
                    locationOverlay.myLocation?.let { point ->
                        runOnUiThread { calculateRoute(point, fromNavigation = false) }
                    }
                }
            }
        }
    }

    private fun requestNavigationStart() {
        if (!hasValidDestination()) {
            Toast.makeText(this, "Destination is unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasLocationPermission()) {
            navigationStartPending = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        startNavigationSession()
    }

    @Suppress("MissingPermission")
    private fun startNavigationSession() {
        if (isNavigating) return
        isNavigating = true
        navigationSession++
        gpsLossShown = false
        networkLossShown = false
        lastSpokenManeuverKey = null
        lastLocationUpdateAt = System.currentTimeMillis()
        btnStartNav.text = "End Navigation"
        currentRouteOverlay?.outlinePaint?.strokeWidth = 18f
        navigationGuidanceCard.visibility = View.VISIBLE
        tvManeuverDistance.text = "Preparing route"
        tvNextManeuver.text = "Finding the best way"
        tvNavigationSpeed.text = "0 km/h"
        locationOverlay.disableMyLocation()
        requestNavigationUpdates()
        navigationHandler.removeCallbacks(gpsWatchdog)
        navigationHandler.postDelayed(gpsWatchdog, GPS_LOSS_TIMEOUT_MS / 3)
        Toast.makeText(this, "Navigation started", Toast.LENGTH_SHORT).show()
        speak("Navigation started. Follow the highlighted route.")

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (isNavigating && location != null) onNavigationLocation(location)
        }
    }

    @Suppress("MissingPermission")
    private fun requestNavigationUpdates() {
        if (!isNavigating || locationUpdatesRequested) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(LOCATION_MIN_DISTANCE_METERS)
            .setWaitForAccurateLocation(false)
            .build()
        fusedLocationClient.requestLocationUpdates(request, navigationLocationCallback, Looper.getMainLooper())
        locationUpdatesRequested = true
    }

    private fun stopLocationUpdates() {
        if (!locationUpdatesRequested) return
        fusedLocationClient.removeLocationUpdates(navigationLocationCallback)
        locationUpdatesRequested = false
    }

    private fun onNavigationLocation(location: Location) {
        if (!isNavigating || location.accuracy > 120f) return
        val now = System.currentTimeMillis()
        lastLocationUpdateAt = now
        gpsLossShown = false

        val destinationDistance = distanceMeters(location.latitude, location.longitude, destLat, destLng)
        if (destinationDistance <= ARRIVAL_DISTANCE_METERS) {
            completeNavigation()
            return
        }

        val heading = resolveHeading(location)
        updateNavigationMarker(location, heading)
        centerMapForNavigation(location, heading)
        updateSpeedDisplay(location)
        updateCurrentLocationLabel(location)

        if (activeRoutePoints.isEmpty()) {
            calculateRoute(GeoPoint(location.latitude, location.longitude), fromNavigation = true)
            return
        }

        val progress = calculateRouteProgress(GeoPoint(location.latitude, location.longitude), activeRoutePoints)
        lastRemainingMeters = progress.remainingMeters
        updateNavigationStats(progress.remainingMeters, location)
        updateManeuverGuidance(progress)

        val offRouteThreshold = max(DEFAULT_OFF_ROUTE_DISTANCE_METERS, location.accuracy.toDouble() * 2.5)
        if (progress.distanceFromRouteMeters > offRouteThreshold
            && now - lastRouteRecalculationAt >= ROUTE_RECALCULATION_COOLDOWN_MS
        ) {
            lastRouteRecalculationAt = now
            if (hasUsableNetwork()) {
                Toast.makeText(this, "You are off route. Finding a new route…", Toast.LENGTH_SHORT).show()
                speak("You are off route. Finding a new route.")
                calculateRoute(GeoPoint(location.latitude, location.longitude), fromNavigation = true)
            } else if (!networkLossShown) {
                networkLossShown = true
                Toast.makeText(this, "Network unavailable. Continuing with the current route.", Toast.LENGTH_LONG).show()
            }
        }

        if (progress.remainingMeters <= ARRIVAL_DISTANCE_METERS) completeNavigation()
    }

    private fun calculateRoute(start: GeoPoint, fromNavigation: Boolean) {
        if (routeRequestInFlight || !hasValidDestination()) return
        if (fromNavigation && !hasUsableNetwork()) {
            if (!networkLossShown) {
                networkLossShown = true
                Toast.makeText(this, "Network unavailable. Route will update when connected.", Toast.LENGTH_LONG).show()
            }
            return
        }

        routeRequestInFlight = true
        val sessionAtRequest = navigationSession
        if (!fromNavigation) {
            pbLoading.visibility = View.VISIBLE
            infoContainer.visibility = View.INVISIBLE
        }

        Thread {
            try {
                val roadManager: RoadManager = OSRMRoadManager(this, packageName)
                (roadManager as OSRMRoadManager).setMean(
                    when (selectedMode) {
                        TravelMode.WALK -> OSRMRoadManager.MEAN_BY_FOOT
                        else -> OSRMRoadManager.MEAN_BY_CAR
                    }
                )
                val road = roadManager.getRoad(arrayListOf(start, GeoPoint(destLat, destLng)))
                runOnUiThread {
                    routeRequestInFlight = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (fromNavigation && (!isNavigating || sessionAtRequest != navigationSession)) return@runOnUiThread
                    pbLoading.visibility = View.GONE
                    infoContainer.visibility = View.VISIBLE

                    if (road.mStatus != Road.STATUS_OK || road.mRouteHigh.isNullOrEmpty()) {
                        if (!fromNavigation) Toast.makeText(this, "Unable to calculate route", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    activeRoutePoints = road.mRouteHigh.toList()
                    drawActiveRoute(road)
                    val routeMeters = routeLengthMeters(activeRoutePoints)
                    lastRemainingMeters = if (routeMeters > 0.0) routeMeters else road.mLength * 1000.0
                    if (isNavigating && lastNavigationLocation != null) {
                        onNavigationLocation(lastNavigationLocation!!)
                    } else {
                        updatePreviewStats(lastRemainingMeters)
                        BoundingBox.fromGeoPoints(activeRoutePoints).let { map.zoomToBoundingBox(it, true, 200) }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    routeRequestInFlight = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    pbLoading.visibility = View.GONE
                    infoContainer.visibility = View.VISIBLE
                    if (!fromNavigation) Toast.makeText(this, "Route failed. Check your connection and try again.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun drawActiveRoute(road: Road) {
        currentRouteOverlay?.let { map.overlays.remove(it) }
        currentRouteOverlay = RoadManager.buildRoadOverlay(road).apply {
            outlinePaint.color = ContextCompat.getColor(this@RouteActivity, R.color.color_primary)
            outlinePaint.strokeWidth = 4f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        }
        map.overlays.add(currentRouteOverlay)
        routeAnimator?.cancel()
        routeAnimator = ValueAnimator.ofFloat(4f, if (isNavigating) 18f else 14f).apply {
            duration = 480L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentRouteOverlay?.outlinePaint?.strokeWidth = animation.animatedValue as Float
                map.invalidate()
            }
            start()
        }
        map.invalidate()
    }

    private fun updateNavigationMarker(location: Location, heading: Float) {
        val target = GeoPoint(location.latitude, location.longitude)
        val marker = navigationUserMarker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.icon = ContextCompat.getDrawable(this, R.drawable.ic_navigation_arrow)
            it.position = target
            map.overlays.add(it)
            navigationUserMarker = it
        }
        marker.rotation = heading

        val from = markerPosition ?: target
        userMarkerAnimator?.cancel()
        userMarkerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (markerPosition == null) 0L else 450L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction.toDouble()
                marker.position = GeoPoint(
                    from.latitude + (target.latitude - from.latitude) * fraction,
                    from.longitude + (target.longitude - from.longitude) * fraction
                )
                map.invalidate()
            }
            start()
        }
        markerPosition = target
        lastNavigationLocation = location
    }

    private fun centerMapForNavigation(location: Location, heading: Float) {
        val currentCenter = map.mapCenter
        val startLatitude = currentCenter.latitude
        val startLongitude = currentCenter.longitude
        val startOrientation = map.mapOrientation
        val targetLatitude = location.latitude
        val targetLongitude = location.longitude
        val targetOrientation = -heading

        cameraAnimator?.cancel()
        cameraAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 620L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction.toDouble()
                map.controller.setCenter(GeoPoint(
                    startLatitude + (targetLatitude - startLatitude) * fraction,
                    startLongitude + (targetLongitude - startLongitude) * fraction
                ))
                map.mapOrientation = interpolateAngle(startOrientation, targetOrientation, fraction.toFloat())
                map.invalidate()
            }
            start()
        }
        map.controller.setZoom(18.0)
    }

    private fun resolveHeading(location: Location): Float {
        if (location.hasBearing()) return location.bearing
        val previous = lastNavigationLocation ?: return 0f
        val results = FloatArray(2)
        Location.distanceBetween(
            previous.latitude, previous.longitude,
            location.latitude, location.longitude,
            results
        )
        return if (results[0] >= 2f) results[1] else 0f
    }

    private fun updateNavigationStats(remainingMeters: Double, location: Location) {
        val speed = effectiveSpeed(location)
        val etaMinutes = max(1, ceil(remainingMeters / speed / 60.0).toInt())
        updateRouteTexts(remainingMeters, etaMinutes)
    }

    private fun updateSpeedDisplay(location: Location) {
        val speedKmh = if (location.hasSpeed()) max(0.0, location.speed.toDouble() * 3.6) else 0.0
        tvNavigationSpeed.text = if (speedKmh < 1.0) "0 km/h" else String.format(Locale.getDefault(), "%.0f km/h", speedKmh)
    }

    private fun updateManeuverGuidance(progress: RouteProgress) {
        val maneuver = nextManeuver(progress.nearestSegmentIndex, progress.remainingMeters)
        tvManeuverDistance.text = formatDistance(maneuver.distanceMeters)
        tvNextManeuver.text = maneuver.instruction
        ivManeuverArrow.rotation = maneuver.arrowRotation

        val now = System.currentTimeMillis()
        if (maneuver.distanceMeters <= 180.0
            && maneuver.key != lastSpokenManeuverKey
            && now - lastSpokenManeuverAt > 8_000L
        ) {
            lastSpokenManeuverKey = maneuver.key
            lastSpokenManeuverAt = now
            speak("In ${formatDistanceForSpeech(maneuver.distanceMeters)}, ${maneuver.instruction.lowercase(Locale.getDefault())}")
        }
    }

    private fun nextManeuver(startSegment: Int, remainingMeters: Double): ManeuverInfo {
        if (activeRoutePoints.size < 3) {
            return ManeuverInfo(remainingMeters, "Continue to destination", 0f, "destination")
        }
        var distanceToCandidate = 0.0
        var previousBearing = bearingBetween(activeRoutePoints[startSegment], activeRoutePoints[startSegment + 1])
        for (index in startSegment + 1 until activeRoutePoints.lastIndex) {
            distanceToCandidate += distanceMeters(
                activeRoutePoints[index].latitude, activeRoutePoints[index].longitude,
                activeRoutePoints[index + 1].latitude, activeRoutePoints[index + 1].longitude
            )
            val nextBearing = bearingBetween(activeRoutePoints[index], activeRoutePoints[index + 1])
            val turn = normalizeAngle(nextBearing - previousBearing)
            if (kotlin.math.abs(turn) >= 35f && distanceToCandidate >= 12.0) {
                val instruction = when {
                    turn >= 135f || turn <= -135f -> "Make a U-turn"
                    turn >= 70f -> "Turn right"
                    turn >= 35f -> "Keep right"
                    turn <= -70f -> "Turn left"
                    else -> "Keep left"
                }
                val arrowRotation = when {
                    turn >= 135f || turn <= -135f -> 180f
                    turn > 0f -> 90f
                    turn < 0f -> -90f
                    else -> 0f
                }
                return ManeuverInfo(distanceToCandidate, instruction, arrowRotation, "turn_$index")
            }
            previousBearing = nextBearing
        }
        return ManeuverInfo(remainingMeters, "Continue to destination", 0f, "destination")
    }

    private fun formatDistance(distanceMeters: Double): String = if (distanceMeters >= 1_000.0) {
        String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1_000.0)
    } else {
        "${max(10, (distanceMeters / 10.0).toInt() * 10)} m"
    }

    private fun formatDistanceForSpeech(distanceMeters: Double): String = if (distanceMeters >= 1_000.0) {
        String.format(Locale.getDefault(), "%.1f kilometers", distanceMeters / 1_000.0)
    } else {
        "${max(10, (distanceMeters / 10.0).toInt() * 10)} meters"
    }

    private fun updatePreviewStats(remainingMeters: Double) {
        updateRouteTexts(remainingMeters, etaFor(remainingMeters, modeSpeedMetersPerSecond(selectedMode)))
    }

    private fun updateRouteTexts(remainingMeters: Double, selectedEtaMinutes: Int) {
        val distanceText = if (remainingMeters >= 1_000) {
            String.format(Locale.getDefault(), "%.1f km", remainingMeters / 1_000.0)
        } else {
            String.format(Locale.getDefault(), "%d m", remainingMeters.toInt())
        }
        tvRouteDistance.text = distanceText
        tvRouteDistanceSelected.text = distanceText
        tvEtaSelected.text = "$selectedEtaMinutes min"
        tvRouteTime.text = "$selectedEtaMinutes min"
        tvEtaBike.text = "Bike: ${etaFor(remainingMeters, modeSpeedMetersPerSecond(TravelMode.BIKE))} min"
        tvEtaCar.text = "Car: ${etaFor(remainingMeters, modeSpeedMetersPerSecond(TravelMode.CAR))} min"
        tvEtaWalk.text = "Walk: ${etaFor(remainingMeters, modeSpeedMetersPerSecond(TravelMode.WALK))} min"
    }

    private fun effectiveSpeed(location: Location): Double {
        val measured = if (location.hasSpeed() && location.speed >= 0.8f) location.speed.toDouble() else 0.0
        if (measured > 0.0) {
            smoothedSpeedMetersPerSecond = if (smoothedSpeedMetersPerSecond == 0.0) {
                measured
            } else {
                smoothedSpeedMetersPerSecond * 0.7 + measured * 0.3
            }
        }
        return if (smoothedSpeedMetersPerSecond >= 0.8) {
            smoothedSpeedMetersPerSecond
        } else {
            modeSpeedMetersPerSecond(selectedMode)
        }
    }

    private fun modeSpeedMetersPerSecond(mode: TravelMode): Double = when (mode) {
        TravelMode.BIKE -> 35.0 / 3.6
        TravelMode.CAR -> 45.0 / 3.6
        TravelMode.WALK -> 5.0 / 3.6
    }

    private fun etaFor(distanceMeters: Double, speedMetersPerSecond: Double): Int =
        max(1, ceil(distanceMeters / max(speedMetersPerSecond, 0.1) / 60.0).toInt())

    private fun calculateRouteProgress(point: GeoPoint, route: List<GeoPoint>): RouteProgress {
        if (route.size < 2) return RouteProgress(distanceMeters(point.latitude, point.longitude, destLat, destLng), Double.MAX_VALUE, 0)
        var nearestDistance = Double.MAX_VALUE
        var nearestSegment = 0
        var nearestPoint = route.first()

        for (index in 0 until route.lastIndex) {
            val projection = projectPointOntoSegment(point, route[index], route[index + 1])
            if (projection.distanceMeters < nearestDistance) {
                nearestDistance = projection.distanceMeters
                nearestSegment = index
                nearestPoint = projection.point
            }
        }

        var remaining = distanceMeters(nearestPoint.latitude, nearestPoint.longitude,
            route[nearestSegment + 1].latitude, route[nearestSegment + 1].longitude)
        for (index in nearestSegment + 1 until route.lastIndex) {
            remaining += distanceMeters(
                route[index].latitude, route[index].longitude,
                route[index + 1].latitude, route[index + 1].longitude
            )
        }
        return RouteProgress(remaining, nearestDistance, nearestSegment)
    }

    private fun projectPointOntoSegment(point: GeoPoint, start: GeoPoint, end: GeoPoint): SegmentProjection {
        val latitudeRadians = Math.toRadians(point.latitude)
        fun x(value: GeoPoint) = Math.toRadians(value.longitude - point.longitude) * EARTH_RADIUS_METERS * cos(latitudeRadians)
        fun y(value: GeoPoint) = Math.toRadians(value.latitude - point.latitude) * EARTH_RADIUS_METERS

        val ax = x(start)
        val ay = y(start)
        val bx = x(end)
        val by = y(end)
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val factor = if (lengthSquared == 0.0) 0.0 else min(1.0, max(0.0, -(ax * dx + ay * dy) / lengthSquared))
        val projectedX = ax + factor * dx
        val projectedY = ay + factor * dy
        val projected = GeoPoint(
            point.latitude + Math.toDegrees(projectedY / EARTH_RADIUS_METERS),
            point.longitude + Math.toDegrees(projectedX / (EARTH_RADIUS_METERS * cos(latitudeRadians)))
        )
        return SegmentProjection(projected, sqrt(projectedX * projectedX + projectedY * projectedY))
    }

    private fun routeLengthMeters(route: List<GeoPoint>): Double {
        var length = 0.0
        for (index in 0 until route.lastIndex) {
            length += distanceMeters(route[index].latitude, route[index].longitude, route[index + 1].latitude, route[index + 1].longitude)
        }
        return length
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun bearingBetween(start: GeoPoint, end: GeoPoint): Float {
        val startLatitude = Math.toRadians(start.latitude)
        val endLatitude = Math.toRadians(end.latitude)
        val longitudeDelta = Math.toRadians(end.longitude - start.longitude)
        val y = sin(longitudeDelta) * cos(endLatitude)
        val x = cos(startLatitude) * sin(endLatitude) - sin(startLatitude) * cos(endLatitude) * cos(longitudeDelta)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    private fun interpolateAngle(start: Float, end: Float, fraction: Float): Float =
        start + normalizeAngle(end - start) * fraction

    private fun applyNightNavigationMode(enable: Boolean) {
        isNightNavigationMode = enable
        nightMapTint.visibility = if (enable) View.VISIBLE else View.GONE
        btnNightMode.setImageResource(if (enable) R.drawable.ic_day_mode else R.drawable.ic_night_mode)
        btnNightMode.contentDescription = if (enable) "Enable day navigation mode" else "Enable night navigation mode"
        map.invalidate()
    }

    private fun updateCurrentLocationLabel(location: Location) {
        val now = System.currentTimeMillis()
        if (geocodeInFlight || now - lastGeocodeAt < GEOCODE_INTERVAL_MS) return
        geocodeInFlight = true
        lastGeocodeAt = now
        val point = GeoPoint(location.latitude, location.longitude)
        Thread {
            val label = try {
                val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(point.latitude, point.longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
            } catch (_: IOException) {
                null
            }
            runOnUiThread {
                geocodeInFlight = false
                if (isNavigating && !label.isNullOrBlank()) tvCurrentLocation.text = label
            }
        }.start()
    }

    private fun selectMode(mode: TravelMode) {
        selectedMode = mode
        fun reset(button: MaterialButton) {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            button.setTextColor(ContextCompat.getColor(this, R.color.color_primary))
            button.iconTint = null
        }
        reset(btnModeBike)
        reset(btnModeCar)
        reset(btnModeWalk)
        val selectedButton = when (mode) {
            TravelMode.BIKE -> btnModeBike
            TravelMode.CAR -> btnModeCar
            TravelMode.WALK -> btnModeWalk
        }
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary))
        selectedButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        if (lastRemainingMeters > 0.0) {
            if (isNavigating && lastNavigationLocation != null) updateNavigationStats(lastRemainingMeters, lastNavigationLocation!!)
            else updatePreviewStats(lastRemainingMeters)
        }
    }

    private fun completeNavigation() {
        speak("You have arrived at your destination")
        stopNavigation(showMessage = false)
        Toast.makeText(this, "You have arrived", Toast.LENGTH_LONG).show()
    }

    private fun stopNavigation(showMessage: Boolean) {
        if (!isNavigating) return
        isNavigating = false
        navigationSession++
        stopLocationUpdates()
        navigationHandler.removeCallbacks(gpsWatchdog)
        userMarkerAnimator?.cancel()
        navigationUserMarker?.let { map.overlays.remove(it) }
        navigationUserMarker = null
        markerPosition = null
        navigationGuidanceCard.visibility = View.GONE
        map.mapOrientation = 0f
        btnStartNav.text = "Start Navigation"
        if (hasLocationPermission()) locationOverlay.enableMyLocation()
        map.invalidate()
        if (showMessage) Toast.makeText(this, "Navigation ended", Toast.LENGTH_SHORT).show()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasValidDestination(): Boolean = destLat != 0.0 || destLng != 0.0

    private fun hasUsableNetwork(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
    }

    private fun speak(text: String) {
        if (!switchTts.isChecked) return
        if (tts == null) initTts()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_tts")
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (isNavigating && hasLocationPermission()) requestNavigationUpdates()
        else if (hasLocationPermission()) locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        if (isNavigating) stopLocationUpdates()
        map.onPause()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        navigationHandler.removeCallbacksAndMessages(null)
        userMarkerAnimator?.cancel()
        cameraAnimator?.cancel()
        routeAnimator?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST) return
        if (!hasLocationPermission()) {
            navigationStartPending = false
            Toast.makeText(this, "Location permission is required to start navigation", Toast.LENGTH_LONG).show()
            return
        }
        locationOverlay.enableMyLocation()
        if (navigationStartPending) {
            navigationStartPending = false
            startNavigationSession()
        } else {
            requestPreviewRoute()
        }
    }

    private data class RouteProgress(val remainingMeters: Double, val distanceFromRouteMeters: Double, val nearestSegmentIndex: Int)
    private data class SegmentProjection(val point: GeoPoint, val distanceMeters: Double)
    private data class ManeuverInfo(val distanceMeters: Double, val instruction: String, val arrowRotation: Float, val key: String)
}
