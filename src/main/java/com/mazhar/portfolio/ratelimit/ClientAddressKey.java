package com.mazhar.portfolio.ratelimit;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;

/**
 * Turns a client's address into the key its rate limit is counted under.
 *
 * <p>An IPv4 address is its own key. An IPv6 client normally controls a whole /64 (billions of addresses), so
 * keying on the full address would let it dodge any per-address limit by sending each request from a new one;
 * IPv6 addresses are therefore keyed by their /64 prefix. An IPv4 address written in IPv6 form
 * ({@code ::ffff:1.2.3.4}) is treated as the IPv4 address it is.
 */
final class ClientAddressKey {

    private static final int PREFIX_BYTES = 8; // 64 bits

    private ClientAddressKey() {}

    static String of(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.indexOf(':') < 0) {
            return remoteAddress; // IPv4 (or unknown): the address itself
        }
        // A link-local address carries its interface ("fe80::1%eth0"), which Java refuses to parse when that
        // interface does not exist on this machine, and which says nothing about who the client is.
        String literal = remoteAddress.contains("%") ? remoteAddress.substring(0, remoteAddress.indexOf('%')) : remoteAddress;
        try {
            // A string containing ':' is parsed as an IPv6 literal; no DNS lookup happens.
            InetAddress address = InetAddress.getByName(literal);
            if (address instanceof Inet6Address) {
                return HexFormat.of().formatHex(address.getAddress(), 0, PREFIX_BYTES) + "::/64";
            }
            return address.getHostAddress(); // an IPv4-mapped IPv6 address resolves to its IPv4 form
        } catch (UnknownHostException notAnAddress) {
            return remoteAddress;
        }
    }
}
