package com.cargo.service

import com.cargo.config.SendGridConfig
import com.cargo.error.ApplicationError.IntegrationError
import zio._
import com.sendgrid._
import com.sendgrid.helpers.mail.objects.Content

trait EmailNotification {
  def sendVerificationEmail(addressee: String, code: String): IO[IntegrationError, Unit]
}

object EmailNotification {
  final case class SendgridEmailNotification(config: SendGridConfig, sendGrid: SendGrid)
      extends EmailNotification {
    override def sendVerificationEmail(
        addressee: String,
        code: String
    ): IO[IntegrationError, Unit] = {
      import com.sendgrid.helpers.mail.Mail
      import com.sendgrid.helpers.mail.objects.Email

      val from = new Email(config.sender)
      val to = new Email(addressee)
      val mail = new Mail(
        from,
        "Verification code",
        to,
        new Content("text/plain", s"verification code: $code")
      )

      val request = new Request
      for {
        _ <- ZIO.logInfo(s"Sending mail to ${to.getEmail}")
        sendResponse <- ZIO.succeed {
          request.setMethod(Method.POST)
          request.setEndpoint("mail/send")
          request.setBody(mail.build)
          sendGrid.api(request)
        }
        _ <-
          ZIO
            .fail(IntegrationError("failed to send an email"))
            .unless(sendResponse.getStatusCode / 100 == 2)
       _ <- ZIO.logInfo("Successfully sent email")
      } yield ()
    }

  }
  val live = ZLayer.fromFunction(SendgridEmailNotification.apply _)
}
