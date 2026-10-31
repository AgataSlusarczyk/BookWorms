package com.bookworms.app.ui.book

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.theme.StarYellow

const val MaxRating = 5

// Five small stars, the first `rating` of them filled. Only for showing – they can't be tapped.
@Composable
fun StarRow(rating: Int, modifier: Modifier = Modifier, starSize: Dp = 14.dp) {
    val description = pluralStringResource(R.plurals.stars_rating, rating, rating)
    // Screen readers say "4 of 5 stars" once instead of reading five separate icons.
    Row(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        for (star in 1..MaxRating) {
            Icon(
                painter = painterResource(if (star <= rating) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = null,
                tint = if (star <= rating) StarYellow else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

// Five big stars to tap. Tapping the star that's already picked takes the rating away again,
// because the rating is optional.
@Composable
fun StarRatingPicker(rating: Int?, onRatingChange: (Int?) -> Unit) {
    Row {
        for (star in 1..MaxRating) {
            val filled = rating != null && star <= rating
            IconButton(onClick = { onRatingChange(if (rating == star) null else star) }) {
                Icon(
                    painter = painterResource(if (filled) R.drawable.ic_star else R.drawable.ic_star_border),
                    contentDescription = pluralStringResource(R.plurals.stars_rating, star, star),
                    tint = if (filled) StarYellow else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
