package com.aditya.attendancesystem.root

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aditya.attendancesystem.databinding.ActivityLoginBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
	
	companion object {
		private const val TAG = "MainActivity"
	}
	
	
	private lateinit var binding: ActivityLoginBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityLoginBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Login"
		
		with(binding) {
			
			loginRegister.setOnClickListener {
				startActivity(Intent(this@Login, Register::class.java))
			}
			
			loginLogin.setOnClickListener {
				verify()
			}
			
		}
	}
	
	
	override fun onStart() {
		super.onStart()
		val user = Firebase.auth.currentUser
		if(user != null) {
			val role = getSharedPreferences("UserData", MODE_PRIVATE).getString("Role", null)
			if(role == "Student") {
				startActivity(Intent(this, com.aditya.attendancesystem.student.Home::class.java))
				finish()
			}
			else if(role == "Teacher") {
				startActivity(Intent(this, com.aditya.attendancesystem.teacher.Home::class.java))
				finish()
			}
		}
	}
	
	
	private fun verify() {
		var flag = true
		with(binding) {
			if (loginEmail.editText?.text.toString() == "") {
				loginEmail.error = "Can not be empty"
			} else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail.editText?.text.toString()).matches()) {
				loginEmail.error = "Email not valid"
				flag = false
			} else if(android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail.editText?.text.toString()).matches()) {
				loginEmail.isErrorEnabled = false
			}
			
			if (loginPassword.editText?.text.toString() == "") {
				loginPassword.error = "Can not be empty"
				flag = false
			} else if(loginPassword.editText?.text.toString().length < 6) {
				loginPassword.error = "Password must be minimum 6 characters"
			} else {
				loginPassword.isErrorEnabled = false
			}
			
			if(!flag)
				return@with
			
			else if(flag) {
				loginUser()
			}
			
		}
	}
	
	
	private fun loginUser() {
		val auth = Firebase.auth
		auth.signInWithEmailAndPassword(binding.loginEmail.editText?.text.toString(), binding.loginPassword.editText?.text.toString())
			.addOnSuccessListener {
				val user = auth.currentUser!!
				
				var db = Firebase.firestore.collection("students").document(user.uid)
				db.get()
					.addOnSuccessListener {
						if (it != null) {
							writeToSharedPreferences(it)
							startActivity(Intent(this, com.aditya.attendancesystem.student.Home::class.java))
							finish()
							return@addOnSuccessListener
						}
					}
				
				db = Firebase.firestore.collection("teachers").document(user.uid)
				db.get()
					.addOnSuccessListener {
						if (it != null) {
							writeToSharedPreferences(it)
							startActivity(Intent(this, com.aditya.attendancesystem.teacher.Home::class.java))
							finish()
							return@addOnSuccessListener
						}
					}
			}
			.addOnFailureListener {
				Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_LONG).show()
			}
	}
	
	
	private fun writeToSharedPreferences(it : DocumentSnapshot) {
		val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putString("Name", it["Name"].toString())
		editor.putString("Phone", it["Phone"].toString())
		editor.putString("Email", it["Email"].toString())
		editor.putString("Role", it["Role"].toString())
		editor.putString("RollNumber", it["RollNumber"].toString())
		editor.commit()
	}
	
}