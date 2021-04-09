package com.aditya.attendancesystem.teacher.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterClassesBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class ClassesAdapter(private val classes: ArrayList<String>, private val urls: ArrayList<String>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
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
				.load(Uri.parse(urls[position]))
				.resize(0, 150)
				.into(teacherAdapterClassesRecyclerImageView, object : Callback {
					override fun onSuccess() {
						teacherAdapterClassesRecyclerImageProgress.visibility = View.GONE
						teacherAdapterClassesRecyclerTextView.text = classes[position]
					}
					override fun onError(e: Exception?) {
						if (e != null) {
							Log.e(TAG, "onError: Error Loading Image: ${e.localizedMessage}")
							root.visibility = View.GONE
						}
					}
				})
		}
	}
	
	override fun getItemCount(): Int {
		return classes.size
	}
	
	
	
	private inner class ViewHolder(val binding: TeacherAdapterClassesBinding) : RecyclerView.ViewHolder(binding.root)
	
}