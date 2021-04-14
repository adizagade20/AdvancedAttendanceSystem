package com.aditya.attendancesystem.teacher.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterVerificationPendingBinding
import com.aditya.attendancesystem.teacher.helperclasses.StudentDataVerification
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class VerificationPendingAdapter(private val context: Context, private val className: String, private val verifiedStudentsList: ArrayList<StudentDataVerification>) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object {
		private const val TAG = "VerificationPendingAda"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterVerificationPendingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder.binding) {
			verifyName.text = verifiedStudentsList[position].Name
			verifyEmail.text = verifiedStudentsList[position].Email
			verifyRollNumber.text = verifiedStudentsList[position].RollNumber.toString()
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
		TODO("Not yet implemented")
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterVerificationPendingBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	private fun verifyStudent(position: Int, switch: CompoundButton) {
		switch.isEnabled = false
		
		var db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students")
			.collection("verification_pending").document(verifiedStudentsList[position].id.toString())
		db.delete()
			.addOnSuccessListener {
				db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString()).collection(className).document("students")
					.collection("verified").document(verifiedStudentsList[position].id.toString())
				db.set(verifiedStudentsList[position])
					.addOnSuccessListener {
						Log.d(TAG, "verifyStudent: Successful")
					}
			}
		
	}
	
	
	private fun deleteRequest(position: Int) {
	
	}
	
	
}
