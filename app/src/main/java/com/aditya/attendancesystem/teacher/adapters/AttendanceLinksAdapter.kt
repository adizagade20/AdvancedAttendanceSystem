package com.aditya.attendancesystem.teacher.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterAttendanceLinksBinding
import com.aditya.attendancesystem.teacher.helperclasses.DynamicLinkModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.CollectionReference

class AttendanceLinksAdapter(private val dynamicEntries: ArrayList<DynamicLinkModel>, private val className: String, private val db: CollectionReference) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object {
		private const val TAG = "AttendanceLinksAdapter"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterAttendanceLinksBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		if(dynamicEntries[position].id == "students") {
			holder.binding.root.visibility = View.GONE
		}
		with(holder.binding) {
				linksDate.text = dynamicEntries[position].date
				linksTime.text = dynamicEntries[position].time
				linksLink.text = dynamicEntries[position].link
				linksActivationSwitch.isChecked = dynamicEntries[position].isActivated == true
				linksShare.isEnabled = dynamicEntries[position].isActivated == true
				
				linksActivationSwitch.setOnCheckedChangeListener { _, isChecked ->
					Log.d(TAG, "onBindViewHolder: $isChecked")
					toggleLinkActivation(position, linksActivationSwitch, linksShare)
				}
				
				linksShare.setOnClickListener {
					val text = "The attendance link for $className for date ${dynamicEntries[position].date} and time ${dynamicEntries[position].time} is ${dynamicEntries[position].link}"
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
		return dynamicEntries.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterAttendanceLinksBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	private fun toggleLinkActivation(position: Int, switch: SwitchMaterial, linksShare: MaterialButton) {
		switch.isEnabled = false
		linksShare.isEnabled = false
		
		val db = db.document(dynamicEntries[position].id.toString())
		db.update("isActivated", !dynamicEntries[position].isActivated!!)
			.addOnSuccessListener {
				switch.isEnabled = true
				dynamicEntries[position].isActivated = !dynamicEntries[position].isActivated!!
				
				linksShare.isEnabled = dynamicEntries[position].isActivated == true
				
			}
			.addOnFailureListener {
				Log.e(TAG, "toggleLinkActivation: $it")
			}
	}
	
}