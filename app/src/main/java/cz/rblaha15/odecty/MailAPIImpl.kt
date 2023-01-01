package cz.rblaha15.odecty

import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

internal class MailAPIImpl : MailAPI {

    override fun sendEmail(
        body: String,
        to: List<String>,
        subject: String,
        attachments: List<File>,
        onError: (MessagingException) -> Unit,
        cc: List<String>,
        bcc: List<String>,
        onSend: () -> Unit
    ) {

        Executors.newSingleThreadExecutor().execute {

            val props = System.getProperties()
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.socketFactory.port"] = "465"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.port"] = "465"

            val session = Session.getInstance(props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(Credentials.EMAIL, Credentials.PASSWORD)
                    }
                })

            try {

                MimeMessage(session).apply {

                    setFrom(InternetAddress(Credentials.EMAIL))

                    to.forEach { to ->
                        addRecipient(
                            Message.RecipientType.TO,
                            InternetAddress(to)
                        )
                    }
                    cc.forEach { cc ->
                        addRecipient(
                            Message.RecipientType.CC,
                            InternetAddress(cc)
                        )
                    }
                    bcc.forEach { bcc ->
                        addRecipient(
                            Message.RecipientType.BCC,
                            InternetAddress(bcc)
                        )
                    }

                    this.subject = subject

                    MimeMultipart().apply {

                        MimeBodyPart().apply {
                            setText(body)
                            addBodyPart(this)
                        }
                        attachments.forEach { file ->
                            MimeBodyPart().apply {
                                dataHandler = DataHandler(FileDataSource(file))
                                fileName = file.name
                                addBodyPart(this)
                            }
                        }
                        setContent(this)
                    }
                    Transport.send(this)
                }

                onSend()

                Log.i("email", "odeslano")

            } catch (e: MessagingException) {
                e.printStackTrace()

                Log.e("email", "CHYBA", e)

                onError(e)
            }
        }
    }
}