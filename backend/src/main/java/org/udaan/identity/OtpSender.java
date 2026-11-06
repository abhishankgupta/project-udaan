package org.udaan.identity;

/**
 * Delivers a one-time code by SMS. Implementations: {@link LoggingOtpSender} until an SMS provider
 * (MSG91 or Twilio, with TRAI DLT registration) is connected.
 */
interface OtpSender {

    /** True when codes actually reach the phone; false for the logging stand-in. */
    boolean deliversToPhone();

    void send(String phoneE164, String code);
}
