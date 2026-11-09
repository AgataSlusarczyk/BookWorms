package com.bookworms.app.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookworms.app.R

// The name of a genre code from the server, e.g. "SCI_FI" → "science fiction".
// null for a code the app doesn't know yet (a newer server), so the screen just leaves it out.
@Composable
fun genreName(code: String): String? {
    val name = when (code) {
        "FANTASY" -> R.string.genre_fantasy
        "SCI_FI" -> R.string.genre_sci_fi
        "CRIME" -> R.string.genre_crime
        "THRILLER" -> R.string.genre_thriller
        "HORROR" -> R.string.genre_horror
        "ROMANCE" -> R.string.genre_romance
        "HISTORICAL_FICTION" -> R.string.genre_historical_fiction
        "LITERARY_FICTION" -> R.string.genre_literary_fiction
        "CLASSICS" -> R.string.genre_classics
        "POETRY" -> R.string.genre_poetry
        "DRAMA" -> R.string.genre_drama
        "BIOGRAPHY" -> R.string.genre_biography
        "HISTORY" -> R.string.genre_history
        "REPORTAGE" -> R.string.genre_reportage
        "SCIENCE" -> R.string.genre_science
        "PHILOSOPHY" -> R.string.genre_philosophy
        "PSYCHOLOGY" -> R.string.genre_psychology
        "BUSINESS" -> R.string.genre_business
        "SELF_HELP" -> R.string.genre_self_help
        "TRAVEL" -> R.string.genre_travel
        "CHILDREN" -> R.string.genre_children
        "YOUNG_ADULT" -> R.string.genre_young_adult
        "COMICS" -> R.string.genre_comics
        "OTHER" -> R.string.genre_other
        else -> null
    }
    return name?.let { stringResource(it) }
}
