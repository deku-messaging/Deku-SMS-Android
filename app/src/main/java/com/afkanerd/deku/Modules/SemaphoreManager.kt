package com.afkanerd.deku.Modules

import kotlinx.coroutines.sync.Semaphore

object SemaphoreManager {
    val resourceSemaphore = Semaphore(1)
}