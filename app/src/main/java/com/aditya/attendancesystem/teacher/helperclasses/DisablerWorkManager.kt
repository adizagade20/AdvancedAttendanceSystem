package com.aditya.attendancesystem.teacher.helperclasses

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aditya.attendancesystem.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class DisablerWorkManager(private val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters), CoroutineScope {
	
	companion object {
		private const val TAG = "AttendanceDisablerWork"
	}
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private lateinit var data: Data
	private lateinit var notification: NotificationCompat.Builder
	private lateinit var notificationManager: NotificationManager
	private var notificationId = Random().nextInt(100)
	private var time = 0
	private var maxTime: Int = 0
	
	private val notificationChannelId = "CHANNEL2"
	
	override fun doWork(): Result {
		data = inputData
		Log.d("TAG", "doWork: ${data.keyValueMap}")
		maxTime = data.getInt("period", 10) * 60
		displayNotification(data.getString("title").toString(), data.getString("description").toString())
		CoroutineScope(Dispatchers.Main).launch {
			loop()
		}
		val outputData = Data.Builder().putString("result", "OK").build()
		notificationManager.cancelAll()
		return Result.success(outputData)
	}
	
	
	private suspend fun loop() {
		delay(1000)
		time += 1
		notification.setProgress(maxTime, time, false)
		notificationManager.notify(notificationId, notification.build())
		if (time < maxTime) {
			loop()
		} else {
			notification.setProgress(0, 0, true)
			disableLink()
		}
	}
	
	
	private fun displayNotification(title: String, description: String) {
		notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notification = NotificationCompat.Builder(applicationContext, "CHANNEL2")
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
			.setContentTitle(title)
			.setContentText(description)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.setOnlyAlertOnce(true)
			.setProgress(maxTime, 0, false)
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				notificationChannelId,
				"Product Uploads Notification",
				NotificationManager.IMPORTANCE_DEFAULT
			)
			notificationManager.createNotificationChannel(channel)
			notification.setChannelId(notificationChannelId)
		}
		notificationManager.notify(notificationId, notification.build())
	}
	
	
	private fun disableLink() {
		val link = data.getString("link")
		if (link != null) {
			val db = Firebase.firestore.document(link)
			db.update("isActivated", false)
				.addOnSuccessListener {
					notification.setContentTitle("Link deactivated successfully")
					notification.setContentText("${data.getString("title")} : ${data.getString("date")}")
					notification.setProgress(0, 0, false)
					notificationManager.notify(notificationId, notification.build())
					Log.d("TAG", "disableLink: ${db.path}")
				}
				.addOnFailureListener {
					notification.setContentTitle("Failed : ${data.getString("title")} : ${data.getString("date")}")
					notification.setContentText("We are not able to deactivate the link automatically, please disable in manually.")
				}
		}
	}
	
	
	override fun onStopped() {
		super.onStopped()
		Log.d(TAG, "onStopped: ")
		notificationManager.cancel(notificationId)
	}
	
	
}
		
		
		
		/*val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			val channel = NotificationChannel("Attendance_System", "Attendance_System", NotificationManager.IMPORTANCE_DEFAULT)
			manager.createNotificationChannel(channel)
		} else {

		}

		val builder = NotificationCompat.Builder(applicationContext, "Attendance_System")
			.setContentTitle(title)
			.setContentText(description)
			.setSmallIcon(R.mipmap.ic_launcher)
		manager.notify(1, builder.build())*/
