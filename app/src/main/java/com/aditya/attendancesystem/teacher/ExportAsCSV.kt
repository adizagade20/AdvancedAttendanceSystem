package com.aditya.attendancesystem.teacher

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherExportAsCSVBinding
import com.aditya.attendancesystem.databinding.TeacherExportHeaderBinding
import com.aditya.attendancesystem.databinding.TeacherExportRowBinding
import com.aditya.attendancesystem.teacher.helperclasses.DynamicLinkModel
import com.aditya.attendancesystem.teacher.helperclasses.StudentAttendanceModel
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext


class ExportAsCSV : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "ExportAsCSV"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private lateinit var className: String
	private lateinit var classImage: String
	
	private lateinit var listener: ListenerRegistration
	
	private lateinit var studentList: ArrayList<StudentAttendanceModel>
	private lateinit var allLinks: ArrayList<DynamicLinkModel>
	
	private lateinit var tableRowParams: LinearLayout.LayoutParams
	
	private lateinit var binding: ActivityTeacherExportAsCSVBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherExportAsCSVBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Export"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		className = getSharedPreferences("ClassDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("ClassImages", MODE_PRIVATE).getString(className, "").toString()
		
		tableRowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		
		CoroutineScope(Dispatchers.IO).launch {
			allLinks = getAttendanceRecords()
			
			val columnTitles = ArrayList<String>()
			columnTitles.add("Roll No.")
			columnTitles.add("Name")
			for (link in allLinks)
				columnTitles.add(link.date?.substring(0, 6).toString())
			columnTitles.add("Attended")
			columnTitles.add("Total")
			columnTitles.add("Percentage")
			runOnUiThread { addColumn(columnTitles) }
			
			
			studentList = getStudentList()
			
			for ((index, student) in studentList.withIndex()) {
				val studentEntry = ArrayList<String>()
				studentEntry.add("${student.rollNumber}")
				studentEntry.add("${student.name}")
				for (i in 0 until allLinks.size + 4)
					studentEntry.add("")
				runOnUiThread { addRow(studentEntry) }
				
				Timer().schedule(1000) {
					val row = binding.exportTableLayout.getChildAt(index + 1) as TableRow
					val textView = row.getChildAt(allLinks.size + 3) as MaterialTextView
					runOnUiThread { textView.text = allLinks.size.toString() }
				}
				
				getStudentWiseAttendanceRecords(index, student)
			}
		}
		
		binding.exportFloating.setOnClickListener {
			binding.exportProgressLayout.visibility = View.VISIBLE
			binding.exportFloating.isClickable = false
			binding.exportFloating.isFocusable = false
			CoroutineScope(Dispatchers.IO).launch {
				exportToCSV()
			}
		}
		
	}
	
	
	private suspend fun getStudentList(): ArrayList<StudentAttendanceModel> = withContext(Dispatchers.IO) {
		val studentList = ArrayList<StudentAttendanceModel>()
		val done = CountDownLatch(1)
		
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students").collection("verified")
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.d(TAG, "getStudentList: ${error.localizedMessage}")
				return@addSnapshotListener
			}
			
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
						DocumentChange.Type.ADDED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentAttendanceModel>()
								obj.id = dc.document.id
								studentList.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<StudentAttendanceModel>()
								obj.id = dc.document.id
								studentList[dc.newIndex] = obj
							}
						}
						DocumentChange.Type.REMOVED -> {
							studentList.removeAt(dc.oldIndex)
						}
					}
				}
			}
			done.countDown()
		}
		done.await()
		return@withContext studentList
	}
	
	
	private suspend fun getAttendanceRecords(): ArrayList<DynamicLinkModel> = withContext(Dispatchers.IO) {
		val allLinks = ArrayList<DynamicLinkModel>()
		val done = CountDownLatch(1)
		
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className)
		
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.d(TAG, "getAttendanceRecords: ${error.localizedMessage}")
				return@addSnapshotListener
			}
			
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
						DocumentChange.Type.ADDED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<DynamicLinkModel>()
								obj.id = dc.document.id
								allLinks.add(dc.newIndex, obj)
							}
						}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								val obj = dc.document.toObject<DynamicLinkModel>()
								obj.id = dc.document.id
								allLinks[dc.newIndex] = obj
							}
						}
						DocumentChange.Type.REMOVED -> {
							allLinks.removeAt(dc.oldIndex)
						}
					}
				}
			}
			done.countDown()
		}
		done.await()
		return@withContext allLinks
	}
	
	
	private suspend fun getStudentWiseAttendanceRecords(index: Int, student: StudentAttendanceModel) = withContext(Dispatchers.IO) {
		for ((linkIndex, link) in allLinks.withIndex()) {
			val db =
				Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document(link.id.toString()).collection("attendance").document("attendance")
			db.addSnapshotListener { value, error ->
				if (error != null) {
					Log.d(TAG, "getStudentWiseAttendanceRecords: $error")
					return@addSnapshotListener
				}
				
				if (value != null) {
					val row = binding.exportTableLayout.getChildAt(index + 1) as TableRow
					if (value.getBoolean(student.rollNumber.toString()) == true) {
						val textView = row.getChildAt(linkIndex + 2) as MaterialTextView
						textView.text = "P"
						student.attendanceCount += 1
					} else {
						val textView = row.getChildAt(linkIndex + 2) as MaterialTextView
						textView.text = "A"
					}
					
					
					val attended = row.getChildAt(allLinks.size + 2) as MaterialTextView
					attended.text = student.attendanceCount.toString()
					
					val percentage = row.getChildAt(allLinks.size + 4) as MaterialTextView
					"${(student.attendanceCount.toFloat() / allLinks.size.toFloat()).times(100)} %".also { percentage.text = it }
					
				}
			}
		}
	}
	
	
	private fun addColumn(titles: ArrayList<String>) {
		val tableRow = TableRow(this)
		tableRow.layoutParams = tableRowParams
		for (title in titles) {
			val view = TeacherExportHeaderBinding.inflate(layoutInflater)
			view.columnTv.text = title
			tableRow.addView(view.root)
		}
		binding.exportTableLayout.addView(tableRow)
	}
	
	
	private fun addRow(row: ArrayList<String>) {
		val tableRow = TableRow(this)
		tableRow.layoutParams = tableRowParams
		for (text in row) {
			val view = TeacherExportRowBinding.inflate(layoutInflater)
			view.rowTv.text = text
			tableRow.addView(view.root)
		}
		binding.exportTableLayout.addView(tableRow)
	}
	
	
	private fun getOutputDirectory(): File {
		val mediaDir = externalMediaDirs.firstOrNull()?.let {
			File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
		}
		return if (mediaDir != null && mediaDir.exists())
			mediaDir else filesDir
	}
	
	
	private suspend fun exportToCSV() = withContext(Dispatchers.IO) {
		val data = ArrayList<ArrayList<String>>()
		with(binding.exportTableLayout) {
			for (i in 0 until childCount) {
				val row = getChildAt(i) as TableRow
				val rowData = ArrayList<String>()
				for (j in 0 until row.childCount) {
					val textView = row.getChildAt(j) as MaterialTextView
						rowData.add(textView.text.toString())
				}
				Log.d(TAG, "exportToCSV: $rowData")
				data.add(rowData)
			}
		}
		Log.d(TAG, "exportToCSV: $data")
		csvWriter().writeAll(data, "${getOutputDirectory()}/$className.xlsx")
		val file = Uri.parse("${getOutputDirectory()}/$className.xlx")
		
		csvReader().open(file.path!!) {
			Log.d(TAG, "exportToCSV: read: ${readNext()}")
		}
	}
	
	
}