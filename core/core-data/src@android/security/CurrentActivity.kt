package dev.spaceapps.secvault.core.data.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.lang.ref.WeakReference

/**
 * BiometricPrompt needs a resumed FragmentActivity. Tracks the one in the foreground through
 * activity lifecycle callbacks, so it stays correct across the main app, the autofill screens
 * and configuration changes. Register once from Application.onCreate.
 */
object CurrentActivity : Application.ActivityLifecycleCallbacks {
    private val resumed = MutableStateFlow<WeakReference<FragmentActivity>?>(null)

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * The foreground activity; suspends while the app is in the background (e.g. auto-lock fired
     * there), so the unlock prompt appears as soon as the user comes back instead of failing.
     */
    internal suspend fun awaitResumed(): FragmentActivity = resumed.mapNotNull { it?.get() }.first()

    override fun onActivityResumed(activity: Activity) {
        if (activity is FragmentActivity) resumed.value = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed.value?.get() === activity) resumed.value = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (resumed.value?.get() === activity) resumed.value = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
