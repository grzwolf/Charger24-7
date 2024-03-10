# Android Phone Charge 24/7
Run an Android phone permanently connected to its charger to allow 24/7 operation.

# Background
There are scenarios requiring to run the phone permanently w/o interruption and 
no further user interaction like taking care about phone charging.
But running an Android phone 24/7 might become tricky for several reasons:
- permanently connecting the phone to a charger might be risky
- the phone's internal charging logic is not always reliable
- replacing the battery with a static power supply might not work at all

The solution provided here requires a WiFi / http controllable power socket, like 
Tasmota power sockets as listed here: https://templates.blakadder.com/plug.html.  
Material costs are approx. 15,- Euro.

# Working principle
The Android phone is permanently connected to a compatible charger 
via the Tasmota WiFi switchable socket.
Tasmota devices are controlled inside the local WiFi network, no internet 
connection is needed.

The class BatteryChargeControl implements: 
- a phone battery monitor
- Tasmota http calls to control the WiFi switch

The most important input parameter is the local IP address of the 
Tasmota socket device.

The phone's battery is charged, if the battery SOC is lower than 20%.
That means, the battery monitor will switch the WiFi socket to power on.

As soon as the battery monitor detects a higher SOC as 80%, 
the WiFi socket is switched to power off. 

The solution was only tested with a "NOUS A1T" device, but compatible devices
shall work w/o code modifications too. 