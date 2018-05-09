package com.example.annasblackhat.printwificlientserver


import android.widget.Toast
import com.google.gson.Gson
import java.io.*
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Created by annasblackhat on 11/02/18.
 */
class Server(val mainActivity: MainActivity, val printerManager: PrinterManager) {

    companion object {
        val socketServerPORT = 2893
    }

    private var serverSocket: ServerSocket? = null

    fun start(){
        val socketServerThread = Thread(SocketServerThread())
        socketServerThread.start()
    }

    inner class SocketServerThread : Thread() {
        var count = 0
        override fun run() {
            mainActivity.runOnUiThread{ mainActivity.toast("Starting...") }

            try {
                serverSocket = ServerSocket(socketServerPORT)
            } catch (e: Exception) {
                mainActivity.runOnUiThread{ mainActivity.toast("Initialize socket error. "+e.message) }
            }

            try {
                while (true){
                    val ipAddress = getIpAddress()
                    mainActivity.runOnUiThread { mainActivity.showIPAddress(ipAddress) }
                    val socket = serverSocket?.accept()
                    count++

                    println("xxx IP Address : $ipAddress")
                    mainActivity.runOnUiThread{ mainActivity.toast("$count Client Connected!") }

//                    SocketCommunicationThread(socket, count).start()

                    val input = DataInputStream(socket?.getInputStream())
                    thread(true){
                        while (true){
                            val msgClient = input.readUTF()
                            println("FROM CLIENT : $msgClient")
                            try {
                                val pendingPrint = Gson().fromJson(msgClient, PendingPrintEntity::class.java)
                                printerManager.addToQueue(pendingPrint)
                            } catch (e: Exception) {
                                println("Parse error. $e")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                mainActivity.runOnUiThread{ mainActivity.toast("Error. "+e.message) }
            }
        }
    }
//
//    inner class SocketServerReplyThread(val socket: Socket?, val count: Int): Thread(){
//        override fun run() {
//            var outputStream: OutputStream?
//            var msgReply = "Hello #$count You are connected to ${Build.BRAND} ${Build.MODEL}"
//            println("Try sending reply..")
//            try {
//                outputStream = socket?.getOutputStream()
//                val printStream = PrintStream(outputStream)
//                printStream.print(msgReply)
//                printStream.close()
//                println("Reply sent...")
//            } catch (e: Exception) {
//                mainActivity.runOnUiThread { mainActivity.toast("Sending reply Error. "+e.message) }
//            }
//
//        }
//    }

    inner class SocketCommunicationThread(val socket: Socket?, val count: Int): Thread(){
        override fun run() {
            //sending message to new connected device
//            var outputStream: OutputStream?
//            var msgReply = "Hello #$count You are connected to ${Build.BRAND} ${Build.MODEL}"
//            println("Try sending reply..")
//            try {
//                outputStream = socket?.getOutputStream()
//                val printStream = PrintStream(outputStream)
//                printStream.print(msgReply)
//                printStream.close()
//                println("Reply sent...")
//            } catch (e: Exception) {
//                mainActivity.runOnUiThread { mainActivity.toast("Sending reply Error. "+e.message) }
//            }


            val input = BufferedReader(InputStreamReader(socket?.getInputStream()))
            while (true){
                try {
                    val request = input.readLine()
                    println("From client : $request")
                    if(request != null && request.isNotEmpty()){
                        mainActivity.runOnUiThread { mainActivity.toast("Get data from client! ($request)") }
                        val pendingPrint = Gson().fromJson(input.readLine(), PendingPrintEntity::class.java)
                        printerManager.addToQueue(pendingPrint)
                    }
                } catch (e: Exception) {
                    println("Parsing error. $e")
                }
            }
        }
    }

    fun getIpAddress(): String{
        var ip = "not found"
        try {
            val enumNetworkInterface = NetworkInterface.getNetworkInterfaces()
            while (enumNetworkInterface.hasMoreElements()){
                val networkInterface = enumNetworkInterface.nextElement()
                val enumInetAddress = networkInterface.inetAddresses
                while (enumInetAddress.hasMoreElements()){
                    val inetAddress = enumInetAddress.nextElement()

                    if(inetAddress.isSiteLocalAddress){
                        ip = inetAddress.hostAddress
                    }
                }
            }
        }catch (e: Exception){
            Toast.makeText(mainActivity.applicationContext, "Get IP address error. "+e.message, Toast.LENGTH_SHORT).show()
        }
        return ip
    }

    fun onDestroy(){
        serverSocket?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Toast.makeText(mainActivity.applicationContext, "Close socket error. "+e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}