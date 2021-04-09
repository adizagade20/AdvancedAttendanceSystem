package com.aditya.attendancesystem.teacher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherGenerateLinkBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class GenerateLink : AppCompatActivity() {
	
	companion object {
		private const val TAG = "TeacherHome"
	}
	
	private val lectureTime = HashMap<String, Int>()
	private lateinit var lectureDate : String
	private var timestamp: Long = 0
	
	private lateinit var binding: ActivityTeacherGenerateLinkBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherGenerateLinkBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Generate Attendance Link"
		
		with(binding) {
			
			val type = arrayOf("1 minute", "2 minutes", "3 minutes", "4 minutes", "5 minutes", "6 minutes", "7 minutes", "8 minutes", "9 minutes", "10 minutes", "12 minutes", "15 minutes", "20 minutes")
			val adapter = ArrayAdapter(applicationContext, R.layout.teacher_link_expire, type)
			generateLinkExpireDropdown.setAdapter(adapter)
			
			
			generateLectureDate.editText?.setOnClickListener {
				val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Lecture Date").build()
				datePicker.show(supportFragmentManager, null)
				
				datePicker.addOnPositiveButtonClickListener {
					timestamp = datePicker.selection!!
					"Date : \t\t ${datePicker.headerText}".also { generateLectureDate.hint = it }
					lectureDate = datePicker.headerText
				}
			}
			
			
			generateLectureStart.editText?.setOnClickListener {
				val currentTime = Calendar.getInstance()
				val hour = currentTime.get(Calendar.HOUR_OF_DAY)
				val minute = currentTime.get(Calendar.MINUTE)
				val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(hour - 1).setMinute(minute).setTitleText("Lecture Start Time").build()
				timePicker.show(supportFragmentManager, null)
				timePicker.addOnPositiveButtonClickListener {
					"Lecture Start TIme : \t\t ${timePicker.hour} : ${timePicker.minute}".also { generateLectureStart.hint = it }
					lectureTime["startHour"] = timePicker.hour
					lectureTime["startMinute"] = timePicker.minute
				}
			}
			
			
			generateLectureEnd.editText?.setOnClickListener {
				val currentTime = Calendar.getInstance()
				val hour = currentTime.get(Calendar.HOUR_OF_DAY)
				val minute = currentTime.get(Calendar.MINUTE)
				val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(hour).setMinute(minute).setTitleText("Lecture End Time").build()
				timePicker.show(supportFragmentManager, null)
				timePicker.addOnPositiveButtonClickListener {
					"Lecture End Time : \t\t ${timePicker.hour} : ${timePicker.minute}".also { generateLectureEnd.hint = it }
					lectureTime["endHour"] = timePicker.hour
					lectureTime["endMinute"] = timePicker.minute
				}
			}
			
			
		}
		
	}
}