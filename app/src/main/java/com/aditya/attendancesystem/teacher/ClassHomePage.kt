package com.aditya.attendancesystem.teacher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherClassHomePageBinding
import com.aditya.attendancesystem.teacher.adapters.AttendanceLinksAdapter
import com.aditya.attendancesystem.teacher.helperclasses.AttendanceLinkDataClass
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs


class ClassHomePage : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "ClassHomePage"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private lateinit var className: String
	private lateinit var classImage: String
	
	private lateinit var listener: ListenerRegistration
	
	
	private lateinit var binding: ActivityTeacherClassHomePageBinding
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherClassHomePageBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		className = getSharedPreferences("classDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("classImages", MODE_PRIVATE).getString(className, "").toString()
		
		setSupportActionBar(binding.classHomePageToolbar)
		supportActionBar?.title = className
		binding.classHomePageCollapsingToolbar.title = className
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		binding.classHomePageAppbarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
			if (abs(verticalOffset) == binding.classHomePageAppbarLayout.totalScrollRange) {
				binding.classHomePageImageView.visibility = View.GONE
				binding.classHomePageCircleMenu.visibility = View.GONE
			} else if (verticalOffset == 0) {
				binding.classHomePageImageView.visibility = View.VISIBLE
				binding.classHomePageCircleMenu.visibility = View.VISIBLE
			} else {
				binding.classHomePageImageView.visibility = View.VISIBLE
				binding.classHomePageCircleMenu.visibility = View.VISIBLE
			}
		})
		
		Picasso.get()
			.load(Uri.parse(classImage))
			.resize(300, 180)
			.into(binding.classHomePageImageView)
		
		CoroutineScope(Dispatchers.IO).launch {
			getAllAttendances()
		}
		
		binding.classHomePageFloating.setOnClickListener {
			val intent = Intent(this, GenerateLink::class.java)
			intent.putExtra("className", className)
			startActivity(intent)
		}
		
		circleMenuConfigure()
		
	}
	
	
	
	private fun circleMenuConfigure() {
		with(binding.classHomePageCircleMenu) {
			
			// #88bef5 -> #ecfffb
			// #83bef5 -> #96f7d2
			// #ff4b32 -> #fac4a2
			// #ba53de -> #d3cde6
			// #ff8a5c -> #fff591
			// binding.root.setBackgroundColor(Color.parseColor("#ecfffb"))
			
			setMainMenu(ResourcesCompat.getColor(resources, R.color.teal_200, null), R.drawable.ic_menu, R.drawable.ic_cancel)
			addSubMenu(Color.parseColor("#83e85a"), R.drawable.ic_circular_verification_pending)
			addSubMenu(Color.parseColor("#ff4b32"), R.drawable.ic_circular_delete_forever)
			addSubMenu(Color.parseColor("#ff8a5c"), R.drawable.ic_circular_export)
			addSubMenu(Color.parseColor("#88bef5"), R.drawable.ic_circular_verification_done)
			setOnMenuSelectedListener {
				when(it) {
					0 -> {
						
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
	
	
	private suspend fun getAllAttendances() = withContext(Dispatchers.IO) {
		binding.classHomePageExtendedInclude.classHomePageExtendedRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
		val attendanceEntries = ArrayList<AttendanceLinkDataClass>()
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className)
		val adapter = AttendanceLinksAdapter(attendanceEntries, className, db)
		listener = db.addSnapshotListener { value, error ->
			if (error != null) {
				Log.e(TAG, "getAllAttendances: ${error.localizedMessage}")
				return@addSnapshotListener
			}
			if (value != null) {
				for (dc in value.documentChanges) {
					when (dc.type) {
							DocumentChange.Type.ADDED -> {
								if (dc.document.exists()) {
									val obj = dc.document.toObject<AttendanceLinkDataClass>()
									obj.id = dc.document.id
									attendanceEntries.add(dc.newIndex, obj)
									adapter.notifyItemInserted(dc.newIndex)
								}
							}
						DocumentChange.Type.MODIFIED -> {
							if(dc.document.exists()) {
								val obj = dc.document.toObject<AttendanceLinkDataClass>()
								obj.id = dc.document.id
								attendanceEntries[dc.newIndex] = obj
								adapter.notifyItemChanged(dc.newIndex)
							}
						}
						DocumentChange.Type.REMOVED -> {
							attendanceEntries.removeAt(dc.oldIndex)
							adapter.notifyItemRemoved(dc.oldIndex)
						}
					}
				}
			}
			binding.classHomePageExtendedInclude.classHomePageExtendedRecyclerView.adapter = adapter
		}
		Timer().schedule(1000) {
			runOnUiThread {
				listener.remove()
			}
		}
	}
	
	
}