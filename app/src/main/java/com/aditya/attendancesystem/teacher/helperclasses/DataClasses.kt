package com.aditya.attendancesystem.teacher.helperclasses

import com.google.firebase.firestore.GeoPoint

data class StudentDataModel(var id: String?, val name: String?, val phone: String?, val email: String?, val rollNumber: String?) {
	constructor() : this(null, null, null, null, null)
}


data class StudentAttendanceModel(var id: String?, var name: String?, val phone: String?, val email: String?, val rollNumber: String?, var attendanceCount: Int = 0, var attendanceDates: ArrayList<String>?) {
	constructor() : this(null, null, null, null, null, 0, null)
}


data class DynamicLinkModel(
	var id: String?,
	val count: Int? = 0,
	val date: String?,
	val duration: Int?,
	var geoPoint: GeoPoint?,
	@field:JvmField var isActivated: Boolean?,
	@field:JvmField var isClassroomActivated: Boolean?,
	val link: String?,
	var radius: Int?,
	val time: String?
) {
	constructor() : this(null, null, null, null, null, null, null, null, null, null)
}


data class ClassNameImageModel(val className: String?, val imageLink: String?) {
	constructor() : this(null, null)
}