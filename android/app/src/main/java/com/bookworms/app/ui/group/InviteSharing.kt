package com.bookworms.app.ui.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.bookworms.app.R

// Puts the invite code on the clipboard, so the user can paste it into any chat.
fun copyInviteCode(context: Context, code: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.invite_code_label), code))
    // Android 13 and newer show their own "Copied" message, so we only show ours on older phones.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.code_copied, Toast.LENGTH_SHORT).show()
    }
}

// Opens the phone's share menu (Messenger, SMS, email…) with the invite link.
fun shareInviteLink(context: Context, invite: GroupInvite) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invite_share_text, invite.groupName, invite.link))
    }
    context.startActivity(Intent.createChooser(send, null))
}
