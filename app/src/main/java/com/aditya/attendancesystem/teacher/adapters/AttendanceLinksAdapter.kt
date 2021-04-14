package com.aditya.attendancesystem.teacher.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterAttendanceLinksBinding
import com.aditya.attendancesystem.teacher.helperclasses.AttendanceLinkDataClass
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AttendanceLinksAdapter(private val attendanceEntries: ArrayList<AttendanceLinkDataClass>, private val className: String, private val db: CollectionReference) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
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
				"${attendanceEntries[position].count} present".also { linksAttendance.text = it }
				linksShare.isEnabled = attendanceEntries[position].isActivated == true
				
				linksActivationSwitch.setOnCheckedChangeListener { _, isChecked ->
					Log.d(TAG, "onBindViewHolder: $isChecked")
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
				switch.isEnabled = true
				attendanceEntries[position].isActivated = !attendanceEntries[position].isActivated!!
				
				linksShare.isEnabled = attendanceEntries[position].isActivated == true
				
			}
			.addOnFailureListener {
				Log.e(TAG, "toggleLinkActivation: $it")
				switch.performClick()
				switch.isEnabled = true
			}
	}
	
}