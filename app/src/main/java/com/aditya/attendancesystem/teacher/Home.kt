package com.aditya.attendancesystem.teacher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.databinding.ActivityTeacherHomeBinding
import com.aditya.attendancesystem.teacher.adapters.ClassesAdapter
import com.aditya.attendancesystem.teacher.helperclasses.ClassNameImageModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class Home : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "TeacherHome"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private lateinit var classesListener: ListenerRegistration
	
	
	private lateinit var binding: ActivityTeacherHomeBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Classes"
		
		CoroutineScope(Dispatchers.IO).launch {
			getClassesList()
		}
		
		binding.teacherHomeFloatingButton.setOnClickListener {
			startActivity(Intent(applicationContext, CreateNewClass::class.java))
		}
	}
	
	
	private suspend fun getClassesList() = withContext(Dispatchers.IO) {
		classesListener = Firebase.firestore.collection("attendance").document("${Firebase.auth.currentUser?.uid}")
			.addSnapshotListener { value, error ->
				if (error != null) {
					Toast.makeText(applicationContext, error.localizedMessage, Toast.LENGTH_LONG).show()
					return@addSnapshotListener
				}
				
				val classes = ArrayList<ClassNameImageModel>()
				
				if (value != null) {
					val editor = getSharedPreferences("ClassImages", MODE_PRIVATE).edit()
					value.data?.forEach {
						classes.add(ClassNameImageModel(it.key, it.value.toString()))
						editor.putString(it.key, it.value.toString())
					}
					editor.apply()
				} else {
					binding.teacherHomeNoClasses.visibility = View.VISIBLE
				}
				
				if (classes.size != 0) {
					binding.teacherHomeRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
					binding.teacherHomeRecyclerView.adapter = ClassesAdapter(classes)
					binding.teacherHomeRecyclerView.adapter?.notifyItemChanged(0)
					
					if (intent.getBooleanExtra("isDeepLink", false)) {
						val sharedPreferences = getSharedPreferences("DynamicLink", MODE_PRIVATE)
						val className = sharedPreferences.getString("className", "")
						sharedPreferences.edit().clear().apply()
						
						val classList = ArrayList<String>()
						classes.forEach {
							classList.add(it.className.toString())
						}
						val position = classList.indexOf(className)
						
						binding.teacherHomeRecyclerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
							override fun onPreDraw(): Boolean {
								binding.teacherHomeRecyclerView.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
								binding.teacherHomeRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
								return true
							}
						})
					}
				}
			}
	}
	
	
	override fun onBackPressed() {
		super.onBackPressed()
		finish()
		finishActivity(0)
	}
	
	
	override fun onStop() {
		super.onStop()
		try {
			classesListener.remove()
			job.cancel()
		} catch (e: Exception) {}
	}
	
}