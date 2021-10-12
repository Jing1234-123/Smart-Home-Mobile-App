package com.example.iot_asgm

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_light.view.*
import kotlinx.android.synthetic.main.fragment_thermostat.view.*
import java.text.SimpleDateFormat
import java.util.*

class LightFragment : Fragment() {

    private val pidevicename: String = "PI_12_"
    private var on = false
    lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_light, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Smart Light"

        val writedb = FirebaseDatabase.getInstance().reference
        sharedPreferences =
            this.requireActivity().getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean("smart_mode", false)) {
            root.smart_mode_switch.isChecked = true
            root.manual_mode_switch.isEnabled = false
            on = true
        }

        if (sharedPreferences.getBoolean("manual_mode", false)) {
            root.manual_mode_switch.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
            writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("====Light ON====")
        }

        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        root.cur_light_intensity.text = context?.getString(R.string.light_inten, 0f)
        root.smart_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                root.manual_mode_switch.isEnabled = false
                on = true
                editor.putBoolean("smart_mode", true)
                editor.apply()
            } else {
                root.manual_mode_switch.isEnabled = true
                on = false
                editor.putBoolean("smart_mode", false)
                editor.apply()
            }
        }

        root.manual_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                root.bulb.setImageResource(R.drawable.on_bulb)
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("====Light ON====")
                editor.putBoolean("manual_mode", true)
                editor.apply()
            } else {
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("===Light OFF====")
                root.bulb.setImageResource(R.drawable.off_bulb)
                editor.putBoolean("manual_mode", false)
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
        val fetchDatabaseRef = FirebaseDatabase.getInstance()
            .reference.child(pidevicename + date)

        // fetch the last records
        val lastRec = fetchDatabaseRef.child(hour).orderByKey().limitToLast(1)

        val postListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in snapshot.children) {
                    val light_inten = i.child("rand1").value.toString().toFloat()
                    Log.i("firebase", i.value.toString())

                    root.cur_light_intensity.text =
                        context?.getString(R.string.light_inten, light_inten)
                    if (on) {
                        if (light_inten < 30) {
                            root.bulb.setImageResource(R.drawable.on_bulb)
                            root.manual_mode_switch.isChecked = true
                            editor.putBoolean("manual_mode", true)
                            editor.apply()
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("====Light ON====")
                        } else {
                            root.bulb.setImageResource(R.drawable.off_bulb)
                            root.manual_mode_switch.isChecked = false
                            editor.putBoolean("manual_mode", false)
                            editor.apply()
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("===Light OFF====")

                        }

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