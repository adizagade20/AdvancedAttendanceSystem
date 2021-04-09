package com.aditya.attendancesystem.teacher

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherHomeBinding
import com.aditya.attendancesystem.teacher.adapters.ClassesAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Home : AppCompatActivity() {
	
	companion object {
		private const val TAG = "TeacherHome"
	}
	
	
	private lateinit var classesListener : ListenerRegistration
	
	
	
	
	
	private lateinit var binding: ActivityTeacherHomeBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Classes"
		
		
		getClassesList()
		
		with(binding) {
			teacherHomeFloatingButton.setOnClickListener {
				startActivity(Intent(applicationContext, CreateNewClass::class.java))
			}
		}
	}
	
	
	private fun getClassesList() {
		classesListener = Firebase.firestore.collection("attendance").document("${Firebase.auth.currentUser?.uid}")
			.addSnapshotListener { value, error ->
				Log.d(TAG, "getClassesList: $value")
				if(error!= null) {
					Toast.makeText(applicationContext, error.localizedMessage, Toast.LENGTH_LONG).show()
				} else {
					val classes = ArrayList<String>()
					val urls = ArrayList<String>()
					if (value != null) {
						if (value.data?.entries != null) {
							for (i in value.data?.entries!!) {
								if((i.key.toInt()%2) == 1) {
									classes.add(i.value.toString())
								}
								else if((i.key.toInt()%2) == 0) {
									urls.add(i.value.toString())
								}
							}
						}
					}
					if(classes.size != 0 || urls.size != 0) {
						binding.teacherHomeRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
						binding.teacherHomeRecyclerView.adapter = ClassesAdapter(classes, urls)
					}
					else {
						binding.teacherHomeNoClasses.visibility = View.VISIBLE
					}
					
					
				}
			}
	}
	
	
	override fun onStop() {
		super.onStop()
		classesListener.remove()
	}
	
	
}