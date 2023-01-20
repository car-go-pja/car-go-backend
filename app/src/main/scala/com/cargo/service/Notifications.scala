package com.cargo.service

import com.cargo.config.{SendGridConfig, TwilioConfig}
import com.cargo.error.ApplicationError.IntegrationError
import zio._
import com.sendgrid._
import com.sendgrid.helpers.mail.objects.Content
import com.twilio.`type`.PhoneNumber
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Email
import com.twilio.http.TwilioRestClient
import com.twilio.rest.api.v2010.account.Message

trait Notifications {
  def sendVerificationEmail(addressee: String, code: String): IO[IntegrationError, Unit]
  def sendSms(to: String, msg: String): IO[IntegrationError, Unit]
  def sendResetLink(addressee: String, code: String): IO[IntegrationError, Unit]
}

object Notifications {
  final case class NotificationsLive(
      sendgridCfg: SendGridConfig,
      sendGrid: SendGrid,
      twilioConfig: TwilioConfig,
      twilio: TwilioRestClient
  ) extends Notifications {
    override def sendVerificationEmail(
        addressee: String,
        code: String
    ): IO[IntegrationError, Unit] = {
      val from = new Email(sendgridCfg.sender)
      val to = new Email(addressee)
      val mail = new Mail(
        from,
        "Verification code",
        to,
        new Content("text/plain", s"verification code: $code")
      )

      val request = new Request
      for {
        _ <- ZIO.logInfo(s"Sending email to ${to.getEmail}")
        sendResponse <- ZIO.succeed {
          request.setMethod(Method.POST)
          request.setEndpoint("mail/send")
          request.setBody(mail.build)
          sendGrid.api(request)
        }
        _ <-
          ZIO
            .fail(IntegrationError(s"failed to send an email ${sendResponse.getBody}"))
            .unless(sendResponse.getStatusCode / 100 == 2)
        _ <- ZIO.logInfo("Successfully sent email")
      } yield ()
    }

    override def sendSms(to: String, msg: String): IO[IntegrationError, Unit] = {
      val message =
        Message.creator(new PhoneNumber(s"+48$to"), new PhoneNumber(twilioConfig.from), msg)

      ZIO
        .attempt(message.create(twilio))
        .mapError(e => IntegrationError(e.getMessage))
        .unit
    }

    override def sendResetLink(addressee: String, code: String): IO[IntegrationError, Unit] = {
      val from = new Email(sendgridCfg.sender)
      val to = new Email(addressee)
      val mail = new Mail(
        from,
        "Reset password",
        to,
        new Content("text/plain", s"reset password link : https://car-go-frontend.vercel.app/reset?code=$code")
      )

      val request = new Request
      for {
        _ <- ZIO.logInfo(s"Sending email to ${to.getEmail}")
        sendResponse <- ZIO.succeed {
          request.setMethod(Method.POST)
          request.setEndpoint("mail/send")
          request.setBody(mail.build)
          sendGrid.api(request)
        }
        _ <-
          ZIO
            .fail(IntegrationError(s"failed to send an email ${sendResponse.getBody}"))
            .unless(sendResponse.getStatusCode / 100 == 2)
        _ <- ZIO.logInfo("Successfully sent email")
      } yield ()
    }
  }
  val live = ZLayer.fromFunction(NotificationsLive.apply _)
}
