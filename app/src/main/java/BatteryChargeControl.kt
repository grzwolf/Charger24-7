package com.example.charger247;

import android.content.BroadcastReceiver
import android.os.Handler
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley.*

//
// monitor & control phone's battery charging with a Tasmota compatible WiFi socket switch
//
// needs manifest entry: <uses-permission android:name="android.permission.INTERNET"/>
// needs manifest entry: android:networkSecurityConfig="@xml/network_security_config" ...
//                         ... containing <base-config cleartextTrafficPermitted="true" />
public class BatteryChargeControl(wifiChargerInUse: Boolean,
                                  wifiChargerIpAddress: String) : BroadcastReceiver() {

    // status vars
    var level = 0
    var chargingStatus: String? = null
    var wifiChargerIpAddress = ""
    var wifiChargerInUse = false
    var wifiChargerStatusText = ""
    var chargeFullMs: Long = -1
    var dischargeMs: Long = -1

    // class global http request queue avoids memory leak
    val queue = newRequestQueue(MainActivity.applicationContext())

    init {
        this.wifiChargerInUse = wifiChargerInUse
        this.wifiChargerIpAddress = wifiChargerIpAddress
    }

    override fun onReceive(context: Context, intent: Intent) {
        // battery status monitoring
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        this.level = (100 * level / scale.toFloat()).toInt()
        val lowState = intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        when (status) {
            BatteryManager.BATTERY_STATUS_DISCHARGING -> this.chargingStatus = "-"
            BatteryManager.BATTERY_STATUS_CHARGING    -> this.chargingStatus = "+"
            BatteryManager.BATTERY_STATUS_FULL        -> this.chargingStatus = "F"
            else                                      -> this.chargingStatus = "?"
        }
        // control battery charger switch depending on battery SOC
        if (this.wifiChargerInUse) {
            if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                this.dischargeMs = System.currentTimeMillis()
            }
            // it may happen, that SOC status is fully charged, but level is lower than 100%
            if (this.level > 80 || status == BatteryManager.BATTERY_STATUS_FULL) {
                this.chargeFullMs = System.currentTimeMillis()
                this.dischargeMs = System.currentTimeMillis()
                chargerSwitchOff(this.wifiChargerIpAddress)
            }
            // it may happen, that level is ok, but battery SOC status is low
            if (this.level <= 21 || lowState) {
                chargerSwitchOn(this.wifiChargerIpAddress)
            }
        }
    }

    // communicate with a "Tasmota compatible WiFi socket" to control phone's battery charging
    private fun httpCallCharger(url: String, text: String) {
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            Response.Listener<String> { response ->
                if (response != null) {
                    setChargerSwitchResponseStatus(response, text)
                } else {
                    setChargerSwitchResponseStatus("no response", "connected")
                }
            },
            Response.ErrorListener {
                setChargerSwitchResponseStatus("error", "Something went wrong.")
            }
        )
        this.queue.add(stringRequest)
    }

    fun chargerSwitchOn(ipString: String) {
        httpCallCharger("http://$ipString/cm?cmnd=Power on", "")
    }

    fun chargerSwitchOff(ipString: String) {
        httpCallCharger("http://$ipString/cm?cmnd=Power off", "")
    }

    fun getChargerSwitchStatus(ipString: String, text: String) {
        Handler().postDelayed(
            { httpCallCharger("http://$ipString/cm?cmnd=status", text) },
            2000
        )
    }
    fun getChargerStatus() {
        getChargerSwitchStatus(this.wifiChargerIpAddress, "")
    }

    fun setChargerSwitchResponseStatus(response: String, text: String) {
        if (response.contains("\"Power\":1")) {
            this.wifiChargerStatusText = text + "Power = ON"
        } else {
            if (response.contains("\"Power\":0")) {
                this.wifiChargerStatusText = text + "Power is off"
            } else {
                this.wifiChargerStatusText = text + " Power undefined"
            }
        }
    }
}