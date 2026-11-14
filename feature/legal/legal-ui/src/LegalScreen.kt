package dev.spaceapps.secvault.feature.legal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.legal.resources.Res
import dev.spaceapps.secvault.feature.legal.resources.legal_last_updated
import dev.spaceapps.secvault.feature.legal.resources.legal_updated_date
import dev.spaceapps.secvault.feature.legal.resources.privacy_title
import dev.spaceapps.secvault.feature.legal.resources.terms_title
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_1
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_2
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_3
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_4
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_5
import dev.spaceapps.secvault.feature.legal.resources.terms_heading_6
import dev.spaceapps.secvault.feature.legal.resources.terms_body_1
import dev.spaceapps.secvault.feature.legal.resources.terms_body_2
import dev.spaceapps.secvault.feature.legal.resources.terms_body_3
import dev.spaceapps.secvault.feature.legal.resources.terms_body_4
import dev.spaceapps.secvault.feature.legal.resources.terms_body_5
import dev.spaceapps.secvault.feature.legal.resources.terms_body_6
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_1
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_2
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_3
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_4
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_5
import dev.spaceapps.secvault.feature.legal.resources.privacy_heading_6
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_1
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_2
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_3
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_4
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_5
import dev.spaceapps.secvault.feature.legal.resources.privacy_body_6
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class LegalDocument { Terms, Privacy }

private val TERMS_SECTIONS = listOf(
        Res.string.terms_heading_1 to Res.string.terms_body_1,
        Res.string.terms_heading_2 to Res.string.terms_body_2,
        Res.string.terms_heading_3 to Res.string.terms_body_3,
        Res.string.terms_heading_4 to Res.string.terms_body_4,
        Res.string.terms_heading_5 to Res.string.terms_body_5,
        Res.string.terms_heading_6 to Res.string.terms_body_6,
)

private val PRIVACY_SECTIONS = listOf(
        Res.string.privacy_heading_1 to Res.string.privacy_body_1,
        Res.string.privacy_heading_2 to Res.string.privacy_body_2,
        Res.string.privacy_heading_3 to Res.string.privacy_body_3,
        Res.string.privacy_heading_4 to Res.string.privacy_body_4,
        Res.string.privacy_heading_5 to Res.string.privacy_body_5,
        Res.string.privacy_heading_6 to Res.string.privacy_body_6,
)

/** Terms of Service / Privacy Policy (placeholder text until the real documents exist). */
@Composable
fun LegalScreen(document: LegalDocument, onBack: () -> Unit) {
    val title = stringResource(if (document == LegalDocument.Terms) Res.string.terms_title else Res.string.privacy_title)
    val icon = if (document == LegalDocument.Terms) Icons.Outlined.Gavel else Icons.Outlined.Policy
    val sections: List<Pair<StringResource, StringResource>> =
        if (document == LegalDocument.Terms) TERMS_SECTIONS else PRIVACY_SECTIONS
    Scaffold(topBar = { AppTopBar(title, navIcon = NavIcon.Back, onNavigate = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.dp16), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(Res.string.legal_last_updated, stringResource(Res.string.legal_updated_date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            sections.forEach { (heading, body) ->
                item {
                    Text(stringResource(heading), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Dimens.dp8))
                }
                item {
                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Row(Modifier.padding(bottom = Dimens.dp24)) {} }
        }
    }
}
