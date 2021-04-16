package com.aditya.attendancesystem.teacher.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterVerificationPendingBinding
import com.aditya.attendancesystem.teacher.helperclasses.StudentDataModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class VerificationPendingAdapter(private val className: String, private val studentsList: ArrayList<StudentDataModel>) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object {
		private const val TAG = "VerificationPendingAda"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterVerificationPendingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		Log.d(TAG, "onBindViewHolder: $studentsList")
		with(holder.binding) {
			verifyName.text = studentsList[position].name
			verifyEmail.text = studentsList[position].email
			verifyRollNumber.text = studentsList[position].rollNumber.toString()
			verifyAcceptSwitch.isChecked = false
			
			verifyAcceptSwitch.setOnCheckedChangeListener { buttonView, _ ->
				verifyStudent(position, buttonView)
			}
			
			verifyDelete.setOnClickListener {
				deleteRequest(position)
			}
			
		}
	}
	
	
	override fun getItemCount(): Int {
		return studentsList.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterVerificationPendingBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	private fun verifyStudent(position: Int, switch: CompoundButton) {
		switch.isEnabled = false
		
		var db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students")
			.collection("verification_pending").document(studentsList[position].id.toString())
		db.delete()
			.addOnSuccessListener {
				db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students")
					.collection("verified").document(studentsList[position].id.toString())
				db.set(studentsList[position])
					.addOnSuccessListener {
						studentsList.removeAt(position)
						notifyItemRemoved(position)
						switch.isEnabled = true
					}
			}
		
	}
	
	
	private fun deleteRequest(position: Int) {
		var db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students")
			.collection("verification_pending").document(studentsList[position].id.toString())
		db.delete()
			.addOnSuccessListener {
				studentsList.removeAt(position)
				notifyItemRemoved(position)
			}
	}
	
	
}
