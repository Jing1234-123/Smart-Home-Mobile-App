package com.example.iot_asgm

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.fragment_curtain.view.*

class AlarmFragment : Fragment() {
    lateinit var sharedPreferences: SharedPreferences
    private var alarm_time = ""
    private val pidevicename: String = "PI_12_"

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_alarm, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Alarm is triggered"

        val writedb = FirebaseDatabase.getInstance().reference

        sharedPreferences = this.requireActivity().getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        if (sharedPreferences.getBoolean("alarm_set", false)) {
            alarm_time = sharedPreferences.getString("alarm", "").toString()
            root.time.text = context?.getString(R.string.time, alarm_time)

        }

        root.close_btn.setOnClickListener{
            writedb.child(pidevicename + "CONTROL").child("buzzer").setValue("0")

            editor.putBoolean("alarm_close", false)
            editor.apply()
            getFragmentManager()?.popBackStack()



        }


        return root
    }
}