/*
        Copyright 2022 worstperson

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

package com.worstperson.tethertproxy;

import android.os.Build;
import android.util.Log;
import com.topjohnwu.superuser.Shell;

public class Script {

    static {
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10));
    }

    static private void shellCommand(String command) {
        for (String message : Shell.cmd(command).exec().getOut()) {
            Log.i("TetherTPROXY", message);
        }
    }

    static boolean hasTPROXY() {
        return Shell.cmd("ip6tables -j TPROXY --help | grep \"TPROXY\"").exec().isSuccess();
    }

    static String getDnsmasqBind() {
        for (String message : Shell.cmd("cat /proc/net/tcp | grep A8C0:0035 | awk '{print $2}'").exec().getOut()) {
            return message;
        }
        return null;
    }

    static String getTetherInterface() {
        String address = getDnsmasqBind();
        if (address != null && address.length() == 13) {
            for (String message : Shell.cmd("cat /proc/net/route | grep " + address.substring(2,8) + " | awk '{print $1}'").exec().getOut()) {
                return message;
            }
        }
        return null;
    }

    static private void configureAddresses(String iface, String ipv4Addr, String ipv6Prefix) {
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        Log.i("TetherTPROXY", "Setting IP addresses");
        //shellCommand("ip address add local " + ipv4Addr + "/24 broadcast " + ipv4Prefix + ".255 scope global dev " + iface);
        shellCommand("ifconfig " + iface + " " + ipv4Addr + " netmask 255.255.255.0 up");
        shellCommand("ip -6 addr add " + ipv6Prefix + "1/64 dev " + iface + " scope global");
    }

    static void configureRoutes(String ipv4Addr, String ipv6Prefix, String iface) {
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        shellCommand("ndc network route add 99 " + iface + " " + ipv4Prefix + ".0/24");
        shellCommand("ndc network route add 99 " + iface + " " + ipv6Prefix + "/64");
    }

    static boolean setTetherAddresses(String iface, String ipv4Addr, String ipv6Prefix) {
        if (!Shell.cmd("ip addr | grep " + iface + " | grep " + ipv4Addr).exec().isSuccess()) {
            configureAddresses(iface, ipv4Addr, ipv6Prefix);
            return true;
        }
        return false;
    }

    static void setTetherRoute(String prefix) {
        if (!Shell.cmd("iptables-save | grep -A TPROXY_ROUTE_PREROUTING -d " + prefix + "::/64 -j RETURN").exec().isSuccess()) {
            shellCommand("ip6tables -t mangle -I TPROXY_ROUTE_PREROUTING -d " + prefix + "::/64 -j RETURN");
        }

    }

    static void configureTPROXY(String ipv4Addr, String ipv6Prefix, Boolean dnsmasq, String appData, boolean dpiCircumvention, boolean dropForward) {
        shellCommand("iptables -t mangle -N TPROXY_ROUTE_PREROUTING");
        shellCommand("iptables -t mangle -A PREROUTING -j TPROXY_ROUTE_PREROUTING");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 0.0.0.0/8 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 10.0.0.0/8 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 127.0.0.0/8 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 169.254.0.0/16 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 172.16.0.0/12 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 192.0.0.0/24 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 192.0.2.0/24 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 192.88.99.0/24 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 192.168.0.0/16 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 198.18.0.0/15 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 198.51.100.0/24 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 203.0.113.0/24 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 224.0.0.0/4 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 240.0.0.0/4 -j RETURN");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -d 255.255.255.255 -j RETURN");
        shellCommand("iptables -t mangle -N TPROXY_MARK_PREROUTING");
        shellCommand("iptables -t mangle -A TPROXY_ROUTE_PREROUTING -j TPROXY_MARK_PREROUTING");
        shellCommand("iptables -t mangle -A TPROXY_MARK_PREROUTING -p tcp -j TPROXY --on-ip 127.0.0.1 --on-port 1088 --tproxy-mark 1088");
        shellCommand("iptables -t mangle -A TPROXY_MARK_PREROUTING -p udp -j TPROXY --on-ip 127.0.0.1 --on-port 1088 --tproxy-mark 1088");
        shellCommand("ip rule add fwmark 1088 table 999");
        shellCommand("ip route add local default dev lo table 999");

        shellCommand("ip6tables -t mangle -N TPROXY_ROUTE_PREROUTING");
        shellCommand("ip6tables -t mangle -A PREROUTING -j TPROXY_ROUTE_PREROUTING");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d :: -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d ::1 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d ::ffff:0:0:0/96 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 64:ff9b::/96 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 100::/64 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 2001::/32 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 2001:20::/28 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 2001:db8::/32 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d 2002::/16 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d fc00::/7 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d fe80::/10 -j RETURN");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -d ff00::/8 -j RETURN");
        shellCommand("ip6tables -t mangle -N TPROXY_MARK_PREROUTING");
        shellCommand("ip6tables -t mangle -A TPROXY_ROUTE_PREROUTING -j TPROXY_MARK_PREROUTING");
        shellCommand("ip6tables -t mangle -A TPROXY_MARK_PREROUTING -p tcp -j TPROXY --on-ip ::1 --on-port 1088 --tproxy-mark 1088");
        shellCommand("ip6tables -t mangle -A TPROXY_MARK_PREROUTING -p udp -j TPROXY --on-ip ::1 --on-port 1088 --tproxy-mark 1088");
        shellCommand("ip -6 rule add fwmark 1088 table 999");
        shellCommand("ip -6 route add local default dev lo table 999");

        //shellCommand(appData + "/hev-socks5-server." + Build.SUPPORTED_ABIS[0] + " " + appData + "/socks.yml &");
        //shellCommand(appData + "/hev-socks5-tproxy." + Build.SUPPORTED_ABIS[0] + " " + appData + "/tproxy.yml &");
        //shellCommand(appData + "/hev-socks5-tproxy." + Build.SUPPORTED_ABIS[0] + " " + appData + "/tproxy4.yml &");

        if (dpiCircumvention) {
            shellCommand("iptables -t mangle -I TPROXY_MARK_PREROUTING -p tcp --dport 80 -j TPROXY --on-ip 127.0.0.1 --on-port 1089 --tproxy-mark 1089");
            shellCommand("iptables -t mangle -I TPROXY_MARK_PREROUTING -p tcp --dport 443 -j TPROXY --on-ip 127.0.0.1 --on-port 1089 --tproxy-mark 1089");
            shellCommand("ip6tables -t mangle -I TPROXY_MARK_PREROUTING -p tcp --dport 80 -j TPROXY --on-ip ::1 --on-port 1089 --tproxy-mark 1089");
            shellCommand("ip6tables -t mangle -I TPROXY_MARK_PREROUTING -p tcp --dport 443 -j TPROXY --on-ip ::1 --on-port 1089 --tproxy-mark 1089");
            shellCommand("ip rule add fwmark 1089 table 998");
            shellCommand("ip route add local default dev lo table 998");
            shellCommand("ip -6 rule add fwmark 1089 table 998");
            shellCommand("ip -6 route add local default dev lo table 998");
            //shellCommand(appData + "/tpws." + Build.SUPPORTED_ABIS[0] + " --bind-addr=127.0.0.1 --bind-addr=::1 --port=1089 --split-pos=3 --uid 1:3003 &");
        }
        if (dnsmasq) { // kill dnsmasq
            String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
            // Redirect IPv4 incoming DHCP Broadcast requests
            shellCommand("iptables -t nat -I PREROUTING -s 0.0.0.0 -d 255.255.255.255 -p udp --dport 67 -j DNAT --to-destination 255.255.255.255:6767");
            // Redirect IPv4 incoming DNS requests
            shellCommand("iptables -t nat -I PREROUTING -s " + ipv4Prefix + ".0/24 -d " + ipv4Addr + " -p udp --dport 53 -j DNAT --to-destination " + ipv4Addr + ":5353");
            // Drop IPv6 Router Advertisement packets from RouterAdvertisementDaemon
            shellCommand("ip6tables -t filter -A OUTPUT -m owner ! --uid-owner 0 -p icmpv6 --icmpv6-type 134 -m hl --hl-eq 255 -j DROP");
            shellCommand("rm " + appData + "/dnsmasq.leases");
            shellCommand("rm " + appData + "/dnsmasq.pid");
            //shellCommand(appData + "/dnsmasq." + Build.SUPPORTED_ABIS[0] + " --keep-in-foreground --no-resolv --no-poll --domain-needed --bogus-priv --dhcp-authoritative --port=5353 --dhcp-alternate-port=6767,68 --dhcp-range=" + ipv4Prefix + ".10," + ipv4Prefix + ".99,1h --dhcp-range=" + ipv6Prefix + "10," + ipv6Prefix + "99,slaac,64,1h --dhcp-option=option:dns-server," + ipv4Addr + " --dhcp-option=option6:dns-server,[2001:4860:4860::8888],[2001:4860:4860::8844] --server=8.8.8.8 --server=8.8.4.4 --listen-mark 0xf0063 --dhcp-leasefile=" + appData + "/dnsmasq.leases --pid-file=" + appData + "/dnsmasq.pid &");
        }
        if (dropForward) {
            // Drop forwarded traffic
            shellCommand("iptables -t filter -I FORWARD -j DROP");
            shellCommand("ip6tables -t filter -I FORWARD -j DROP");
        }
    }

    static void unconfigureTPROXY(String ipv4Addr, boolean dnsmasq, boolean dpiCircumvention, boolean dropForward) {
        if (dnsmasq) {
            shellCommand("killall dnsmasq." + Build.SUPPORTED_ABIS[0]);
            String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
            shellCommand("iptables -t nat -D PREROUTING -s 0.0.0.0 -d 255.255.255.255 -p udp --dport 67 -j DNAT --to-destination 255.255.255.255:6767");
            shellCommand("iptables -t nat -D PREROUTING -s " + ipv4Prefix + ".0/24 -d " + ipv4Addr + " -p udp --dport 53 -j DNAT --to-destination " + ipv4Addr + ":5353");
            shellCommand("ip6tables -t filter -D OUTPUT -m owner ! --uid-owner 0 -p icmpv6 --icmpv6-type 134 -m hl --hl-eq 255 -j DROP");
            shellCommand("ip rule delete priority 18000");
            shellCommand("ip -6 rule delete priority 18000");
        }
        if (dpiCircumvention) {
            shellCommand("killall tpws." + Build.SUPPORTED_ABIS[0]);
            shellCommand("ip rule delete fwmark 1089 table 998");
            shellCommand("ip route delete local default dev lo table 998");
            shellCommand("ip -6 rule delete fwmark 1089 table 998");
            shellCommand("ip -6 route delete local default dev lo table 998");
        }
        shellCommand("killall hev-socks5-server." + Build.SUPPORTED_ABIS[0]);
        shellCommand("killall hev-socks5-tproxy." + Build.SUPPORTED_ABIS[0]);
        shellCommand("iptables -t mangle -D TPROXY_ROUTE_PREROUTING -j TPROXY_MARK_PREROUTING");
        shellCommand("iptables -t mangle -F TPROXY_MARK_PREROUTING");
        shellCommand("iptables -t mangle -X TPROXY_MARK_PREROUTING");
        shellCommand("iptables -t mangle -D PREROUTING -j TPROXY_ROUTE_PREROUTING");
        shellCommand("iptables -t mangle -F TPROXY_ROUTE_PREROUTING");
        shellCommand("iptables -t mangle -X TPROXY_ROUTE_PREROUTING");
        shellCommand("ip6tables -t mangle -D TPROXY_ROUTE_PREROUTING -j TPROXY_MARK_PREROUTING");
        shellCommand("ip6tables -t mangle -F TPROXY_MARK_PREROUTING");
        shellCommand("ip6tables -t mangle -X TPROXY_MARK_PREROUTING");
        shellCommand("ip6tables -t mangle -D PREROUTING -j TPROXY_ROUTE_PREROUTING");
        shellCommand("ip6tables -t mangle -F TPROXY_ROUTE_PREROUTING");
        shellCommand("ip6tables -t mangle -X TPROXY_ROUTE_PREROUTING");
        shellCommand("ip rule delete fwmark 1088 table 999");
        shellCommand("ip route delete local default dev lo table 999");
        shellCommand("ip -6 rule delete fwmark 1088 table 999");
        shellCommand("ip -6 route delete local default dev lo table 999");
        shellCommand("iptables -t filter -D FORWARD -j DROP");
        shellCommand("ip6tables -t filter -D FORWARD -j DROP");
    }
}
