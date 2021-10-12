package com.example.iot_asgm

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_curtain.view.*
import kotlinx.android.synthetic.main.fragment_curtain.view.cur_temp
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CurtainFragment : Fragment() {
    private val pidevicename: String = "PI_12_"
    private var on = false
    lateinit var sharedPreferences: SharedPreferences
    private var threshold = 20
    private var hour = 0
    private var min = 0
    private var alarm_time = ""
    private var alarm_on = false


    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_curtain, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Smart Curtain"

        val writedb = FirebaseDatabase.getInstance().reference

        root.cur_temp.text = context?.getString(R.string.cur_temp, 25f)
        sharedPreferences =
            this.requireActivity().getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        if (sharedPreferences.getBoolean("threshold_set", false)) {
            threshold = sharedPreferences.getInt("threshold", 0)
            root.temp.value = threshold.toFloat()
            root.thres_temp.text = getString(R.string.threshold_temp, root.temp.value)
        }

        if (sharedPreferences.getBoolean("curtain_auto_mode", false)) {
            on = true
            root.remote_cur_switch.isEnabled = false
            root.auto_curtain_switch.isChecked = true

        }

        if (sharedPreferences.getBoolean("open_curtain", false)) {
            root.status.text = context?.getString(R.string.status, "OPEN")
            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
            writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("==CURTAIN OPEN==")

        } else {
            root.status.text = context?.getString(R.string.status, "CLOSED")
        }

        if (sharedPreferences.getBoolean("alarm_on", false)) {
            root.alarm_switch.isChecked = true
            alarm_on = true

        }

        root.temp.addOnChangeListener { _, _, _ ->
            root.thres_temp.text = context?.getString(R.string.threshold_temp, root.temp.value)
            threshold = root.temp.value.toInt()
            editor.putInt("threshold", threshold)
            editor.putBoolean("threshold_set", true)
            editor.apply()
        }

        root.alarm_picker_min.minValue = 0
        root.alarm_picker_min.maxValue = 59

        root.alarm_picker_min.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        root.alarm_picker_hour.minValue = 0
        root.alarm_picker_hour.maxValue = 23

        if (sharedPreferences.getBoolean("alarm_set", false)) {
            alarm_time = sharedPreferences.getString("alarm", "").toString()
            root.alarm_time.text = context?.getString(R.string.time, alarm_time)


            root.alarm_picker_hour.value = alarm_time.substring(0, 2).toInt()
            Log.i("testing", alarm_time.substring(0, 2).toInt().toString())
            root.alarm_picker_min.value = alarm_time.substring(2, 4).toInt()
            Log.i("testing", alarm_time.substring(2, 4).toInt().toString())
        }

        root.alarm_picker_hour.setFormatter(NumberPicker.Formatter { i ->
            String.format(
                "%02d",
                i
            )
        })

        // enable the time number picker
        root.set_alarm.setOnClickListener {
            root.set_time.isVisible = true
            root.alarm_picker_hour.isVisible = true
            root.alarm_picker_min.isVisible = true
            root.confirm.isVisible = true
            root.confirm.isEnabled = true
        }


        root.confirm.setOnClickListener {
            hour = root.alarm_picker_hour.value
            min = root.alarm_picker_min.value
            alarm_time = String.format("%02d", hour) + String.format("%02d", min)
            root.set_time.isVisible = false
            root.alarm_picker_hour.isVisible = false
            root.alarm_picker_min.isVisible = false
            root.confirm.isVisible = false
            root.confirm.isEnabled = false

            root.alarm_time.text = context?.getString(R.string.time, alarm_time)
            editor.putString("alarm", alarm_time)
            editor.putBoolean("alarm_set", true)
            editor.apply()

        }

        root.auto_curtain_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                on = true
                root.remote_cur_switch.isEnabled = false
                editor.putBoolean("curtain_auto_mode", true)
                editor.apply()

            } else {
                on = false
                root.remote_cur_switch.isEnabled = true
                editor.putBoolean("curtain_auto_mode", false)
                editor.apply()
            }
        })

        root.remote_cur_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                root.status.text = context?.getString(R.string.status, "OPEN")
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("==CURTAIN OPEN==")

                editor.putBoolean("open_curtain", true)
                editor.apply()

            } else {
                root.status.text = context?.getString(R.string.status, "CLOSED")
                writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                writedb.child(pidevicename + "CONTROL").child("lcdtxt").setValue("=CURTAIN CLOSED=")

                editor.putBoolean("open_curtain", false)
                editor.apply()
            }
        })

        root.alarm_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                alarm_on = true

                editor.putBoolean("alarm_on", true)
                editor.apply()

            } else {
                alarm_on = false

                editor.putBoolean("alarm_on", false)
                editor.apply()
            }
        })

        // current Date and hour format
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")
        val hourFormat = SimpleDateFormat("HH")
        val hourminFormat = SimpleDateFormat("HHmm")
        // adjust the hour
        hourFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")
        hourminFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")

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
                    val rand1 = i.child("rand1").value.toString().toFloat()
                    Log.i("firebase", i.value.toString())

                    val temp = ((rand1 * (35 - 20)) / 65) + 20
                    root.cur_temp.text = context?.getString(R.string.cur_temp, temp)

                    // if auto mode is on
                    if (on) {
                        if (temp >= threshold) {
                            root.remote_cur_switch.isChecked = true
                            root.status.text = context?.getString(R.string.status, "OPEN")
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("==CURTAIN OPEN==")

                            editor.putBoolean("open_curtain", true)
                            editor.apply()

                        } else {
                            root.remote_cur_switch.isChecked = false
                            root.status.text = context?.getString(R.string.status, "CLOSED")
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("0")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("=CURTAIN CLOSED=")

                            editor.putBoolean("open_curtain", false)
                            editor.apply()
                        }
                    }


                }
                if (alarm_on) {
                    val cur_time: String = hourminFormat.format(Date())
                    // alarm is triggered

                    if (alarm_time == cur_time) {
                        if (sharedPreferences.getBoolean("alarm_close", false)) {
                            root.remote_cur_switch.isChecked = true
                            root.status.text = context?.getString(R.string.status, "OPEN")
                            writedb.child(pidevicename + "CONTROL").child("relay1").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("buzzer").setValue("1")
                            writedb.child(pidevicename + "CONTROL").child("lcdtxt")
                                .setValue("==CURTAIN OPEN==")
//                            getFragmentManager()?.popBackStackImmediate()
                            view?.findNavController()
                                ?.navigate(R.id.action_curtainFragment_to_alarmFragment)

                        }

                    } else {
                        editor.putBoolean("alarm_close", true)
                        editor.apply()
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