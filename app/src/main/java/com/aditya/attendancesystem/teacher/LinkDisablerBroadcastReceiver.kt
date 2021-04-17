package com.aditya.attendancesystem.teacher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.aditya.attendancesystem.teacher.helperclasses.AttendanceDisablerWorker

class LinkDisablerBroadcastReceiver: BroadcastReceiver() {
	
	companion object {
		private const val TAG = "LinkDisabler"
	}
	
	override fun onReceive(context: Context?, intent: Intent?) {
		Log.d(TAG, "onReceive: ")
		
		val data = Data.Builder()
			.putString("title", "Work Data Sending")
			.putString("description", "Work Data Sending")
			.build()
		
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		
		val request = OneTimeWorkRequest.Builder(AttendanceDisablerWorker::class.java)
			.setInputData(data)
			.setConstraints(constraints)
			.build()
		
		if (context != null) {
			WorkManager.getInstance(context).enqueue(request)
		}
	}
	
	
}
