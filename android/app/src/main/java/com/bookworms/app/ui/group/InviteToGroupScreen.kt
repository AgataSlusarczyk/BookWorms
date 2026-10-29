package com.bookworms.app.ui.group

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.CloseTitle
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun InviteToGroupScreen(
    invite: GroupInvite,
    recentMembers: List<RecentMember>,
    onCopyCodeClick: () -> Unit,
    onShareLinkClick: () -> Unit,
    onGenerateNewCodeClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        CloseTitle(
            title = stringResource(R.string.invite_title),
            subtitle = invite.groupName,
            onCloseClick = onCloseClick,
        )

        Spacer(Modifier.height(16.dp))

        InviteCodeCard(invite)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCopyCodeClick,
                shape = FieldShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                ButtonIcon(R.drawable.ic_copy)
                Text(stringResource(R.string.copy_code))
            }
            Button(
                onClick = onShareLinkClick,
                shape = FieldShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                ButtonIcon(R.drawable.ic_share)
                Text(stringResource(R.string.share_link))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(R.string.recently_joined),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(8.dp))

        // weight(1f) = take the space that's left, so the button below stays at the bottom.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(recentMembers) { member ->
                MemberCard(member)
            }
        }

        TextButton(
            onClick = onGenerateNewCodeClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.generate_new_code))
        }
    }
}

// The big code in the middle, with how long and how many times it can be used.
@Composable
private fun InviteCodeCard(invite: GroupInvite) {
    OutlinedCard(
        shape = FieldShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.invite_code_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = invite.code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.invite_validity,
                    pluralStringResource(R.plurals.invite_days_valid, invite.daysValid, invite.daysValid),
                    pluralStringResource(R.plurals.invite_uses_left, invite.usesLeft, invite.usesLeft),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Small icon before a button's text. No description, because the text already says what it does.
@Composable
private fun ButtonIcon(@DrawableRes icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(8.dp))
}

@Composable
private fun MemberCard(member: RecentMember) {
    OutlinedCard(
        shape = FieldShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            AvatarPlaceholder()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = member.joined,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(text = stringResource(R.string.member_role))
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun InviteToGroupScreenPreview() {
    BookWormsTheme {
        InviteToGroupScreen(
            invite = sampleInvite,
            recentMembers = sampleRecentMembers,
            onCopyCodeClick = {},
            onShareLinkClick = {},
            onGenerateNewCodeClick = {},
            onCloseClick = {},
        )
    }
}
