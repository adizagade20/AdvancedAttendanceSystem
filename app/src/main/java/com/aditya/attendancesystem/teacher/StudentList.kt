package com.aditya.attendancesystem.teacher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.databinding.ActivityTeacherStudentListBinding
import com.aditya.attendancesystem.teacher.adapters.StudentListAdapter
import com.aditya.attendancesystem.teacher.helperclasses.StudentAttendanceRecord
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
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
		
		className = getSharedPreferences("classDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("classImages", MODE_PRIVATE).getString(className, "").toString()
		supportActionBar?.subtitle  = className
		
		CoroutineScope(Dispatchers.IO).launch {
			getStudentList()
		}
		
	}
	
	
	private suspend fun getStudentList() = withContext(Dispatchers.IO) {
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students").collection("verified")
		val verifiedStudentsList = ArrayList<StudentAttendanceRecord>()
		
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
								val obj = dc.document.toObject<StudentAttendanceRecord>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentAttendanceRecord>()
								obj.id = dc.document.id
								verifiedStudentsList.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.REMOVED -> {
							verifiedStudentsList.removeAt(dc.oldIndex)
						}
					}
				}
				getAttendanceLinks(verifiedStudentsList)
			}
		}
	}
	
	
	private fun getAttendanceLinks(verifiedStudentsList: ArrayList<StudentAttendanceRecord>) {
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
	
	
	private fun getAttendances(verifiedStudentsList: ArrayList<StudentAttendanceRecord>, attendanceLinks: ArrayList<String>) {
		for(entry in attendanceLinks) {
			listener = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document(entry)
				.collection("attendance").document("attendance")
				.addSnapshotListener { value, error ->
					if (error != null) {
						Log.d(TAG, "getAttendances: ${error.localizedMessage}")
						Log.d(TAG, "getAttendances: $error")
						return@addSnapshotListener
					}
					
					if(value != null) {
						for((index, student) in verifiedStudentsList.withIndex()) {
							if(value.contains(student.RollNumber.toString())) {
								verifiedStudentsList[index].attendanceCount = verifiedStudentsList[index].attendanceCount.plus(1)
								verifiedStudentsList[index].attendanceDates?.add(value.get("").toString())
								Log.d(TAG, "getAttendances: verifiedStudentsList: $verifiedStudentsList")
								runOnUiThread {
									binding.studentListRecyclerView.adapter?.notifyItemChanged(index)
								}
							}
						}
					}
				}
		}
	}
	
	
	override fun onStop() {
		super.onStop()
		job.cancel()
		listener.remove()
	}
	
	
}