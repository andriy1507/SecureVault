package dev.spaceapps.secvault.core.presentation.mvi

/** User actions sent from the View to the ViewModel. */
interface UiIntent

/** Everything the screen renders. Immutable. */
interface UiState

/** One-off events (navigation, messages). */
interface UiEffect
