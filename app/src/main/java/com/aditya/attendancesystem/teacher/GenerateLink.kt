package com.aditya.attendancesystem.teacher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aditya.attendancesystem.BuildConfig
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherGenerateLinkBinding
import com.aditya.attendancesystem.teacher.helperclasses.DynamicLinkModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.DynamicLink.*
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


class GenerateLink : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {
	
	companion object {
		private const val TAG = "TeacherHome"
		private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
		private val LOCATION_REQUEST_CODE = Random.nextInt(1000)
		private val GPS_REQUEST_CODE = Random.nextInt(1000)
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private var lectureDate: String? = null
	private var timeStamp: Long?= null
	private val lectureTime = HashMap<String, Int>()
	private var lectureDuration: String? = null
	private var expireTime: String? = null
	private var radiusSelected: String? = null
	private var latitudeMyLocation: Double?= null
	private var longitudeMyLocation: Double?= null
	
	
	private lateinit var className: String
	
	private lateinit var googleMap: GoogleMap
	private lateinit var locationClient : FusedLocationProviderClient
	
	
	private lateinit var binding: ActivityTeacherGenerateLinkBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherGenerateLinkBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		className = intent.getStringExtra("className").toString()
		
		supportActionBar?.title = "Generate Attendance Link"
		supportActionBar?.subtitle = className
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		with(binding) {
			
			generateDateLayout.editText?.setOnClickListener {
				val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(MaterialDatePicker.todayInUtcMilliseconds()).setTitleText("Lecture Date").build()
				datePicker.show(supportFragmentManager, null)
				
				datePicker.addOnPositiveButtonClickListener {
					timeStamp = datePicker.selection?.plus(19800)
					generateDateLayout.hint = datePicker.headerText
					lectureDate = datePicker.headerText
					generateDateLayout.isErrorEnabled = false
				}
			}
			
			
			generateStartTimeLayout.editText?.setOnClickListener {
				val currentTime = Calendar.getInstance()
				val hour = currentTime.get(Calendar.HOUR_OF_DAY)
				val minute = currentTime.get(Calendar.MINUTE)
				val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(hour - 1).setMinute(minute).setTitleText("Lecture Start Time").build()
				timePicker.show(supportFragmentManager, null)
				timePicker.addOnPositiveButtonClickListener {
					generateStartTimeLayout.hint = "${timePicker.hour} : ${timePicker.minute}"
					lectureTime["startHour"] = timePicker.hour
					lectureTime["startMinute"] = timePicker.minute
					generateStartTimeLayout.isErrorEnabled = false
				}
			}
			
			
			val lecturePeriodArray = arrayOf("10 min", "20 min", "30 min", "40 min", "45 min", "60 min", "75 min", "90 min", "105 min", "120 min")
			val periodAdapter = ArrayAdapter(applicationContext, R.layout.support_simple_spinner_dropdown_item, lecturePeriodArray)
			generateDuration.setAdapter(periodAdapter)
			generateDuration.setOnItemClickListener { _, _, position, _ ->
				lectureDuration = lecturePeriodArray[position]
				generateDurationLayout.isErrorEnabled = false
			}
			
			
			val expireTimeArray = arrayOf("1 min", "2 min", "3 min", "4 min", "5 min", "6 min", "7 min", "8 min", "9 min", "10 min", "11 min", "12 min", "13 min", "14 min", "15 min")
			val expireAdapter = ArrayAdapter(applicationContext, R.layout.support_simple_spinner_dropdown_item, expireTimeArray)
			generateExpire.setAdapter(expireAdapter)
			generateExpire.setOnItemClickListener { _, _, position, _ ->
				expireTime = expireTimeArray[position]
				generateExpireLayout.isErrorEnabled = false
			}
			
			
			generateClassroomActivationSwitch.setOnCheckedChangeListener { _, isChecked ->
				if (isChecked) {
					generateAdvanced.visibility = View.VISIBLE
					generateExpireLayout.visibility = View.VISIBLE
					generateRadiusLayout.visibility = View.VISIBLE
					
					if (isPermissionGranted()) {
						CoroutineScope(Dispatchers.Main).launch {
							initMap()
						}
					} else {
						ActivityCompat.requestPermissions(this@GenerateLink, REQUIRED_PERMISSIONS, LOCATION_REQUEST_CODE)
					}
				} else {
					binding.generateAdvanced.visibility = View.INVISIBLE
					binding.generateExpireLayout.visibility = View.INVISIBLE
					binding.generateRadiusLayout.visibility = View.INVISIBLE
				}
			}
			generateClassroomActivationSwitch.isChecked = true
			
			
			val radiusArray = arrayOf("100m", "200m", "300m", "400m", "500m", "600m", "700m", "800m", "900m", "1000m")
			val radiusAdapter = ArrayAdapter(applicationContext, R.layout.support_simple_spinner_dropdown_item, radiusArray)
			generateRadius.setAdapter(radiusAdapter)
			generateRadius.setOnItemClickListener { _, _, position, _ ->
				radiusSelected = radiusArray[position]
				generateRadiusLayout.isErrorEnabled = false
			}
		
		}
	}
	
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.generate_link_menu, menu)
		return true
	}
	
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.generate_create -> {
				if (verify()) {
					CoroutineScope(Dispatchers.IO).launch {
						generateAttendance()
					}
				}
			}
		}
		return super.onOptionsItemSelected(item)
	}
	
	
	private fun verify(): Boolean {
		with(binding) {
			if (lectureDate == null) {
				generateDateLayout.error = "Required"
				return false
			} else
				generateDateLayout.isErrorEnabled = false
			
			if(lectureTime.size == 0) {
				generateStartTimeLayout.error = "Required"
				return false
			} else
				generateStartTimeLayout.isErrorEnabled = false
			
			if(lectureDuration == null) {
				generateDurationLayout.error = "Required"
				return false
			} else
				generateDurationLayout.isErrorEnabled = false
			
			
			if(generateClassroomActivationSwitch.isChecked) {
				if(expireTime == null) {
					generateExpireLayout.error = "Required"
					return false
				} else
					generateExpireLayout.isErrorEnabled = false
				
				if(radiusSelected == null) {
					generateRadiusLayout.error = "Required"
					return false
				} else
					generateRadiusLayout.isErrorEnabled = false
				
				if(latitudeMyLocation == null || longitudeMyLocation == null) {
					Toast.makeText(applicationContext, "Location is not yet loaded, please wait or reload the page", Toast.LENGTH_LONG).show()
					return false
				}
			}
			
		}
		return true
	}
	
	
	private fun generateAttendance() {
		runOnUiThread { binding.generateProgressLayout.visibility = View.VISIBLE }
		
		val calendar = Calendar.getInstance()
		calendar.timeInMillis = timeStamp!!
		
		val year = calendar.get(Calendar.YEAR)
		val month = if((calendar.get(Calendar.MONTH)+1).toString().length < 2) "0${calendar.get(Calendar.MONTH)+1}" else calendar.get(Calendar.MONTH)+1
		val day = if((calendar.get(Calendar.DAY_OF_MONTH)).toString().length < 2) "0${calendar.get(Calendar.DAY_OF_MONTH)}" else calendar.get(Calendar.DAY_OF_MONTH)
		val startHour = if(lectureTime["startHour"].toString().length < 2) "0${lectureTime["startHour"].toString()}" else lectureTime["startHour"].toString()
		val startMinute = if(lectureTime["startMinute"].toString().length < 2) "0${lectureTime["startMinute"].toString()}" else lectureTime["startMinute"].toString()
		
		val time = "$startHour:$startMinute"
		val docId = "$year$month${day}_$startHour$startMinute"
		
		val attendanceLinkDataClass = DynamicLinkModel(
			id = docId,
			count = 0,
			date = lectureDate,
			duration = lectureDuration?.replace("[^0-9]".toRegex(), "")?.toInt(),
			geoPoint = GeoPoint(0.0, 0.0),
			isActivated = true,
			isClassroomActivated = false,
			link = "",
			radius = 0,
			time = time
		)
		if(binding.generateClassroomActivationSwitch.isChecked) {
			with(attendanceLinkDataClass) {
				isClassroomActivated = true
				geoPoint = GeoPoint(latitudeMyLocation!!, longitudeMyLocation!!)
				radius = radiusSelected?.replace("[^0-9]".toRegex(), "")?.toInt()
			}
		}
		
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document(docId)
		db.set(attendanceLinkDataClass)
			.addOnSuccessListener {
				generateShortLink(docId, db, time)
			}
			.addOnFailureListener {
				Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_LONG).show()
				runOnUiThread { binding.generateProgressLayout.visibility = View.GONE }
			}
	}
	
	
	private fun generateShortLink(docId: String, db: DocumentReference, time: String) {
		Firebase.dynamicLinks.shortLinkAsync {
			link = Uri.parse("https://advancedattendance.web.app?privateKey=${Firebase.auth.uid}&className=${className}&date=${docId}")
			domainUriPrefix = "https://advancedattendance.page.link"
			
			androidParameters(BuildConfig.APPLICATION_ID) {
				fallbackUrl = Uri.parse("https://google.com")
			}
			socialMetaTagParameters {
				title = resources.getString(R.string.app_name)
				description = "Class : $className"
				imageUrl = Uri.parse("")
			}
		}
			.addOnSuccessListener { shortLink ->
				val text = "The attendance link for $className for date $lectureDate and time $time is ${shortLink.shortLink}"
				db.update("link", shortLink.shortLink.toString())
					.addOnSuccessListener {
						val sendIntent = Intent().apply {
							action = Intent.ACTION_SEND
							putExtra(Intent.EXTRA_TITLE, "Attendance for class $className")
							putExtra(Intent.EXTRA_TEXT, text)
							flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
							type = "text/plain"
						}
						val shareIntent = Intent.createChooser(sendIntent, null)
						startActivity(shareIntent)
						finish()
					}
			}
			.addOnFailureListener {
				Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_LONG).show()
				Log.d(TAG, "generateShortLink: $it")
				runOnUiThread { binding.generateProgressLayout.visibility = View.GONE }
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
				binding.generateClassroomActivationSwitch.isChecked = false
			}
		}
	}
	
	
	private fun initMap() {
		if (isPermissionGranted()) {
			if (isGPSEnabled()) {
				val supportMapFragment = supportFragmentManager.findFragmentById(R.id.generate_advanced_map) as SupportMapFragment
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
				latitudeMyLocation = location.latitude
				longitudeMyLocation  = location.longitude
				
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
		val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 19f)
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
				.setNegativeButton("No") { _, _ ->
					binding.generateClassroomActivationSwitch.isChecked = false
				}
				.show()
		}
		return false
	}
	
	
	@SuppressLint("MissingPermission")
	override fun onMapReady(googleMap: GoogleMap?) {
		if (isPermissionGranted()) {
			if (googleMap != null) {
				this.googleMap = googleMap
				googleMap.isMyLocationEnabled = true
			}
		}
	}
	
}