package dev.spaceapps.secvault

import android.app.Application
import dev.spaceapps.secvault.core.data.security.CurrentActivity
import dev.spaceapps.secvault.core.data.di.androidDataModule

class SecureVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CurrentActivity.register(this)
        initKoin(androidDataModule(this))
    }
}
