package com.aditya.attendancesystem.teacher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.databinding.ActivityTeacherStudentListBinding
import com.aditya.attendancesystem.teacher.adapters.StudentListAdapter
import com.aditya.attendancesystem.teacher.helperclasses.StudentAttendanceModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class StudentList : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "StudentList"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private lateinit var className: String
	private lateinit var classImage: String
	
	private lateinit var listener: ListenerRegistration
	
	
	private lateinit var binding: ActivityTeacherStudentListBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherStudentListBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "StudentList"
		
		className = getSharedPreferences("ClassDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("ClassImages", MODE_PRIVATE).getString(className, "").toString()
		supportActionBar?.subtitle  = className
		
		CoroutineScope(Dispatchers.IO).launch {
			getStudentList()
		}
		
	}
	
	
	private suspend fun getStudentList() = withContext(Dispatchers.IO) {
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students").collection("verified")
		val verifiedStudentsList = ArrayList<StudentAttendanceModel>()
		
		runOnUiThread {
			binding.studentListRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
			val adapter = StudentListAdapter(className, verifiedStudentsList)
			binding.studentListRecyclerView.adapter = adapter
		}
		
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.d(TAG, "getStudentList: ${error.localizedMessage}")
				Log.d(TAG, "getStudentList: $error")
				return@addSnapshotListener
			}
			
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
						DocumentChange.Type.ADDED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentAttendanceModel>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentAttendanceModel>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.REMOVED -> {
							verifiedStudentsList.removeAt(dc.oldIndex)
						}
					}
				}
				getAttendanceRecords(verifiedStudentsList)
			}
		}
	}
	
	
	private fun getAttendanceRecords(verifiedStudentsList: ArrayList<StudentAttendanceModel>) {
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className)
		val attendanceLinks = ArrayList<String>()
		
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.d(TAG, "getAttendanceLinks: ${error.localizedMessage}")
				Log.d(TAG, "getAttendanceLinks: $error")
				return@addSnapshotListener
			}
			
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
						DocumentChange.Type.ADDED -> {
							attendanceLinks.add(dc.newIndex, dc.document.id)
						}
						DocumentChange.Type.MODIFIED -> {
							attendanceLinks[dc.newIndex] = dc.document.id
						}
						DocumentChange.Type.REMOVED -> {
							attendanceLinks.removeAt(dc.newIndex)
						}
					}
				}
				getAttendances(verifiedStudentsList, attendanceLinks)
			}
		}
	}
	
	
	private fun getAttendances(verifiedStudentsList: ArrayList<StudentAttendanceModel>, attendanceLinks: ArrayList<String>) {
		for ((index, student) in verifiedStudentsList.withIndex()) {
			for (link in attendanceLinks) {
				val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString())
					.collection(className).document(link).collection("attendance").document("attendance")
				db.addSnapshotListener { value, error ->
					if (error != null) {
						Log.d(TAG, "getStudentWiseAttendanceRecords: $error")
						return@addSnapshotListener
					}
					
					if (value != null) {
						if (value.getBoolean(student.rollNumber.toString()) == true) {
							student.attendanceCount += 1
							binding.studentListRecyclerView.adapter?.notifyItemChanged(index)
						}
					}
				}
				
			}
		}
	}
	
	
	override fun onStop() {
		super.onStop()
		try {
			job.cancel()
			listener.remove()
		} catch (e: Exception) {}
	}
	
	
}