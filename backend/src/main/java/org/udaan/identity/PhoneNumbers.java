package org.udaan.identity;

import java.util.Optional;
import java.util.regex.Pattern;

/** Normalizes Indian mobile numbers to E.164 (+91 followed by 10 digits starting 6–9). */
final class PhoneNumbers {

    private static final Pattern INDIAN_MOBILE = Pattern.compile("[6-9]\\d{9}");

    private PhoneNumbers() {
    }

    static Optional<String> normalizeIndianMobile(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0091")) {
            digits = digits.substring(4);
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return INDIAN_MOBILE.matcher(digits).matches() ? Optional.of("+91" + digits) : Optional.empty();
    }

    /** "+919876543210" → "+91 ••••• •3210", for messages and logs. */
    static String masked(String e164) {
        return e164.substring(0, 3) + " ••••• •" + e164.substring(e164.length() - 4);
    }
}
