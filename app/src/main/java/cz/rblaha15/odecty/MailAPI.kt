package cz.rblaha15.odecty

import java.io.File
import javax.mail.MessagingException

internal interface MailAPI {

    fun sendEmail(
        body: String,
        to: List<String>,
        subject: String,
        attachments: List<File> = emptyList(),
        onError: (MessagingException) -> Unit = {},
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        onSend: () -> Unit = {},
    )
}