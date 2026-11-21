package com.example

import android.app.Application
import com.example.security.AppIntegrityGuard

/**
 * Custom Application entry point executing lightweight release-only integrity verification.
 */
class RafiqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppIntegrityGuard.verifyReleaseIntegrity(this)
    }
}
