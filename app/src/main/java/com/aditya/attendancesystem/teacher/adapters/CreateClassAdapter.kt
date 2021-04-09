package com.aditya.attendancesystem.teacher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aditya.attendancesystem.databinding.TeacherAdapterCreateClassBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class CreateClassAdapter(private val imageUrls : ArrayList<String>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
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
				.into(teacherAdapterCreateClassImageView, object : Callback{
					override fun onSuccess() {
						teacherAdapterCreateClassProgress.visibility = View.GONE
					}
					
					override fun onError(e: Exception?) {
						root.visibility = View.GONE
					}
				})
		}
	}
	
	
	override fun getItemCount(): Int {
		return imageUrls.size
	}
	
	
	private inner class ViewHolder(val binding: TeacherAdapterCreateClassBinding) : RecyclerView.ViewHolder(binding.root)
	
}