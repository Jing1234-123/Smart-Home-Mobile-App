package com.example.iot_asgm

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_thermostat.view.*
import java.text.SimpleDateFormat
import java.util.*

class ThermostatFragment : Fragment() {

    private val pidevicename: String = "PI_12_"
    private var on = false
    lateinit var sharedPreferences: SharedPreferences
    var target_temp_set = false

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?


    ): View? {

        val root = inflater.inflate(R.layout.fragment_thermostat, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Smart Thermostat"


        BottomSheetBehavior.from(root.downsheet).apply {
            peekHeight = 200
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // write to firebase
        val writedb = FirebaseDatabase.getInstance().reference

        sharedPreferences =
            this.requireActivity().getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        target_temp_set = sharedPreferences.getBoolean("Target_temp", false)

        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        if (target_temp_set) {
            root.target_temp.text = sharedPreferences.getInt("temp", 0).toString()
        }

        if (root.target_temp.text.toString().equals("22")) {
            root.down_btn.isEnabled = false
            root.down_btn.alpha = 0.5f
        } else if (root.target_temp.text.toString().equals("30")) {
            root.up_btn.isEnabled = false
            root.up_btn.alpha = 0.5f
        }

        if (sharedPreferences.getBoolean("panel", false)) {
            on = true
            root.tbutton.isChecked = true

            // smart mode on, disable manual mode
            root.aircons_swtich.isEnabled = false
            root.fan_switch.isEnabled = false
            root.air_humi_switch.isEnabled = false
        }

        if (sharedPreferences.getBoolean("aircons", false)) {
            // The toggle is enabled - ON
            root.aircons_swtich.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
            writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("=AC-ON, FAN-OFF=")
        }

        if (sharedPreferences.getBoolean("fan", false)) {
            // The toggle is enabled - ON
            root.fan_switch.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("relay2").setValue("1")
            writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("=AC-OFF, FAN-ON=")
        }

        if (sharedPreferences.getBoolean("humi", false)) {
            // The toggle is enabled - ON
            root.air_humi_switch.isChecked = true
            writedb.child(pidevicename + "CONTROL").child("ledlgt").setValue("1")
        }

        // initialize those value
        root.cur_temp.text = context?.getString(R.string.air_cons_cur_temp, 25.0)
        root.cur_humi.text = context?.getString(R.string.cur_humi, 65.0)


        // if the thermostat is open
        root.tbutton.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled - ON
                root.panel.alpha = 1f
                on = true

                // smart mode on, disable manual mode
                root.aircons_swtich.isEnabled = false
                root.fan_switch.isEnabled = false
                root.air_humi_switch.isEnabled = false

                editor.putBoolean("panel", true)
                editor.apply()

            } else {
                // The toggle is disabled - OFF
                root.panel.alpha = 0.5f
                on = false

                // smart mode off, enable manual mode
                root.aircons_swtich.isEnabled = true
                root.fan_switch.isEnabled = true
                root.air_humi_switch.isEnabled = true

                editor.putBoolean("panel", false)
                editor.apply()
            }
        })


        root.aircons_swtich.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled - ON
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("===AIRCONS-ON===")
                editor.putBoolean("aircons", true)
                editor.apply()
            } else {
                // The toggle is disabled - OFF
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("===AIRCONS-OFF==")
                editor.putBoolean("aircons", false)
                editor.apply()
            }
        })

        root.fan_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled - ON
                writedb.child(pidevicename + "CONTROL").child("relay2").setValue("1")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("=====FAN-ON=====")
                editor.putBoolean("fan", true)
                editor.apply()
            } else {
                // The toggle is disabled - OFF
                writedb.child(pidevicename + "CONTROL").child("relay2").setValue("0")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("====FAN-OFF=====")
                editor.putBoolean("fan", false)
                editor.apply()

            }
        })

        root.air_humi_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled - ON
                writedb.child(pidevicename + "CONTROL").child("ledlgt").setValue("1")
                editor.putBoolean("humi", true)
                editor.apply()
            } else {
                // The toggle is disabled - OFF
                writedb.child(pidevicename + "CONTROL").child("ledlgt").setValue("0")
                editor.putBoolean("humi", false)
                editor.apply()
            }
        })

        root.up_btn.setOnClickListener {
            root.down_btn.isEnabled = true
            root.down_btn.alpha = 1.0f

            var target_temp = Integer.parseInt(root.target_temp.text.toString())

            target_temp++

            editor.putInt("temp", target_temp)
            editor.putBoolean("Target_temp", true)
            editor.apply()

            root.target_temp.text = target_temp.toString()

            if (target_temp == 30) {
                root.up_btn.isEnabled = false
                root.up_btn.alpha = 0.5f
            }
        }

        root.down_btn.setOnClickListener {
            root.up_btn.isEnabled = true
            root.up_btn.alpha = 1.0f

            var target_temp = Integer.parseInt(root.target_temp.text.toString())

            target_temp--
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putInt("temp", target_temp)
            editor.putBoolean("Target_temp", true)
            editor.apply()

            root.target_temp.text = target_temp.toString()

            if (target_temp == 22) {
                root.down_btn.isEnabled = false
                root.down_btn.alpha = 0.5f
            }
        }


        // when user leave home
        root.away.setOnClickListener {
            val dialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
            dialog.setMessage("Confirm to close all things?")
            dialog.setTitle("Confirmation")

            dialog.setNegativeButton(
                "Yes"
            ) { _, _ ->
                root.tbutton.isChecked = false
                root.aircons_swtich.isChecked = false
                root.fan_switch.isChecked = false
                root.air_humi_switch.isChecked = false

                editor.putBoolean("panel", false)
                editor.putBoolean("aircons", false)
                editor.putBoolean("fan", false)
                editor.putBoolean("humi", false)
                editor.apply()

                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("=AC-OFF,FAN-OFF=")
            }
            dialog.setPositiveButton("Cancel")
            { _, _ -> }

            val alertDialog: android.app.AlertDialog? = dialog.create()
            alertDialog!!.show()

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
                for (snapshot in snapshot.children) {
                    val rand1 = snapshot.child("rand1").value.toString().toFloat()
                    val rand2 = snapshot.child("rand2").value.toString().toFloat()
                    Log.i("firebase", snapshot.value.toString())

                    // convert the range of rand1 to 20-35 for temperature
                    val cur_temp = ((rand1 * (35 - 20)) / 65) + 20
                    root.cur_temp.text = context?.getString(R.string.air_cons_cur_temp, cur_temp)

                    // convert the range of rand2 to 0-100 for humidity level
                    val cur_humi = ((rand2 * (100 - 30)) / 1023) + 30

                    root.cur_humi.text = context?.getString(R.string.cur_humi, cur_humi)

                    if (on) {

                        // air cons and fans
                        val target_temp = Integer.parseInt(root.target_temp.text.toString())
                        if (cur_temp > target_temp) {
                            root.aircons_swtich.isChecked = true
                            root.fan_switch.isChecked = false
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("relay2").setValue("0")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("=AC-ON, FAN-OFF=")
                        } else {
                            root.fan_switch.isChecked = true
                            root.aircons_swtich.isChecked = false
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                            writedb.child(pidevicename + "CONTROL").child("relay2").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("=AC-OFF, FAN-ON=")
                        }

//                        if(root.aircons_swtich.isChecked){
//                            if(cur_temp < target_temp - 3)
//                            {
//                                root.fan_switch.isChecked = true
//                                root.aircons_swtich.isChecked = false
//                                // writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
//                                // writedb.child(pidevicename + "CONTROL").child("relay2").setValue("1")
//                            }
//                        }


                        // air humidifier on if current humidity level less than 65%
                        if (cur_humi < 65) {
                            root.air_humi_switch.isChecked = true
                            writedb.child(pidevicename + "CONTROL").child("ledlgt").setValue("1")
                        } else {
                            root.air_humi_switch.isChecked = false
                            writedb.child(pidevicename + "CONTROL").child("ledlgt").setValue("0")
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