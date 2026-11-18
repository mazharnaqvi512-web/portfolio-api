package com.mazhar.portfolio.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClientAddressKeyTest {

    @Test
    void anIpv4AddressIsItsOwnKey() {
        assertThat(ClientAddressKey.of("203.0.113.9")).isEqualTo("203.0.113.9");
        assertThat(ClientAddressKey.of("203.0.113.10")).isNotEqualTo(ClientAddressKey.of("203.0.113.9"));
    }

    @Test
    void ipv6AddressesInTheSame64ShareOneKey() {
        String a = ClientAddressKey.of("2001:db8:1:2::1");
        assertThat(ClientAddressKey.of("2001:db8:1:2::2")).isEqualTo(a);
        assertThat(ClientAddressKey.of("2001:db8:1:2:aaaa:bbbb:cccc:dddd")).isEqualTo(a);
        assertThat(ClientAddressKey.of("2001:0db8:0001:0002:0000:0000:0000:0001")).isEqualTo(a);
    }

    @Test
    void ipv6AddressesInDifferent64sGetDifferentKeys() {
        assertThat(ClientAddressKey.of("2001:db8:1:3::1")).isNotEqualTo(ClientAddressKey.of("2001:db8:1:2::1"));
        assertThat(ClientAddressKey.of("2001:db9:1:2::1")).isNotEqualTo(ClientAddressKey.of("2001:db8:1:2::1"));
    }

    @Test
    void anIpv6KeyCannotCollideWithAnIpv4Key() {
        assertThat(ClientAddressKey.of("2001:db8:1:2::1")).endsWith("::/64").doesNotMatch("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }

    @Test
    void anIpv4AddressWrittenInIpv6FormIsTreatedAsTheIpv4Address() {
        assertThat(ClientAddressKey.of("::ffff:203.0.113.9")).isEqualTo("203.0.113.9");
    }

    @Test
    void loopbackAndScopedAddressesDoNotBlowUp() {
        assertThat(ClientAddressKey.of("0:0:0:0:0:0:0:1")).endsWith("::/64");
        assertThat(ClientAddressKey.of("fe80::1%eth0")).endsWith("::/64");
    }

    @Test
    void somethingThatIsNotAnAddressIsKeptAsIs() {
        assertThat(ClientAddressKey.of("not:an:address")).isEqualTo("not:an:address");
        assertThat(ClientAddressKey.of(null)).isNull();
        assertThat(ClientAddressKey.of("")).isEmpty();
    }
}
