package com.example.charger247

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.charger247.ui.theme.Charger247Theme
import java.util.Timer
import java.util.TimerTask

class MainActivity : ComponentActivity() {

    // have global access to MainActivity context
    companion object {
        private var instance: MainActivity? = null
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
    init {
        instance = this
    }

    // battery control object
    private var battery: BatteryChargeControl? = null

    // life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // prevent sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // show battery status via timer
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (battery != null) {
                        battery?.getChargerStatus()
                        setContent {
                            Charger247Theme {
                                Surface(modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background) {
                                    Text(text =
                                            "24/7 Android battery charger: " +
                                            battery?.chargingStatus + " " +
                                            battery?.wifiChargerStatusText)
                                }
                            }
                        }
                    } else {
                        setContent {
                            Charger247Theme {
                                Surface(modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background) {
                                    Text(text = "no battery control")
                                }
                            }
                        }
                    }
                }
            }
        }, 0, 10000)
    }

    override fun onResume() {
        // BatteryChargeControl
        if (battery != null) {
            try {
                unregisterReceiver(battery)
                battery = null
            } catch (e: Exception) {
                Log.e("onCreate()", "unregisterReceiver " + e.message)
            }
        }
        var wifiChargerInUse = true             // control whether to use the charger control
        var wifiChargerIpAddress = "10.0.1.19"  // local ip of "Tasmota compatible WiFi socket"
        battery = BatteryChargeControl(wifiChargerInUse, wifiChargerIpAddress)
        this.registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        super.onResume()
    }

    override fun onStop() {
        if (battery != null) {
            try {
                unregisterReceiver(battery)
                battery = null
            } catch (e: Exception) {
                Log.e("onStop()", "unregisterReceiver " + e.message)
            }
        }
        super.onStop()
    }

}





