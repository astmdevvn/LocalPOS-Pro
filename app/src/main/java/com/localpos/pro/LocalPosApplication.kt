package com.localpos.pro

import android.app.Application
import com.localpos.pro.data.LocalPosDatabase
import com.localpos.pro.data.PosRepository

class LocalPosApplication : Application() {
    val database by lazy { LocalPosDatabase.create(this) }
    val repository by lazy { PosRepository(database) }
}
