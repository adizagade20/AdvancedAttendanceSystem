package com.aditya.attendancesystem.root

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityRegisterBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

class Register : AppCompatActivity() {
	
	companion object {
		private const val TAG = "MainActivity"
	}
	
	
	private lateinit var binding: ActivityRegisterBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityRegisterBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Register"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		with(binding) {
			
			registerRegister.setOnClickListener {
				root.performClick()
				verify()
			}
			
			registerLogin.setOnClickListener {
				finish()
			}
			
			root.setOnClickListener {
				val inputMethodManager: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
				if (inputMethodManager.isAcceptingText) {
					inputMethodManager.hideSoftInputFromWindow(currentFocus?.getWindowToken(), 0)
				}
			}
			
			registerRole.setOnCheckedChangeListener { _, checkedId ->
				if (checkedId == R.id.register_role_teacher) {
					binding.registerRollNo.visibility = View.INVISIBLE
				} else if (checkedId == R.id.register_role_student) {
					binding.registerRollNo.visibility = View.VISIBLE
				}
			}
		}
	}
	
	
	private fun verify() {
		with(binding) {
			if (registerName.editText?.text.toString() == "") {
				registerName.error = "Can not be empty"
				return@with
			} else {
				registerName.isErrorEnabled = false
			}
			
			if (registerEmail.editText?.text.toString() == "") {
				registerEmail.error = "Can not be empty"
				return@with
			} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(registerEmail.editText?.text.toString()).matches()) {
				registerEmail.error = "Email not valid"
				return@with
			} else if (android.util.Patterns.EMAIL_ADDRESS.matcher(registerEmail.editText?.text.toString()).matches()) {
				registerEmail.isErrorEnabled = false
			}
			
			if (registerPhone.editText?.text.toString() == "") {
				registerPhone.error = "Can not be empty"
				return@with
			} else if (!android.util.Patterns.PHONE.matcher(registerPhone.editText?.text.toString()).matches()) {
				registerPhone.error = "Mobile number not valid"
				return@with
			} else if (android.util.Patterns.PHONE.matcher(registerPhone.editText?.text.toString()).matches()) {
				registerPhone.isErrorEnabled = false
			}
			
			if (registerPassword.editText?.text.toString() == "") {
				registerPassword.error = "Can not be empty"
				return@with
			} else if (registerPassword.editText?.text.toString().length < 6) {
				registerPassword.error = "Password must be minimum 6 characters"
				return@with
			} else {
				registerPassword.isErrorEnabled = false
			}
			
			when {
				registerRoleStudent.isChecked -> {
					if (registerRollNo.editText?.text.toString() == "") {
						registerRollNo.error = "Can not be empty"
						return@with
					} else {
						registerRollNo.isErrorEnabled = false
					}
				}
				registerRoleTeacher.isChecked -> {
				
				}
			}
			registerRegister.startLoading()
			createUser()
			
		}
		
	}

	
	private fun createUser() {
		val auth = Firebase.auth
		
		auth.createUserWithEmailAndPassword(binding.registerEmail.editText?.text.toString(), binding.registerPassword.editText?.text.toString())
			.addOnSuccessListener { itAuth ->
				val db = Firebase.firestore.collection(if (binding.registerRoleStudent.isChecked) "students" else "teachers").document("${itAuth.user?.uid}")
				val userData = HashMap<String, Any>()
				with(binding) {
					userData["name"] = registerName.editText?.text.toString()
					userData["phone"] = registerPhone.editText?.text.toString()
					userData["email"] = registerEmail.editText?.text.toString()
					userData["password"] = registerPassword.editText?.text.toString()
					userData["role"] = if (binding.registerRoleStudent.isChecked) "Student" else "Teacher"
					if (registerRollNo.editText?.text.toString() != "")
						userData["rollNumber"] = registerRollNo.editText?.text.toString()
				}
				writeToSharedPreferences()
				db.set(userData)
					.addOnSuccessListener {
						binding.registerRegister.loadingSuccessful()
						if (itAuth.additionalUserInfo?.isNewUser == true) {
							AlertDialog.Builder(this)
								.setCancelable(false)
								.setTitle("Welcome")
								.setMessage("Hi ${itAuth.user?.displayName}, Welcome to the \"Attendance System\"")
								.setPositiveButton("Continue") { dialog, _ ->
									intentFunction()
									dialog.dismiss()
								}
								.create()
								.show()
						} else {
							intentFunction()
						}
					}
			}
			.addOnFailureListener {
				binding.registerRegister.loadingFailed()
				Toast.makeText(applicationContext, "Authentication failed, try after some time", Toast.LENGTH_LONG).show()
			}
	}
	
	
	private fun writeToSharedPreferences() {
		with(binding) {
			val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
			val editor = sharedPreferences.edit()
			editor.putString("name", registerName.editText?.text.toString())
			editor.putString("phone", registerPhone.editText?.text.toString())
			editor.putString("email", registerEmail.editText?.text.toString())
			editor.putString("role", if (binding.registerRoleStudent.isChecked) "Student" else "Teacher")
			editor.putString("rollNumber", registerRollNo.editText?.text.toString())
			editor.apply()
		}
	}
	
	
	private fun intentFunction() {
		if (binding.registerRoleStudent.isChecked) {
			startActivity(Intent(this, com.aditya.attendancesystem.student.Home::class.java))
			finish()
		} else {
			startActivity(Intent(this, com.aditya.attendancesystem.teacher.Home::class.java))
			finish()
		}
	}
	
}