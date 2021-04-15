package com.aditya.attendancesystem.teacher.helperclasses

import com.google.firebase.firestore.GeoPoint

data class StudentDataVerification(var id: String?, val Name: String?, val Phone: String?, val Email: String?, val RollNumber: String?) {
	constructor() : this(null, null, null, null, null)
}


data class StudentAttendanceRecord(var id: String?, var Name: String?, val Phone: String?, val Email: String?, val RollNumber: String?, var attendanceCount: Int = 0, var attendanceDates: ArrayList<String>?) {
	constructor() : this(null, null, null, null, null, 0, null)
}


data class AttendanceLinkDataClass(
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
	constructor(id: String?, count: Int?, date: String?, duration: Int?, isActivated: Boolean?, time: String?) : this(id, count, date, duration, null, isActivated, null, null, null, time)
}


data class ClassListDataClass(val className: String?, val imageLink: String?) {
	constructor() : this(null, null)
}