package com.aditya.attendancesystem.teacher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.attendancesystem.databinding.ActivityTeacherCreateNewClassBinding
import com.aditya.attendancesystem.teacher.adapters.CreateClassAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext

class CreateNewClass : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "CreateNewClass"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	
	private lateinit var binding : ActivityTeacherCreateNewClassBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherCreateNewClassBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Create New Class"
		
		
		with(binding) {
			teacherCreateRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
			CoroutineScope(Dispatchers.IO).launch {
				val urls = getImageUrls()
				val adapter = CreateClassAdapter(this@CreateNewClass, urls, teacherCreateClassName, teacherCreateProgressLayout)
				runOnUiThread { teacherCreateRecyclerView.adapter = adapter }
			}
			
		}
	}
	
	
	private suspend fun getImageUrls(): ArrayList<String> = withContext(Dispatchers.IO) {
		val done = CountDownLatch(1)
		val urls = ArrayList<String>()
		val db = Firebase.firestore.collection("publicFiles").document("classBackgrounds")
		
		db.addSnapshotListener { value, error ->
			if(error!= null) {
				Log.e(TAG, "getImageUrls: ${error.localizedMessage}")
				return@addSnapshotListener
			}
			if (value != null) {
				value.data?.forEach {
					urls.add(it.value.toString())
				}
				done.countDown()
			}
			
		}
		done.await()
		return@withContext urls
	}
	
	
	override fun onStop() {
		super.onStop()
		job.cancel()
	}
	
	
}