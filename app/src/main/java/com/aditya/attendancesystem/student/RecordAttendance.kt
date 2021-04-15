package com.aditya.attendancesystem.student

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityStudentRecordAttendanceBinding
import com.aditya.attendancesystem.teacher.GenerateLink
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class RecordAttendance : AppCompatActivity(), CoroutineScope, OnMapReadyCallback {
	
	companion object {
		private const val TAG = "RecordAttendance"
		private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
		private val LOCATION_REQUEST_CODE = Random.nextInt(1000)
		private val GPS_REQUEST_CODE = Random.nextInt(1000)
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private lateinit var googleMap: GoogleMap
	private lateinit var locationClient : FusedLocationProviderClient
	
	
	private lateinit var binding: ActivityStudentRecordAttendanceBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityStudentRecordAttendanceBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		val sharedPreferences = getSharedPreferences("dynamicLink", MODE_PRIVATE)
		val privateKey = sharedPreferences.getString("privateKey", null)
		val className = sharedPreferences.getString("className", null)
		val date = sharedPreferences.getString("date", null)
		sharedPreferences.edit().clear().apply()
		
		supportActionBar?.hide()
		if(privateKey != null && className != null && date != null) {
			getAttendanceData(privateKey, className, date)
		}
		
		CoroutineScope(Dispatchers.IO).launch {
			initMap()
		}
		
	}
	
	private fun getAttendanceData(privateKey: String?, className: String?, date: String?) {
		val db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document(date!!)
		db.get()
			.addOnFailureListener {
				Log.d(TAG, "getAttendanceData: $it")
			}
			.addOnSuccessListener { it ->
				binding.recordClassName.text = className
				binding.recordDate.text = it.get("date").toString()
				binding.recordTime.text = it.get("time").toString()
				"${it.get("duration")} minutes".also { binding.recordDuration.text = it }
			}
	}
	
	
	
	
	
	
	
	
	
	
	private fun isPermissionGranted() = REQUIRED_PERMISSIONS.all {
		ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
	}
	
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == LOCATION_REQUEST_CODE) {
			if (isPermissionGranted()) {
				CoroutineScope(Dispatchers.Main).launch {
					initMap()
				}
			} else {
				Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
				
			}
		}
	}
	
	
	private fun initMap() {
		if (isPermissionGranted()) {
			if (isGPSEnabled()) {
				val supportMapFragment = supportFragmentManager.findFragmentById(R.id.record_map) as SupportMapFragment
				runOnUiThread { supportMapFragment.getMapAsync(this) }
				getCurrentLocation()
			}
		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, LOCATION_REQUEST_CODE)
		}
	}
	
	
	@SuppressLint("MissingPermission", "VisibleForTests")
	private fun getCurrentLocation() {
		locationClient = FusedLocationProviderClient(this)
		locationClient.lastLocation.addOnCompleteListener {
			if (it.isSuccessful) {
				val location = it.result
				Log.d(TAG, "getCurrentLocation: location: $location")
				gotoLocation(location.latitude, location.longitude)

//				val latitude = 18.7216973
				//				val longitude = 75.1481091
//
//				val results = FloatArray(1)
//				Location.distanceBetween(location.latitude, location.longitude, latitude, longitude, results)
//				val distanceInMeters = results[0]
//				Log.d(TAG, "getCurrentLocation: $distanceInMeters")
			}
		}
	}
	
	
	private fun gotoLocation(latitude: Double, longitude: Double) {
		val latLng = LatLng(latitude, longitude)
		val cameraUpdate = CameraUpdateFactory.newLatLng(latLng)
		googleMap.moveCamera(cameraUpdate)
		googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
		
		googleMap.addCircle(
			CircleOptions()
				.center(LatLng(latitude, longitude))
				.radius(500.0)
				.strokeWidth(3f)
				.strokeColor(ResourcesCompat.getColor(resources, R.color.design_default_color_primary, null))
		)
	}
	
	
	private fun isGPSEnabled(): Boolean {
		val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
		val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
		if(providerEnable) {
			return true
		} else {
			AlertDialog.Builder(this)
				.setTitle("GPS permission")
				.setMessage("GPS is required for classroom mode to work, please enable GPS")
				.setCancelable(false)
				.setPositiveButton("Yes") { _, _ ->
					val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
					startActivityForResult(intent, GPS_REQUEST_CODE)
				}
				.show()
		}
		return false
	}
	
	
	@SuppressLint("MissingPermission")
	override fun onMapReady(googleMap: GoogleMap?) {
		if (googleMap != null) {
			this.googleMap = googleMap
			googleMap.isMyLocationEnabled = true
		}
	}
	
	
}