package com.example.annasblackhat.printwificlientserver

import android.content.Context
import android.graphics.BitmapFactory
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.Epos2Exception
import kotlin.concurrent.thread


/**
 * Created by annasblackhat on 02/02/18.
 */
class PrinterWifiUtil : ReceiveListener {

    private var mPrinter: Printer? = null
    private var mContext: Context? = null
    var pendingPrint = ArrayList<PendingPrintEntity>()
    private var ipPrinter: String = ""
    private var printerWifiListener: PrinterWifiListener? = null
    private var printerName: String? = ""
    private var isDataSent = false

    fun printWifi(ipAddress: String, context: Context, vararg data: String, listener: PrinterWifiListener? = null, id: Long = 0, printerName: String? = ""): Boolean{
        mContext = context
        ipPrinter = ipAddress
        printerWifiListener = listener
        this.printerName = printerName

        if (!initializeObject()) {
            listener?.onFinish(false)
            return false
        }

        if (!createReceiptData(*data)) {
            finalizeObject()
            printerWifiListener?.onFinish(false)
            return false
        }

        if (!printData()) {
            finalizeObject()
            if(id == 0L) { pendingPrint.add(PendingPrintEntity(ipAddress, dataString =  data[0], printerName = printerName, type = "wifi", id = System.currentTimeMillis())) }
            printerWifiListener?.onFinish(false)
            return false
        }

        removePendingPrint(id)
        return true
    }

    private fun removePendingPrint(id: Long) {
        pendingPrint.forEachIndexed { index, item ->
            if(item.id == id) pendingPrint.removeAt(index)
        }
    }

    private fun disconnectPrinter(){
        if (mPrinter == null) {
            mContext?.toast("Printer $printerName is Uninitialized")
            return
        }
        try {
            mPrinter?.endTransaction()
            mContext?.toast("End Transaction $printerName Success...")
        } catch (e: Exception) {
            mContext?.toast("End Transaction $printerName Error. $e")
        }

        try {
            mPrinter?.disconnect()
            mContext?.toast("Disconnect $printerName Success...")
        } catch (e: Exception) {
            mContext?.toast("Disconnect $printerName Failed!")
        }
        finalizeObject()
        printerWifiListener?.onFinish(isDataSent)
    }

    private fun printData(): Boolean {
        if (mPrinter == null) {
            mContext?.toast("Printer $printerName is Uninitialized")
            return false
        }

        if (!connectPrinter()) {
            return false
        }

        val status = mPrinter?.status
        if (!isPrintable(status)) {
            try {
                mContext?.toast("Printer $printerName is not printable. Disconnect...")
                mPrinter?.disconnect()
            } catch (ex: Exception) {
                println("xxx 373 : Oups.. disconnect $printerName error : " + ex.message)
                mContext?.toast("Oups.. Disconect $printerName Error. " + ex.message)
            }
            return false
        }

        try {
            mPrinter?.sendData(Printer.PARAM_DEFAULT)
            mContext?.toast("Data Sent to printer $printerName...")
            isDataSent = true
        } catch (e: Exception) {
            mContext?.toast("Send data error. $e")
            try {
                mPrinter?.disconnect()
            } catch (ex: Exception) {
                println("xxx Yea... Error " + e)
                mContext?.toast("Yea.. Disconect  $printerName Error. " + ex.message)
            }
            return false
        }
        return true
    }

    private fun isPrintable(status: PrinterStatusInfo?): Boolean {
        if (status == null) {
            return false
        }

        if (status.connection == Printer.FALSE) {
            return false
        } else if (status.online == Printer.FALSE) {
            return false
        } else {
        }//print available

        return true
    }

    private fun connectPrinter(): Boolean {
        var isBeginTransaction = false

        if (mPrinter == null) {
            return false
        }
        try {
            mContext?.toast("Connecting to printer $printerName")
            mPrinter?.connect(ipPrinter, Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            mContext?.toast("Connect to printer $printerName failed!")
            return false
        }

        try {
            mPrinter?.beginTransaction()
            isBeginTransaction = true
        } catch (e: Exception) {
            mContext?.toast("Begin transaction error. $e")
        }

        if (isBeginTransaction === false) {
            try {
                mPrinter?.disconnect()
            } catch (e: Epos2Exception) {
                mContext?.toast("OMG.... Disconnect failed! $e")
                return false
            }
        }
        return true
    }

    private fun finalizeObject() {
        if (mPrinter == null) {
            return
        }
        mPrinter?.clearCommandBuffer()
        mPrinter?.setReceiveEventListener(null)
        mPrinter = null
    }

    private fun createReceiptData(vararg data: String): Boolean {
        mPrinter?.addPageBegin()
        data.forEach { mPrinter?.addText(it) }
        mPrinter?.addPageEnd()
//        mPrinter?.addCut(Printer.CUT_FEED)
        return true
    }

    private fun initializeObject(): Boolean {
        try {
            mPrinter = Printer(Printer.TM_U220,
                    Printer.MODEL_ANK,
                    mContext)
        } catch (e: Exception) {
            mContext?.toast("Initialize printer $printerName error. $e")
            return false
        }
        mPrinter?.setReceiveEventListener(this)
        return true
    }

    override fun onPtrReceive(p0: Printer?, p1: Int, p2: PrinterStatusInfo?, p3: String?) {
//        Thread{ Runnable {
//            disconnectPrinter()
//        }}.start()
//        Thread{ disconnectPrinter() }.start()
//        Thread().run { disconnectPrinter() }
        thread(start = true) {
            disconnectPrinter()
        }
    }

    fun printTestWifi(context: Context, ipAddress: String, listener: PrinterWifiListener? = null): Boolean{
        mContext = context
        ipPrinter = ipAddress
        printerWifiListener = listener

        if (!initializeObject()) {
            listener?.onFinish(false)
            return false
        }

        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_black_small)
        mPrinter?.let { printer ->
            printer.addTextAlign(com.epson.epos2.printer.Printer.ALIGN_CENTER)
            printer.addImage(logo, 0, 0,
                    logo.width, logo.height,
                    com.epson.epos2.printer.Printer.COLOR_1,
                    com.epson.epos2.printer.Printer.MODE_MONO,
                    com.epson.epos2.printer.Printer.HALFTONE_DITHER,
                    com.epson.epos2.printer.Printer.PARAM_DEFAULT.toDouble(),
                    com.epson.epos2.printer.Printer.COMPRESS_AUTO)
            printer.addFeedLine(1)
            printer.addText("===== PRINT TEST SUCCESS =====")
            printer.addFeedLine(2)
            printer.addCut(com.epson.epos2.printer.Printer.CUT_FEED)

        }

        if (!printData()) {
            listener?.onFinish(false)
            finalizeObject()
            return false
        }
        listener?.onFinish(false)
        return true
    }

    interface PrinterWifiListener{
        fun onFinish(status: Boolean)
    }
}
