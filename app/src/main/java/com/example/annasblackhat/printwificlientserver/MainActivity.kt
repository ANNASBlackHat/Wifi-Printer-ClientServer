package com.example.annasblackhat.printwificlientserver

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.*
import java.net.Socket
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var server: Server
    private var socket: Socket? = null
    private lateinit var printerManager: PrinterManager
    private var isServer = false
    private var printerTarget: String? = "TCP:192.168.0.16"
    private var selectedSetting = 0
    private val socketSenderOptions = arrayOf("Write UTF","Print Writer")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerManager = PrinterManager(this)
        server = Server(this, printerManager)

        btn_ping.setOnClickListener { connectToServer() }
        opt_server.setOnCheckedChangeListener { _, isChecked -> setAsServer(isChecked)}
        btn_findprinter.setOnClickListener { startActivityForResult(Intent(this, FindPrinterActivity::class.java), 25) }
        btn_print_custom.setOnClickListener { printCustomText() }
        btn_print_receipt.setOnClickListener { printCustomText("receipt") }
    }

    private fun printCustomText(type: String = "custom") {
        val teks = if(type == "custom")edt_custom.text.toString() else createReceipt()
        if(teks.isEmpty()){
            toast("Please type your custom text!")
            return
        }
        if(printerTarget == null){
            toast("Please choose the printer!")
            return
        }

        printerTarget?.let {
            val pendingPrint = PendingPrintEntity(it, "wifi", teks, id = System.currentTimeMillis())
            if(isServer){
                printerManager.addToQueue(pendingPrint)
            }else{
                try {
                    val req = Gson().toJson(pendingPrint)
//                    val out = PrintWriter(BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
//                    out.println(req)

                    toast("Sending Data Using ${socketSenderOptions[selectedSetting]}")
                    if(selectedSetting == 0){
                        val dataOut = DataOutputStream(socket?.getOutputStream())
                        dataOut.writeUTF(req)
                    }else if(selectedSetting == 1){
                        val out =  PrintWriter( BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
                        out.println(out)
                    }
                    if(socket?.isConnected == true)toast("Yes,, still connect!")
                    toast("Data sent to server. $req")
                } catch (e: Exception) {
                    toast("Communicate to server fail. "+e.message)
                }
            }
        } ?: kotlin.run {
            toast("Please choose printer!!!")
        }
    }

    private fun createReceipt(): String {
        val sb = StringBuilder()
        sb.append("========== UNIQ POS TEST ==========\n")
        sb.append("Pelanggan  : Painah\n")
        sb.append("Meja       : 1\n")
        sb.append("Kasir      : Verdi\n")
        sb.append("Tanggal    : 10-02-2018 12:30\n\n")
        sb.append("Yamie Asin Sedang   3   45.000\n")
        sb.append("Yamie Manis Sedang  3   45.000\n")
        sb.append("Yamie Asin Komplit  3   62.000\n")
        sb.append("Es Teh              6   36.000\n")
        sb.append("Es Jeruk            3   15.000\n")
        sb.append("------------------------------\n")
        sb.append("         Grand Total : 230.000\n")
        sb.append("         Cash        : 250.000\n")
        sb.append("         Kembali     :  20.000\n\n")
        sb.append("         Powered By UNIQ")

        return sb.toString()
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
        this.isServer = isServer
        edt_ipserver.isEnabled = !isServer
        btn_ping.isEnabled = !isServer
    }

    fun showIPAddress(ip: String){
        txt_ip.text = "Socket Address : $ip"
    }

    fun toast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("xxxx Req Code $requestCode")
        println("RESUL : $resultCode | ${Activity.RESULT_OK}")
        if(requestCode == 25 && resultCode == Activity.RESULT_OK){
            printerTarget = data?.getStringExtra("target")
            ip_address.text = "selected printer : $printerTarget"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        AlertDialog.Builder(this)
                .setSingleChoiceItems(socketSenderOptions, selectedSetting, { dialog, position ->
                    selectedSetting = position
                    dialog.dismiss()
                })
                .setTitle("Choose Socket Sender")
                .show()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
