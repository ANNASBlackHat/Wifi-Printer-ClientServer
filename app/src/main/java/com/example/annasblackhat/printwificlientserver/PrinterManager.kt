package com.example.annasblackhat.printwificlientserver

import android.content.Context

/**
 * Created by annasblackhat on 11/02/18.
 */
class PrinterManager(val context: Context) {

    private var isPrinting = false
    private val printerWifi = PrinterWifiUtil()

    fun addToQueue(pendingPrintEntity: PendingPrintEntity){
        printerWifi.pendingPrint.add(pendingPrintEntity)
        if(!isPrinting)printWifi()
    }

    fun printWifi(index: Int = 0){
        if(index < printerWifi.pendingPrint.size){
            isPrinting = true
            val data = printerWifi.pendingPrint[index]
            printerWifi.printWifi(data.address, context, data.dataString ?: "", listener = object : PrinterWifiUtil.PrinterWifiListener{
                override fun onFinish(status: Boolean) {
                    if(status){
                        printWifi(index)
                    }else{
                        printWifi(index+1)
                    }
                }
            }, printerName = data.printerName)
        }else{
            isPrinting = false
        }
    }
}