package com.example.annasblackhat.printwificlientserver

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.epson.epos2.discovery.DeviceInfo
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import android.widget.SimpleAdapter
import kotlinx.android.synthetic.main.activity_find_printer.*
import android.content.Intent
import android.view.MenuItem


class FindPrinterActivity : AppCompatActivity(), DiscoveryListener, AdapterView.OnItemClickListener {

    private var mPrinterList: ArrayList<HashMap<String, String>>? = null
    private var mPrinterListAdapter: SimpleAdapter? = null
    private lateinit var  mFilterOption: FilterOption

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_printer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mPrinterList = ArrayList()
        mPrinterListAdapter = SimpleAdapter(this, mPrinterList, R.layout.list_at,
                arrayOf("PrinterName", "Target"),
                intArrayOf(R.id.PrinterName, R.id.Target))

        list_view.adapter = mPrinterListAdapter

        list_view.onItemClickListener = this
        btn_refresh.setOnClickListener { refreshFinder() }

        mFilterOption = FilterOption()
        mFilterOption.deviceType = Discovery.TYPE_PRINTER
        mFilterOption.epsonFilter = Discovery.FILTER_NAME

        try {
            Discovery.start(this, mFilterOption, this)
        } catch (e: Exception) {
            Toast.makeText(this, "Start Error. "+e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshFinder(){
        while (true){
            try {
                Discovery.stop()
                break
            } catch (e: Exception) {
                Toast.makeText(this, "Stop Error. "+e.message, Toast.LENGTH_SHORT).show()
                return
            }
        }

        mPrinterList?.clear()
        mPrinterListAdapter?.notifyDataSetChanged()

        try {
            Discovery.start(this, mFilterOption, this)
            Toast.makeText(this, "Refresh...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Restart Error. "+e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        val intent = Intent()

        val item = mPrinterList?.get(position)
        intent.putExtra("target", item?.get("Target"))
        setResult(Activity.RESULT_OK, intent)

        finish()
    }

    override fun onDiscovery(deviceInfo: DeviceInfo?) {
        runOnUiThread {
            val item = HashMap<String, String>()
            item["PrinterName"] = deviceInfo?.deviceName ?: ""
            item["Target"] = deviceInfo?.target ?: ""
            mPrinterList?.add(item)
            mPrinterListAdapter?.notifyDataSetChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        while (true){
            try {
                Discovery.stop()
                break
            } catch (e: Exception) {
                break
            }
        }
    }
}
