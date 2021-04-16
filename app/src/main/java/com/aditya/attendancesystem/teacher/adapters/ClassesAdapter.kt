package com.aditya.attendancesystem.teacher.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterClassesBinding
import com.aditya.attendancesystem.teacher.ClassHomePage
import com.aditya.attendancesystem.teacher.helperclasses.ClassNameImageModel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception


@SuppressLint("ApplySharedPref")
class ClassesAdapter(private val classNameImageModels: ArrayList<ClassNameImageModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	companion object{
		private const val TAG = "ClassesAdapter"
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(TeacherAdapterClassesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder.binding) {
			Picasso.get()
				.load(Uri.parse(classNameImageModels[position].imageLink))
				.resize(0, 150)
				.into(teacherAdapterClassesRecyclerImageView, object : Callback {
					override fun onSuccess() {
						teacherAdapterClassesRecyclerImageProgress.visibility = View.GONE
						teacherAdapterClassesRecyclerTextView.text = classNameImageModels[position].className
					}
					override fun onError(e: Exception?) {
						if (e != null) {
							Log.e(TAG, "onError: Error Loading Image: ${e.localizedMessage}")
							root.visibility = View.GONE
						}
					}
				})
			
			root.setOnClickListener {
				val intent = Intent(root.context, ClassHomePage::class.java)
				root.context.getSharedPreferences("ClassDetails", AppCompatActivity.MODE_PRIVATE).edit().apply {
					putString("className", classNameImageModels[position].className)
					putString("classImage", classNameImageModels[position].imageLink)
					commit()
				}
				root.context.startActivity(intent)
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return classNameImageModels.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterClassesBinding) : RecyclerView.ViewHolder(binding.root)
	
}