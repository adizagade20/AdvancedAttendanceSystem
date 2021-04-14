package com.aditya.attendancesystem.student

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aditya.attendancesystem.databinding.ActivityStudentRecordAttendanceBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RecordAttendance : AppCompatActivity() {
	
	companion object {
		private const val TAG = "RecordAttendance"
	}
	
	
	private lateinit var binding: ActivityStudentRecordAttendanceBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityStudentRecordAttendanceBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		val sharedPreferences = getSharedPreferences("dynamicLink", MODE_PRIVATE)
		val privateKey = sharedPreferences.getString("privateKey", null)
		val className = sharedPreferences.getString("className", null)
		val docId = sharedPreferences.getString("date", null)
		sharedPreferences.edit().clear().apply()
		
		getAttendanceData(privateKey, className, docId)
		
	}
	
	private fun getAttendanceData(privateKey: String?, className: String?, docId: String?) {
		val db = Firebase.firestore.collection("attendance").document(privateKey!!).collection(className!!).document(docId!!)
		
	}
	
}