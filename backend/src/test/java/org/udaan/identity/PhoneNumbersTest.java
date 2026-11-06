package org.udaan.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void acceptsCommonIndianFormats() {
        assertThat(PhoneNumbers.normalizeIndianMobile("9876543210")).contains("+919876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("+91 98765 43210")).contains("+919876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("09876543210")).contains("+919876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("919876543210")).contains("+919876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("0091-98765-43210")).contains("+919876543210");
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThat(PhoneNumbers.normalizeIndianMobile("12345")).isEmpty();
        assertThat(PhoneNumbers.normalizeIndianMobile("5876543210")).isEmpty();
        assertThat(PhoneNumbers.normalizeIndianMobile("98765432101")).isEmpty();
        assertThat(PhoneNumbers.normalizeIndianMobile(null)).isEmpty();
    }

    @Test
    void masksAllButLastFourDigits() {
        assertThat(PhoneNumbers.masked("+919876543210")).isEqualTo("+91 ••••• •3210");
    }
}
