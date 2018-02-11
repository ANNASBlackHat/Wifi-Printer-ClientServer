package com.example.annasblackhat.printwificlientserver

/**
 * Created by annasblackhat on 04/02/18.
 */

data class PendingPrintEntity(
        var address: String,
        var type: String? = "",
        var dataString: String? = null,
        var printerName: String? = "",
        var id: Long = 0
)