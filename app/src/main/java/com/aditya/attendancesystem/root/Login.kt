package com.aditya.attendancesystem.root

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aditya.attendancesystem.databinding.ActivityLoginBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


@SuppressLint("ApplySharedPref")
class Login : AppCompatActivity() {
	
	companion object {
		private const val TAG = "Login"
	}
	
	private var isDeepLink = false
	
	
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
		getSharedPreferences("classDetails", MODE_PRIVATE).edit().clear().commit()
		
		Firebase.dynamicLinks.getDynamicLink(intent)
			.addOnSuccessListener { pendingDynamicLinkData ->
				val deepLink: Uri
				if (pendingDynamicLinkData != null) {
					isDeepLink = true
					deepLink = pendingDynamicLinkData.link!!
					Log.d(TAG, "onStart: $deepLink")
					getSharedPreferences("dynamicLink", MODE_PRIVATE).edit().apply {
						putString("privateKey", deepLink.getQueryParameter("privateKey"))
						putString("className", deepLink.getQueryParameter("className"))
						putString("date", deepLink.getQueryParameter("date"))
						commit()
					}
				}
				
				val user = Firebase.auth.currentUser
				if (user != null) {
					val role = getSharedPreferences("UserData", MODE_PRIVATE).getString("Role", null)
					if(role == "Student") {
						val intent = Intent(this, com.aditya.attendancesystem.student.RecordAttendance::class.java)
						intent.putExtra("isDeepLink", isDeepLink)
						startActivity(intent)
						finish()
					}
					else if(role == "Teacher") {
						val intent = Intent(this, com.aditya.attendancesystem.teacher.Home::class.java)
						intent.putExtra("isDeepLink", isDeepLink)
						startActivity(intent)
						finish()
					}
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
						Log.d(TAG, "loginUser: $it")
						if (it["Role"] == "Student") {
							writeToSharedPreferences(it)
							val intent = Intent(this, com.aditya.attendancesystem.student.RecordAttendance::class.java)
							intent.putExtra("isDeepLink", isDeepLink)
							startActivity(intent)
							finish()
						}
					}
				
				db = Firebase.firestore.collection("teachers").document(user.uid)
				db.get()
					.addOnSuccessListener {
						Log.d(TAG, "loginUser: $it")
						if (it["Role"] == "Teacher") {
							writeToSharedPreferences(it)
							val intent = Intent(this, com.aditya.attendancesystem.teacher.Home::class.java)
							intent.putExtra("isDeepLink", isDeepLink)
							startActivity(intent)
							finish()
						}
					}
			}
			.addOnFailureListener {
				Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_LONG).show()
				Log.d(TAG, "loginUser: ${it.localizedMessage}")
			}
	}
	
	
	private fun writeToSharedPreferences(it : DocumentSnapshot) {
		getSharedPreferences("UserData", MODE_PRIVATE).edit().apply {
			putString("Name", it["Name"].toString())
			putString("Phone", it["Phone"].toString())
			putString("Email", it["Email"].toString())
			putString("Role", it["Role"].toString())
			putString("RollNumber", it["RollNumber"].toString())
			commit()
		}
	}
	
	
}