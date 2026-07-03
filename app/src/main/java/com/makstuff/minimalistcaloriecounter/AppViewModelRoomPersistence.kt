package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.persistence.room.AppRoomStore
import kotlinx.coroutines.launch

internal fun AppViewModelEnvironment.launchRoomWrite(block: suspend AppRoomStore.() -> Unit) {
    scope.launch {
        runCatching {
            roomStore.block()
        }
    }
}
