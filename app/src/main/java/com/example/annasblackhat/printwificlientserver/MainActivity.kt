package com.example.annasblackhat.printwificlientserver

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.Socket
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var server: Server
    private var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        server = Server(this)

        btn_ping.setOnClickListener { connectToServer() }
        opt_server.setOnCheckedChangeListener { _, isChecked -> setAsServer(isChecked)}
        btn_findprinter.setOnClickListener { startActivityForResult(Intent(this, FindPrinterActivity::class.java), 25) }
    }

    private fun connectToServer() {
        val ip = edt_ipserver.text.toString()
        if(ip.isEmpty()){
            toast("Please fill IP Address Server")
            return
        }

        val ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"
        val pattern = Pattern.compile(ipPattern)
        if(!pattern.matcher(ip).matches()){
            toast("Invalid IP address Format")
            return
        }
        async(UI){
            val pDialog = ProgressDialog.show(this@MainActivity, null, "Loading...")
            socket = bg { Socket(edt_ipserver.text.toString(), Server.socketServerPORT) }.await()
            pDialog.dismiss()

            val response = bg { BufferedReader(InputStreamReader(socket?.getInputStream())).readLine() }
            toast(response.await())
        }
    }

    private fun setAsServer(isServer: Boolean){
        if(isServer){
            server.start()
            btn_ping.setTextColor(Color.parseColor("#B6B7B7"))
        }else{
            server.onDestroy()
            btn_ping.setTextColor(Color.BLACK)
        }
        edt_ipserver.isEnabled = !isServer
        btn_ping.isEnabled = !isServer
    }

    fun showIPAddress(ip: String){
        txt_ip.text = "Socket Address : $ip"
    }

    fun toast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
