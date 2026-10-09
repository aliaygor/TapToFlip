package com.aliaygor.taptoflip

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class TapToFlipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Unconfigured builds remain fully usable as offline/guest games.
        if ((getString(R.string.game_services_project_id).trim().toLongOrNull() ?: 0L) > 0L) {
            PlayGamesSdk.initialize(this)
        }
    }
}
