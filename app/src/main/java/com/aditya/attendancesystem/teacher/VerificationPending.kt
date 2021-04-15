package com.aditya.attendancesystem.teacher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.databinding.ActivityTeacherVerificationPendingBinding
import com.aditya.attendancesystem.teacher.adapters.VerificationPendingAdapter
import com.aditya.attendancesystem.teacher.helperclasses.StudentDataVerification
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class VerificationPending : AppCompatActivity() {
	
	companion object {
		private const val TAG = "VerificationPending"
	}
	
	private lateinit var className: String
	private lateinit var classImage: String
	
	private lateinit var listener: ListenerRegistration
	
	
	lateinit var binding: ActivityTeacherVerificationPendingBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherVerificationPendingBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Pending Requests"
		
		className = getSharedPreferences("classDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("classImages", MODE_PRIVATE).getString(className, "").toString()
		
		getStudentList()
		
	}
	
	
	private fun getStudentList() {
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students").collection("verification_pending")
		val studentsList = ArrayList<StudentDataVerification>()
		val adapter = VerificationPendingAdapter(className, studentsList)
		
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.d(TAG, "getVerifiedStudents: ${error.localizedMessage}")
				Log.d(TAG, "getVerifiedStudents: $error")
				return@addSnapshotListener
			}
			
			if (value != null) {
				Log.d(TAG, "getVerifiedStudents: ${value.documents}")
			}
			
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
						DocumentChange.Type.ADDED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentDataVerification>()
								obj.id = dc.document.id
								studentsList.add(dc.newIndex, obj)
								adapter.notifyItemInserted(dc.newIndex)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentDataVerification>()
								obj.id = dc.document.id
								studentsList.add(dc.newIndex, obj)
								adapter.notifyItemChanged(dc.newIndex)
							}
						}
						DocumentChange.Type.REMOVED -> {
							studentsList.removeAt(dc.oldIndex)
							adapter.notifyItemRemoved(dc.oldIndex)
						}
					}
				}
			}
			binding.verifiedRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
			binding.verifiedRecyclerView.adapter = adapter
		}
	}
	
	
	override fun onStop() {
		super.onStop()
		listener.remove()
	}
	
	
}