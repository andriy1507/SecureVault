package dev.spaceapps.secvault

import dev.spaceapps.secvault.di.appModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/** Called once per process by each platform with its platform module. */
fun initKoin(platformModule: Module) {
    startKoin { modules(platformModule, appModule) }
}
