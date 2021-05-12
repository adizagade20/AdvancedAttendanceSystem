package com.aditya.attendancesystem.teacher.helperclasses

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aditya.attendancesystem.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class DisablerAlarm : BroadcastReceiver() {
	private lateinit var mContext :Context
	
	private lateinit var notification: NotificationCompat.Builder
	private lateinit var notificationManager: NotificationManager
	private val notificationId = Random.nextInt(100)
	
	override fun onReceive(context: Context?, intent: Intent?) {
		if (context != null) {
			mContext = context
			
			val path = intent?.getStringExtra("firestorePath")
			val className = intent?.getStringExtra("className")
			val dateTime = intent?.getStringExtra("dateTime")
			
			if (className != null && dateTime != null) {
				displayNotification(className, dateTime)
			}
			disableLink(path, className, dateTime)
		}
	
	}
	
	
	private fun disableLink(path: String?, className: String?, dateTime: String?) {
		if (path != null) {
			val db = Firebase.firestore.document(path)
			db.update("isActivated", false)
				.addOnSuccessListener {
					notification.setContentTitle("Link deactivated successfully")
					notification.setContentText("$className : $dateTime")
					notification.setProgress(0, 0, false)
					notificationManager.notify(notificationId, notification.build())
				}
		}
	}
	
	
	private fun displayNotification(title: String, description: String) {
		notificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notification = NotificationCompat.Builder(mContext, "CHANNEL2")
			.setSmallIcon(R.drawable.ic_check_circle)
			.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
			.setContentTitle(title)
			.setContentText(description)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.setOnlyAlertOnce(true)
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				"Advanced Attendance Disabler",
				"Advanced Attendance Disabler",
				NotificationManager.IMPORTANCE_DEFAULT
			)
			notificationManager.createNotificationChannel(channel)
			notification.setChannelId("Advanced Attendance Disabler")
		}
		notificationManager.notify(notificationId, notification.build())
	}
	
	
}