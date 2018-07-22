package com.robj.firestore_utils

import java.util.*

/**
 * Created by Rob J on 21/09/17.
 */

class Optional<T>(private val optional: T?) {

    val isEmpty: Boolean
        get() = this.optional == null

    fun get(): T {
        if (optional == null)
            throw NoSuchElementException("Item was null..")
        return optional
    }
}
