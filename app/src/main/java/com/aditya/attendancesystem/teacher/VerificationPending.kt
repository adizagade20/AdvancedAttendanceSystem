package com.aditya.attendancesystem.teacher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aditya.attendancesystem.databinding.ActivityTeacherVerificationPendingBinding
import com.aditya.attendancesystem.teacher.adapters.VerificationPendingAdapter
import com.aditya.attendancesystem.teacher.helperclasses.StudentDataVerification
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext

class VerificationPending : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "VerificationPending"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private lateinit var className: String
	private lateinit var classImage: String
	
	
	lateinit var binding: ActivityTeacherVerificationPendingBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherVerificationPendingBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = ""
		
		className = getSharedPreferences("classDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("classImages", MODE_PRIVATE).getString(className, "").toString()
		
		getVerifiedStudents()
		
	}
	
	
	private fun getVerifiedStudents() {
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students").collection("verified")
		val verifiedStudentsList = ArrayList<StudentDataVerification>()
		val adapter = VerificationPendingAdapter(applicationContext, className, verifiedStudentsList)
		
		val listener = db.addSnapshotListener { value, error ->
			if(error != null) {
				Log.d(TAG, "getVerifiedStudents: ${error.localizedMessage}")
				Log.d(TAG, "getVerifiedStudents: $error")
				return@addSnapshotListener
			}
			
			if(value != null) {
				for(dc in value.documentChanges) {
					when(dc.type) {
						DocumentChange.Type.ADDED -> {
							if(dc.document.exists()) {
								val obj = dc.document.toObject<StudentDataVerification>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
								adapter.notifyItemInserted(dc.newIndex)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if(dc.document.exists()) {
								val obj = dc.document.toObject<StudentDataVerification>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
								adapter.notifyItemChanged(dc.newIndex)
							}
						}
						DocumentChange.Type.REMOVED -> {
							verifiedStudentsList.removeAt(dc.oldIndex)
							adapter.notifyItemRemoved(dc.oldIndex)
						}
					}
				}
			}
			binding.verifiedRecyclerView.adapter = adapter
		}
		Timer().schedule(1000) {
			runOnUiThread {
				listener.remove()
			}
		}
	}
	
	
}