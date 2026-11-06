package org.udaan.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in sender: records that a code was issued, without the code itself.
 * Replace this bean with a real SMS sender when a provider is connected.
 */
@Component
class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public boolean deliversToPhone() {
        return false;
    }

    @Override
    public void send(String phoneE164, String code) {
        log.info("One-time code issued for {} (no SMS provider connected)", PhoneNumbers.masked(phoneE164));
    }
}
