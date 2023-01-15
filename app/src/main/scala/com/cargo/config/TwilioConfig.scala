package com.cargo.config

final case class TwilioConfig(
    from: String,
    accountSid: String,
    authToken: String
)
