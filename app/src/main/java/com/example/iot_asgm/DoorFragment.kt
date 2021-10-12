package com.example.iot_asgm

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_door.*
import kotlinx.android.synthetic.main.fragment_door.view.*
import kotlinx.android.synthetic.main.fragment_light.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DoorFragment : Fragment() {

    private val pidevicename: String = "PI_12_"
    private var on = false
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var minsec: String
    private var camera_status = false

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_door, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Smart Door"

        val writedb = FirebaseDatabase.getInstance().reference

        sharedPreferences =
            this.requireActivity().getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        if (sharedPreferences.getBoolean("security_mode", false)) {
            on = true
            root.security_mode.isChecked = true
        }

        if (sharedPreferences.getBoolean("alarm", false)) {
            root.alarm_switch.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("buzzer").setValue("1")
        }

        if (sharedPreferences.getBoolean("door", false)) {
            root.door_switch.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
            writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("DOOR UNLOCKED...")
        }

        root.security_mode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                on = true
                editor.putBoolean("security_mode", true)
                editor.apply()

            } else {
                on = false
                root.us_value.isVisible = false
                root.datetime.isVisible = false
                root.door_image.isVisible = false

                editor.putBoolean("security_mode", false)
                editor.apply()
            }
        }

        root.alarm_switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                writedb.child(pidevicename + "CONTROL").child("buzzer").setValue("1")
                editor.putBoolean("alarm", true)
                editor.apply()
            } else {
                writedb.child(pidevicename + "CONTROL").child("buzzer").setValue("0")
                editor.putBoolean("alarm", false)
                editor.apply()
            }
        }


        root.door_switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("DOOR UNLOCKED...")
                editor.putBoolean("door", true)
                editor.apply()


            } else {
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("DOOR LOCKED.....")
                editor.putBoolean("door", false)
                editor.apply()
            }
        }

        // current Date and hour format
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")
        val hourFormat = SimpleDateFormat("HH")
        // adjust the hour
        hourFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")

        // get current date and hour
        val date: String = dateFormat.format(Date())
        val hour: String = hourFormat.format(Date())


        // database reference
        val fetchDatabaseRef =
            FirebaseDatabase.getInstance()
                .reference.child(pidevicename + date)

        // fetch the last records
        val lastRec = fetchDatabaseRef.child(hour).orderByKey().limitToLast(1)

        var cap_photo = false
        val postListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in snapshot.children) {
                    minsec = i.key.toString()
                    val distance = i.child("rand1").value.toString().toFloat()
                    Log.i("firebase", i.value.toString())
                    if (camera_status) {
                        cap_photo = true
                        camera_status = false
                        root.door_image.isVisible = true
                        root.datetime.isVisible = true
                        root.datetime.text = context?.getString(
                            R.string.cap_img, "Date: " + date +
                                    " Time: " + hour + minsec
                        )
                    }

                    if (on) {
                        root.us_value.isVisible = true
                        root.us_value.text = context?.getString(R.string.ultrasonic, distance)
                        if (distance < 40) {
                            writedb.child(pidevicename + "CONTROL").child("camera").setValue("1")
                            camera_status = true

                        }

                    }

                }
                Timer().schedule(6000) {
                    // do something after 1 second
                    if (cap_photo) {

                        //image name format = cam_20210328125043
                        val imagename = "cam_$date$hour$minsec"
                        Log.i("name", pidevicename + "CONTROL/" + imagename + ".jpg")

                        val fstorage = FirebaseStorage.getInstance()
                            .reference.child(pidevicename + "CONTROL/" + imagename + ".jpg")
                        val localFile = createTempFile(imagename, "jpg")
                        fstorage.getFile(localFile).addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Image Captured Successfully",
                                Toast.LENGTH_LONG
                            ).show()
                            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                            root.door_image.setImageBitmap(bitmap)
                        }

                        cap_photo = false
                    }
                }


            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }
        lastRec.addValueEventListener(postListener)

        return root
    }


}