package com.aditya.attendancesystem.student

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aditya.attendancesystem.databinding.ActivityStudentHomeBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Home : AppCompatActivity() {
	
	companion object {
		private const val TAG = "StudentHome"
	}
	
	
	
	private lateinit var binding : ActivityStudentHomeBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityStudentHomeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		
		
		
		
	}
	
	
	
}