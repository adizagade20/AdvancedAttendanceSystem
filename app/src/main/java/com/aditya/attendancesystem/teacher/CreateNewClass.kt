package com.aditya.attendancesystem.teacher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aditya.attendancesystem.databinding.ActivityTeacherCreateNewClassBinding

class CreateNewClass : AppCompatActivity() {
	
	companion object {
		private const val TAG = "CreateNewClass"
	}
	
	
	
	
	
	private lateinit var binding : ActivityTeacherCreateNewClassBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherCreateNewClassBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Create New Class"
		
		
		
		
		
		
		
		
	}
}