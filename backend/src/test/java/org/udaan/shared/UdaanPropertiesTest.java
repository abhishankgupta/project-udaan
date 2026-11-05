package org.udaan.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UdaanPropertiesTest {

    @Test
    void fillsSafeDefaultsWhenUnset() {
        UdaanProperties props = new UdaanProperties(null, " ", null);
        assertThat(props.environment()).isEqualTo("local");
        assertThat(props.version()).isEqualTo("dev");
        assertThat(props.corsOrigins()).isEmpty();
    }

    @Test
    void keepsGivenValues() {
        UdaanProperties props = new UdaanProperties("test", "abc123", List.of("https://test.udaan.pages.dev"));
        assertThat(props.environment()).isEqualTo("test");
        assertThat(props.version()).isEqualTo("abc123");
        assertThat(props.corsOrigins()).containsExactly("https://test.udaan.pages.dev");
    }
}
