package com.aditya.attendancesystem.teacher.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterStudentListBinding
import com.aditya.attendancesystem.teacher.helperclasses.StudentAttendanceModel
import java.util.*

class StudentListAdapter(private val className: String, private val verifiedStudentsList: ArrayList<StudentAttendanceModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object {
		private const val TAG = "StudentListAdapter"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterStudentListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder.binding) {
			"${position.plus(1)} : ".also { listSrNo.text = it }
			listName.text = verifiedStudentsList[position].name
			listRollNumber.text = verifiedStudentsList[position].rollNumber
			listEmail.text = verifiedStudentsList[position].email
			if(verifiedStudentsList[position].attendanceCount == 1) {
				"${verifiedStudentsList[position].attendanceCount} lecture".also { listAttendance.text = it }
			} else {
				"${verifiedStudentsList[position].attendanceCount} lectures".also { listAttendance.text = it }
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return verifiedStudentsList.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterStudentListBinding) : RecyclerView.ViewHolder(binding.root)
}
