package com.critical.wifiswitcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var seekBarText: TextView
    private lateinit var prioritizeCheckBox: CheckBox
//    private var savedNetworkIds: ArrayList<Int> = ArrayList()
    private var savedNetworkIds: HashMap<String, Int> = HashMap<String, Int>()
    private var arrayList: ArrayList<String> = ArrayList()
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        checkForPermissionsAndPrompt()

        // TODO: also add swipe down refresh
        // scan when button is clicked
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            msgUser("Scanning!!!!!")
            Snackbar.make(view, "Scanning!!!!!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            scan()
        }

        prioritizeCheckBox = findViewById<CheckBox>(R.id.checkboxPrioritize)

        seekBarText = findViewById<TextView>(R.id.seekBarText)
        val seekBarMinStrength = findViewById<SeekBar>(R.id.seekBarMinStrength)
        seekBarMinStrength.setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                // TODO Auto-generated method stub
                seekBarText.text = "-" + progress.toFloat().toString()
            }
        })
        seekBarText.text = "-" + seekBarMinStrength.progress.toString()

        // setup wifi listview adapter
//        listView = findViewById<View>(R.id.wifiList) as ListView
//        arrayList.add("Wifi will populate after a scan");
//        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
//        listView.adapter = adapter

        // create wifi manager
        val context = this.applicationContext
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // if wifi is disabled, enable it
        if (!wifiManager.isWifiEnabled) {
            msgUser("WiFi is disabled ... We need to enable it")
            wifiManager.isWifiEnabled = true;
        }

        val savedNetworks = wifiManager.configuredNetworks
//        wifiManager.enableNetwork()
        for (network in savedNetworks){
            val ssid = network.SSID.substring(1, network.SSID.length - 1);
            savedNetworkIds[ssid] = network.networkId
        }

        // create receiver to check if scan was a success
        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }

        // create intent and register receiver with it
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        this.applicationContext.registerReceiver(wifiScanReceiver, intentFilter)

        // finally run the scan when the app is created
        scan()
    }

    /**
     * This was by far the most annoying part of this project
     */
    private fun checkForPermissionsAndPrompt() {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1);
            }
        }
    }

    private fun scan() {
//        val success = wifiManager.startScan()
//        if (!success) {
//            // scan failure handling
//            scanFailure()
//        }
    }

    fun scanSuccess() {
        val results = wifiManager.scanResults
        showScanResults(results)
    }

    fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager.scanResults
        Log.d("rb", "Scan failed")
//        showScanResults(results)
    }

    private fun getWifiQuality(rssi : Int) : Int {
        val quality = 2 * (rssi + 100)
        return if (quality > 100) 100 else quality
    }

    private fun showScanResults(results: MutableList<ScanResult>) {
        arrayList.clear();
        var bestWifiSSID = ""
        var bestWifiQuality = -100
        var nextBestWifiSSID = ""
        var nextBestWifiQuality = -100
        for (result in results) {
            val nameSSID = if (result.SSID.isEmpty()) "(hidden)" else result.SSID
//            val curQuality = getWifiQuality(result.level)
            val curQuality = result.level
            val msgText = nameSSID + ", rssi = " + result.level + ", q = " + getWifiQuality(result.level) + "%"
            msgUser(msgText)
            arrayList.add(msgText);
            // assign the best wifi and quality out of all the saved networks
            if (curQuality > bestWifiQuality && savedNetworkIds.containsKey(nameSSID)) {
                nextBestWifiQuality = bestWifiQuality
                nextBestWifiSSID = bestWifiSSID
                bestWifiQuality = curQuality
                bestWifiSSID = nameSSID
            }
        }
        Log.d("rb", "Best wifi is $bestWifiSSID (id=" + savedNetworkIds[bestWifiSSID] + ") with quality $bestWifiQuality%")
        // TODO: check threshold set and current wifi is different than the best
        if (wifiManager.connectionInfo.ssid != bestWifiSSID) {
//            && wifiManager.connectionInfo.rssi < Integer.parseInt(seekBarText.text as String)) {
            val netId = savedNetworkIds[bestWifiSSID] as Int
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
        // update list adapter
//        adapter.notifyDataSetChanged()
//        listView.adapter = adapter
        msgUser("Found " + results.size + " wifis")
    }

    private fun msgUser(text : String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        Log.d("rb", text)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}