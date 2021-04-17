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
import com.aditya.attendancesystem.teacher.helperclasses.DynamicLinkModel
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
		
		className = getSharedPreferences("ClassDetails", MODE_PRIVATE).getString("className", "").toString()
		classImage = getSharedPreferences("ClassImages", MODE_PRIVATE).getString(className, "").toString()
		
		setSupportActionBar(binding.classHomePageToolbar)
		supportActionBar?.title = className
		binding.classHomePageCollapsingToolbar.title = className
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		binding.classHomePageAppbarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
			if (abs(verticalOffset) == binding.classHomePageAppbarLayout.totalScrollRange) {
				binding.classHomePageImageView.visibility = View.GONE
				binding.classHomePageCircleMenu.visibility = View.GONE
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
			
			setMainMenu(
				ResourcesCompat.getColor(resources, R.color.teal_200, null),
				ResourcesCompat.getDrawable(resources, R.drawable.ic_menu, null),
				ResourcesCompat.getDrawable(resources, R.drawable.ic_cancel, null)
			)
			addSubMenu(Color.parseColor("#ba53de"), ResourcesCompat.getDrawable(resources, R.drawable.ic_generate, null))
			addSubMenu(Color.parseColor("#ff4b32"),ResourcesCompat.getDrawable(resources,  R.drawable.ic_circular_delete_forever, null))
			addSubMenu(Color.parseColor("#ff8a5c"), ResourcesCompat.getDrawable(resources, R.drawable.ic_circular_export, null))
			addSubMenu(Color.parseColor("#88bef5"), ResourcesCompat.getDrawable(resources, R.drawable.ic_circular_verification_done, null))
			addSubMenu(Color.parseColor("#83e85a"), ResourcesCompat.getDrawable(resources, R.drawable.ic_circular_verification_pending, null))
			
			setOnMenuSelectedListener {
				Timer().schedule(1000) {
					runOnUiThread {
						when (it) {
							0 -> {
								val intent = Intent(this@ClassHomePage, GenerateLink::class.java)
								intent.putExtra("className", className)
								startActivity(intent)
							}
							1 -> {}
							2 -> {
								startActivity(Intent(this@ClassHomePage, ExportAsCSV::class.java))
							}
							3 -> startActivity(Intent(this@ClassHomePage, StudentList::class.java))
							4 ->  startActivity(Intent(this@ClassHomePage, VerificationPending::class.java))
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
	
	
	private suspend fun getAllAttendances() = withContext(Dispatchers.IO) {
		binding.classHomePageExtendedRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
		val attendanceEntries = ArrayList<DynamicLinkModel>()
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className)
		val adapter = AttendanceLinksAdapter(applicationContext, attendanceEntries, className, db)
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
									val obj = dc.document.toObject<DynamicLinkModel>()
									obj.id = dc.document.id
									attendanceEntries.add(dc.newIndex, obj)
									adapter.notifyItemInserted(dc.newIndex)
								}
							}
						DocumentChange.Type.MODIFIED -> {
							if (dc.document.exists()) {
								if (!dc.document.metadata.isFromCache) {
									val obj = dc.document.toObject<DynamicLinkModel>()
									obj.id = dc.document.id
									attendanceEntries[dc.newIndex] = obj
									adapter.notifyItemChanged(dc.newIndex)
								}
							}
						}
						DocumentChange.Type.REMOVED -> {
							attendanceEntries.removeAt(dc.oldIndex)
							adapter.notifyItemRemoved(dc.oldIndex)
						}
					}
				}
			}
			binding.classHomePageExtendedRecyclerView.adapter = adapter
			
		}
	}
	
	
}