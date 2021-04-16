package com.aditya.attendancesystem.student

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.concurrent.schedule
import kotlin.random.Random

class RecordAttendance : AppCompatActivity(), OnMapReadyCallback {
	
	companion object {
		private const val TAG = "RecordAttendance"
		private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
		private val LOCATION_REQUEST_CODE = Random.nextInt(1000)
		private val GPS_REQUEST_CODE = Random.nextInt(1000)
	}
	
	private var privateKey: String? = null
	private var className: String? = null
	private var date: String? = null
	
	
	private lateinit var googleMap: GoogleMap
	private lateinit var locationClient: FusedLocationProviderClient
	
	private lateinit var geoPoint: GeoPoint
	private var radius: Int = 200
	private lateinit var myLocation: LatLng
	
	private var isActivated: Boolean = false
	private var isClassroomActivated: Boolean = false
	
	private lateinit var binding: ActivityStudentRecordAttendanceBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityStudentRecordAttendanceBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		val sharedPreferences = getSharedPreferences("DynamicLink", MODE_PRIVATE)
		privateKey = sharedPreferences.getString("privateKey", null)
		className = sharedPreferences.getString("className", null)
		date = sharedPreferences.getString("date", null)
		sharedPreferences.edit().clear().apply()
		
		supportActionBar?.hide()
		if (privateKey != null && className != null && date != null) {
			getAttendanceData()
		} else {
			finish()
		}
		
	}
	
	private fun getAttendanceData() {
		val db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document(date!!)
		db.get()
			.addOnFailureListener {
				Log.d(TAG, "getAttendanceData: $it")
				return@addOnFailureListener
			}
			.addOnSuccessListener { it ->
				
				binding.recordClassName.text = className
				binding.recordDate.text = it.get("date").toString()
				binding.recordTime.text = it.get("time").toString()
				"${it.get("duration")} minutes".also { binding.recordDuration.text = it }
				
				geoPoint = it.getGeoPoint("geoPoint") as GeoPoint
				radius = it.get("radius").toString().replace("[^0-9]".toRegex(), "").toInt()
				isActivated = it.getBoolean("isActivated") == true
				isClassroomActivated = it.getBoolean("isClassroomActivated") == true
				
				if (!isActivated) {
					AlertDialog.Builder(this)
						.setTitle("Expired")
						.setMessage("You are late, link has been closed by the teacher. \nIf you think this is the mistake, you can contact the teacher to reactivate")
						.setCancelable(false)
						.setPositiveButton("Close") { _, _ ->
							finish()
						}
						.show()
					return@addOnSuccessListener
				}
				
				isGPSEnabled()
				if (isPermissionGranted()) {
					initMap()
				} else {
					ActivityCompat.requestPermissions(this@RecordAttendance, REQUIRED_PERMISSIONS, LOCATION_REQUEST_CODE)
				}
			}
	}
	
	
	private fun isPermissionGranted() = REQUIRED_PERMISSIONS.all {
		ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
	}
	
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == LOCATION_REQUEST_CODE) {
			if (isPermissionGranted()) {
					initMap()
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
				if(isClassroomActivated) {
					myLocation = LatLng(location.latitude, location.longitude)
				} else {
					myLocation = LatLng(0.0, 0.0)
				}
				gotoLocation(myLocation)
			}
		}
	}
	
	
	private fun gotoLocation(myLocation: LatLng) {
		val cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation, 15f)
		googleMap.moveCamera(cameraUpdate)
		googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
		
		googleMap.addCircle(
			CircleOptions()
				.center(myLocation)
				.radius(radius.toDouble())
				.strokeWidth(3f)
				.strokeColor(ResourcesCompat.getColor(resources, R.color.design_default_color_primary, null))
		)
		val point = LatLng(geoPoint.latitude, geoPoint.longitude)
		
		googleMap.addMarker(
			MarkerOptions()
				.position(point)
				.title("Classroom")
		)
		
		isStudentVerified()
	}
	
	
	private fun isGPSEnabled(): Boolean {
		val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
		val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
		if (providerEnable) {
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
//			googleMap.apply {
//				val point = LatLng(geoPoint.latitude, geoPoint.longitude)
//				addMarker(
//					MarkerOptions()
//						.position(point)
//						.title("Classroom")
//				)
//			}
//			isStudentVerified()
		}
	}
	
	
	
	
	
	
	
	
	private fun isStudentVerified() {
		
		val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
		var db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document("students")
			.collection("verified").document(Firebase.auth.uid.toString())
		db.get()
			.addOnSuccessListener {
				Log.d(TAG, "isStudentVerified: $it")
				Log.d(TAG, "isStudentVerified: ${it.data}")
				if (it.get("rollNumber") == sharedPreferences.getString("rollNumber", "")) {
					isInsideRadius()
				} else {
					AlertDialog.Builder(this)
						.setTitle("Not Verified")
						.setIcon(android.R.drawable.stat_notify_error)
						.setMessage("You are not a verified student for this class : $className")
						.setCancelable(false)
						.setPositiveButton("Apply") { _, _ ->
							val myData = hashMapOf(
								"email" to sharedPreferences.getString("email", ""),
								"phone" to sharedPreferences.getString("phone", ""),
								"name" to sharedPreferences.getString("name", ""),
								"rollNumber" to sharedPreferences.getString("rollNumber", "")
							)
							
							db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document("students")
								.collection("verification_pending").document(Firebase.auth.uid.toString())
							db.set(myData)
								.addOnSuccessListener {
									Toast.makeText(applicationContext, "Applied Successfully", Toast.LENGTH_LONG).show()
									Timer().schedule(1500) {
										runOnUiThread { finish() }
									}
								}
						}
						.setNegativeButton("Cancel") { _, _ ->
							finish()
						}
						.show()
				}
			}
	}
	
	
	private fun isInsideRadius() {
		if(isClassroomActivated) {
			val results = FloatArray(1)
			Location.distanceBetween(myLocation.latitude, myLocation.longitude, geoPoint.latitude, geoPoint.longitude, results)
			if (results[0] <= radius) {
				recordAttendance()
			} else {
				AlertDialog.Builder(this)
					.setTitle("Error")
					.setIcon(android.R.drawable.stat_notify_error)
					.setMessage("You are not in the ${radius}m radius of given location. \nYou are ${results[0]}m away.\n\n NOT ALLOWED")
					.setCancelable(false)
					.setPositiveButton("Close") { _, _ ->
						finish()
					}
					.show()
			}
		}
		else {
			recordAttendance()
		}
	}
	
	
	private fun recordAttendance() {
		val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
		val rollNumber = sharedPreferences.getString("rollNumber", "")
		val db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document(date!!).collection("attendance").document("attendance")
		if (rollNumber != null) {
			db.update(rollNumber, true, "geoPoint", GeoPoint(myLocation.latitude, myLocation.longitude))
				.addOnSuccessListener {
					AlertDialog.Builder(this)
						.setTitle("Successful")
						.setIcon(android.R.drawable.checkbox_on_background)
						.setMessage("Attendance recorded successfully for Roll Number $rollNumber")
						.setCancelable(false)
						.setPositiveButton("Close") { _, _ ->
							finish()
						}
						.show()
				}
				.addOnFailureListener {
					Log.d(TAG, "recordAttendance: $it")
					db.set(hashMapOf(rollNumber to true, "geoPoint" to GeoPoint(myLocation.latitude, myLocation.longitude)))
						.addOnSuccessListener {
							AlertDialog.Builder(this)
								.setTitle("Successful")
								.setIcon(android.R.drawable.checkbox_on_background)
								.setMessage("Attendance recorded successfully for Roll Number $rollNumber")
								.setCancelable(false)
								.setPositiveButton("Close") { _, _ ->
									finish()
								}
								.show()
						}
				}
		} else {
			AlertDialog.Builder(this)
				.setTitle("please login again")
				.setIcon(android.R.drawable.stat_notify_error)
				.setMessage("There is some error, please re-login")
				.setCancelable(false)
				.setPositiveButton("Logout") { _, _ ->
					Firebase.auth.signOut()
				}
				.show()
		}
	}
	
	
}
