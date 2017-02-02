package com.ogsdroid

import com.ogs.OGS
import java.util.concurrent.atomic.AtomicInteger

object Globals {
    private val ogs: OGS = OGS("82ff83f2631a55273c31", "cd42d95fd978348d57dc909a9aecd68d36b17bd2")
    private val refCount = AtomicInteger()

    // Returns the OGS object. The caller must putOGS() when finished with the
    // object.
    fun getOGS(): OGS {
	    refCount.incrementAndGet()
	    println("NJ getOGS: refCount = ${refCount.get()}")
	    return ogs
    }

    // Closes the OGS socket if this is the last reference.
    fun putOGS() {
	    println("NJ putOGS: refCount = ${refCount.get()}")
	    if (refCount.decrementAndGet() == 0) {
		    println("NJ putOGS: closing socket")
		    ogs.closeSocket()
	    }
    }
}
