package com.aditya.attendancesystem.teacher.adapters

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.aditya.attendancesystem.databinding.TeacherAdapterAttendanceLinksBinding
import com.aditya.attendancesystem.teacher.helperclasses.DisablerAlarm
import com.aditya.attendancesystem.teacher.helperclasses.DisablerWorkManager
import com.aditya.attendancesystem.teacher.helperclasses.DynamicLinkModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import java.util.*
import kotlin.collections.ArrayList

class AttendanceLinksAdapter(private val context: Context, private val attendanceEntries: ArrayList<DynamicLinkModel>, private val className: String, private val db: CollectionReference) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object {
		private const val TAG = "AttendanceLinksAdapter"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterAttendanceLinksBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		if(attendanceEntries[position].id == "students") {
			holder.binding.root.visibility = View.GONE
		}
		with(holder.binding) {
			linksDate.text = attendanceEntries[position].date
			linksTime.text = attendanceEntries[position].time
			linksLink.text = attendanceEntries[position].link
			linksActivationSwitch.isChecked = attendanceEntries[position].isActivated == true
			linksActivationSwitch.isEnabled = true
			linksShare.isEnabled = attendanceEntries[position].isActivated == true
			
			if(attendanceEntries[position].isActivated == true) {
				
				registerExpireService(db.document(attendanceEntries[position].id.toString()), attendanceEntries[position].date!!)
				
				val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
				val intent = Intent(context, DisablerAlarm::class.java)
				intent.putExtra("firestorePath", db.document(attendanceEntries[position].id.toString()).path)
				intent.putExtra("className", className)
				intent.putExtra("dateTime", "${attendanceEntries[position].date} \t ${attendanceEntries[position].time}")
				val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, Calendar.getInstance().timeInMillis + 300000, pendingIntent)
			}
			
			linksActivationSwitch.setOnCheckedChangeListener { _, _ ->
				toggleLinkActivation(position, linksActivationSwitch, linksShare)
			}
			
			linksShare.setOnClickListener {
				val text = "The attendance link for $className for date ${attendanceEntries[position].date} and time ${attendanceEntries[position].time} is ${attendanceEntries[position].link}"
				val sendIntent = Intent().apply {
					action = Intent.ACTION_SEND
					putExtra(Intent.EXTRA_TITLE, "Attendance for $className")
					putExtra(Intent.EXTRA_TEXT, text)
					flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
					type = "text/plain"
				}
				val shareIntent = Intent.createChooser(sendIntent, null)
				root.context.startActivity(shareIntent)
			}
		}
	}
	
	override fun getItemCount(): Int {
		return attendanceEntries.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterAttendanceLinksBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	private fun toggleLinkActivation(position: Int, switch: SwitchMaterial, linksShare: MaterialButton) {
		switch.isEnabled = false
		linksShare.isEnabled = false
		
		val db = db.document(attendanceEntries[position].id.toString())
		db.update("isActivated", !attendanceEntries[position].isActivated!!)
			.addOnSuccessListener {
//				switch.isEnabled = true
//				linksShare.isEnabled = dynamicEntries[position].isActivated == true
			}
			.addOnFailureListener {
				Log.e(TAG, "toggleLinkActivation: $it")
			}
	}
	
	
	private fun registerExpireService(db: DocumentReference, date: String) {
		val data = Data.Builder()
			.putString("title", className)
			.putString("description", date)
			.putInt("period", 5)
			.putString("link", db.path)
			.putString("date", date)
			.build()
		
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		
		val request = OneTimeWorkRequest.Builder(DisablerWorkManager::class.java)
			.setInputData(data)
			.setConstraints(constraints)
			.build()
		
		WorkManager.getInstance(context).enqueueUniqueWork(date, ExistingWorkPolicy.REPLACE, request)
		
		WorkManager.getInstance(context)
	}
	
}