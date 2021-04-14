package com.aditya.attendancesystem.teacher.helperclasses

import com.google.firebase.firestore.GeoPoint

data class StudentDataVerification(var id: String?, val Name: String?, val Phone: String?, val Role: String?, val Email: String?, val RollNumber: Int?) {
	constructor() : this(null, null, null, null, null, null, )
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
