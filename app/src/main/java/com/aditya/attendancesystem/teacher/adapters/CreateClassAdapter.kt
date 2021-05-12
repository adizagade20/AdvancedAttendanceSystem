package com.aditya.attendancesystem.teacher.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterCreateClassBinding
import com.aditya.attendancesystem.teacher.CreateNewClass
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class CreateClassAdapter(private val context: Context, private val imageUrls: ArrayList<String>, private val teacherCreateClassName: TextInputLayout, private val teacherCreateProgressLayout: ConstraintLayout): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object{
		private const val TAG = "ClassesAdapter"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterCreateClassBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder.binding) {
			Picasso.get()
				.load(imageUrls[position])
				.resize(0, 150)
				.into(teacherAdapterCreateClassImageView, object : Callback {
					override fun onSuccess() {
						teacherAdapterCreateClassProgress.visibility = View.GONE
					}
					
					override fun onError(e: Exception?) {
						root.visibility = View.GONE
					}
				})
			
			root.setOnClickListener {
				val className = teacherCreateClassName.editText?.text.toString()
				if(className == "") {
					teacherCreateClassName.error = "Class Name is Required"
				}
				else {
					val inputMethodManager: InputMethodManager = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
					if (inputMethodManager.isAcceptingText) {
						inputMethodManager.hideSoftInputFromWindow((context as CreateNewClass).currentFocus?.windowToken, 0)
					}
					teacherCreateClassName.isErrorEnabled = false
					createClass(position, className)
				}
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return imageUrls.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterCreateClassBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	
	
	private fun createClass(position: Int, className: String) {
		teacherCreateProgressLayout.visibility = View.VISIBLE
		
		val db = Firebase.firestore.collection("attendance").document(Firebase.auth.uid.toString())
		
		db.update(className, imageUrls[position])
			.addOnSuccessListener {
				teacherCreateProgressLayout.visibility = View.GONE
				AlertDialog.Builder(teacherCreateClassName.context)
					.setTitle("Successful")
					.setMessage("Class \"$className\" created successfully")
					.setCancelable(false)
					.setPositiveButton("Back to Homepage") { _, _ ->
						val context: Context = context
						(context as CreateNewClass).finish()
					}
					.show()
			}
			.addOnFailureListener {
				db.set(mapOf(className to imageUrls[position]))
					.addOnSuccessListener {
						teacherCreateProgressLayout.visibility = View.GONE
						AlertDialog.Builder(teacherCreateClassName.context)
							.setTitle("Successful")
							.setMessage("Class \"$className\" created successfully")
							.setCancelable(false)
							.setPositiveButton("Back to Homepage") { _, _ ->
//								context.startActivity(Intent(binding.root.context, Home::class.java))
								(context as CreateNewClass).finish()
							}
							.show()
					}
					.addOnFailureListener {
						AlertDialog.Builder(teacherCreateClassName.context)
							.setTitle("Failure")
							.setMessage(it.localizedMessage)
							.setCancelable(false)
							.setPositiveButton("Back to Classes") { _, _ ->
//								context.startActivity(Intent(binding.root.context, Home::class.java))
								(context as CreateNewClass).finish()
							}
							.show()
					}
			}
		
	}
	
}