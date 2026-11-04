package com.bookworms.app.ui.book

import android.content.Context
import android.content.Intent
import com.bookworms.app.R

// Opens the phone's share menu (Messenger, SMS, email…) with the book's title and author.
fun shareBook(context: Context, book: BookDetails) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_book_text, book.title, book.author))
    }
    context.startActivity(Intent.createChooser(send, null))
}
