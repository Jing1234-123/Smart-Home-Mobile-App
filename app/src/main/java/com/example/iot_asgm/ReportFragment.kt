package com.example.iot_asgm

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.fragment_report.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ReportFragment : Fragment() {
    private val pidevicename: String = "PI_12_"
    private var tempArrayList = arrayListOf<String>()
    private var humiArrayList = arrayListOf<String>()
    var firstLoad = true
    val temp = ArrayList<Entry>()
    val humi = ArrayList<Entry>()
    val xvalue = ArrayList<String>()
    val tempdataset = LineDataSet(temp, "Temp")
    val humidataset = LineDataSet(humi, "Humi")
    var total_humi = 0f
    var total_temp = 0f

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?


    ): View? {
        val root = inflater.inflate(R.layout.fragment_report, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = "Summary Report"

        // current Date and hour format
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")
        val hourFormat = SimpleDateFormat("HH")
        // adjust the hour
        hourFormat.timeZone = TimeZone.getTimeZone("GMT+08:00")

        // get current date and hour
        val date: String = dateFormat.format(Date())
        val hour: String = hourFormat.format(Date())

        root.hour.text = getString(R.string.cur_hour, hour)
        // database reference
        val fetchDatabaseRef = FirebaseDatabase.getInstance()
            .reference.child(pidevicename + date).child(hour)


        val postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                tempArrayList.clear()
                humiArrayList.clear()
                for (i in snapshot.children) {
                    val rand1 = i.child("rand1").value.toString().toFloat()
                    val rand2 = i.child("rand2").value.toString().toFloat()
                    Log.i("firebase", i.value.toString())

                    // convert the range of rand1 to 20-35 for temperature
                    val temp = (((rand1 * (35 - 20)) / 65) + 20)

                    // convert the range of rand2 to 0-100 for humidity level
                    val humi = (((rand2 * (100 - 30)) / 1023) + 30)


                    tempArrayList.add(i.key.toString() + "/" + String.format("%.2f", temp))
                    humiArrayList.add(i.key.toString() + "/" + String.format("%.2f", humi))

                }

                if (firstLoad || tempArrayList.size == 1) {

                    for (i in tempArrayList.indices) {
                        val info: List<String> = tempArrayList[i].split("/")
                        val time = info[0]
                        val tempe = info[1]

                        xvalue.add(time)

                        temp.add(Entry(tempe.toFloat(), i))
                        total_temp += tempe.toFloat()

                    }

                    root.avg_temp.text =
                        context?.getString(R.string.avg_temp, total_temp / tempArrayList.size)



                    for (i in humiArrayList.indices) {
                        val info: List<String> = humiArrayList[i].split("/")
                        val humidity = info[1]


                        humi.add(Entry(humidity.toFloat(), i))
                        total_humi += humidity.toFloat()

                    }

                    root.avg_humi.text =
                        context?.getString(R.string.avg_humi, total_humi / humiArrayList.size)


                    firstLoad = false
                    // set lineCHart design
                    tempdataset.color = R.color.purple_700
                    humidataset.color = R.color.blue


                    tempdataset.circleRadius = 0f
                    tempdataset.setDrawFilled(true)
                    tempdataset.fillColor = R.color.purple_700
//                      tempdataset.fillAlpha = 30
                    tempdataset.valueTextSize = 15F

                    humidataset.circleRadius = 0f
                    humidataset.setDrawFilled(true)
                    humidataset.fillColor = R.color.blue
                    //humidataset.fillAlpha = 10
                    humidataset.valueTextSize = 15F


                    root.progressBar.isVisible = false

                    val finaldataset = ArrayList<LineDataSet>()
                    finaldataset.add(tempdataset)
                    finaldataset.add(humidataset)
                    tempdataset.setDrawCubic(true)

                    val data = LineData(xvalue, finaldataset as List<ILineDataSet>?)
                    root.lineChart.data = data
                    root.lineChart.setBackgroundColor(resources.getColor(R.color.white))
                    root.lineChart.animateXY(3000, 3000)

                    val l: Legend = root.lineChart.legend
                    l.textSize = 20f
                    l.textColor = R.color.black;
                    l.form = Legend.LegendForm.CIRCLE;
                    root.lineChart.xAxis.textSize = 20f
                    root.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM

                    root.lineChart.setVisibleXRangeMaximum(10f)
                } else {
                    val info: List<String> = tempArrayList.last().split("/")
                    val time = info[0]
                    val tempe = info[1]
                    xvalue.add(time)

                    temp.add(Entry(tempe.toFloat(), tempArrayList.lastIndex))
                    total_temp += tempe.toFloat()
                    root.avg_temp.text =
                        context?.getString(R.string.avg_temp, total_temp / tempArrayList.size)

                    val humiinfo: List<String> = humiArrayList.last().split("/")
                    val humidity = humiinfo[1]

                    humi.add(Entry(humidity.toFloat(), humiArrayList.lastIndex))
                    total_humi += humidity.toFloat()
                    root.avg_humi.text =
                        context?.getString(R.string.avg_humi, total_humi / humiArrayList.size)
                    tempdataset.notifyDataSetChanged()
                    humidataset.notifyDataSetChanged()
                    root.lineChart.notifyDataSetChanged()
                    root.lineChart.invalidate()
                }
            }
        }
        fetchDatabaseRef.addValueEventListener(postListener)








        return root
    }


}



