package dev.spaceapps.secvault.autofill

import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import dev.spaceapps.secvault.R

/** Builds a suggestion with the Android 13+ API, falling back to the older one on 11-12. */
internal object Datasets {
    /**
     * A value of null means "filled after [authentication]".
     * The suggestion row uses the app's look (R.layout.autofill_dataset) so it reads as Vault, not the system.
     */
    fun build(
        context: Context,
        label: String,
        values: List<Pair<AutofillId, AutofillValue?>>,
        authentication: IntentSender? = null,
        subtitle: String? = null,
        @DrawableRes icon: Int = R.drawable.ic_autofill_shield,
    ): Dataset {
        val view = RemoteViews(context.packageName, R.layout.autofill_dataset).apply {
            setTextViewText(R.id.autofill_title, label)
            setImageViewResource(R.id.autofill_icon, icon)
            if (subtitle.isNullOrBlank()) {
                setViewVisibility(R.id.autofill_subtitle, View.GONE)
            } else {
                setTextViewText(R.id.autofill_subtitle, subtitle)
            }
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dataset.Builder(Presentations.Builder().setMenuPresentation(view).build()).apply {
                values.forEach { (id, value) -> setField(id, Field.Builder().apply { value?.let(::setValue) }.build()) }
            }
        } else {
            @Suppress("DEPRECATION")
            Dataset.Builder(view).apply { values.forEach { (id, value) -> setValue(id, value) } }
        }
        authentication?.let(builder::setAuthentication)
        return builder.build()
    }
}
