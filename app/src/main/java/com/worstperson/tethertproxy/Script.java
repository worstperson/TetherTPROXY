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

    // Dnsmasq looses it's bind when the address changes, so we need a separate check for localhost
    static boolean isDnsmasqRunning() {
        return Shell.cmd("cat /proc/net/tcp | grep 0100007F:0035").exec().isSuccess();
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

    static private void configureAddress(String ipv4Addr, String iface) {
        Log.i("TetherTPROXY", "Setting IP address");
        shellCommand("ifconfig " + iface + " " + ipv4Addr + " netmask 255.255.255.0 up");
    }

    static void configureRoute(String ipv4Addr, String iface) {
        Log.i("TetherTPROXY", "Setting route");
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        shellCommand("ndc network route add 99 " + iface + " " + ipv4Prefix + ".0/24");
    }

    static void addRules(String ipv4Addr) {
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        // Redirect IPv4 incoming DHCP Broadcast requests
        if (!Shell.cmd("iptables -t nat -C PREROUTING -s 0.0.0.0 -d 255.255.255.255 -p udp --dport 67 -j DNAT --to-destination 255.255.255.255:6767").exec().isSuccess()) {
            shellCommand("iptables -t nat -I PREROUTING -s 0.0.0.0 -d 255.255.255.255 -p udp --dport 67 -j DNAT --to-destination 255.255.255.255:6767");
        }
        // Redirect IPv4 incoming DNS requests
        if (!Shell.cmd("iptables -t nat -C PREROUTING -s " + ipv4Prefix + ".0/24 -d " + ipv4Addr + " -p udp --dport 53 -j DNAT --to-destination " + ipv4Addr + ":5353").exec().isSuccess()) {
            shellCommand("iptables -t nat -I PREROUTING -s " + ipv4Prefix + ".0/24 -d " + ipv4Addr + " -p udp --dport 53 -j DNAT --to-destination " + ipv4Addr + ":5353");
        }
    }

    static void removeRules(String ipv4Addr) {
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        // Redirect IPv4 incoming DHCP Broadcast requests
        shellCommand("iptables -t nat -D PREROUTING -s 0.0.0.0 -d 255.255.255.255 -p udp --dport 67 -j DNAT --to-destination 255.255.255.255:6767");
        // Redirect IPv4 incoming DNS requests
        shellCommand("iptables -t nat -D PREROUTING -s " + ipv4Prefix + ".0/24 -d " + ipv4Addr + " -p udp --dport 53 -j DNAT --to-destination " + ipv4Addr + ":5353");
    }

    static void configureTether(String ipv4Addr, String iface) {
        if (!Shell.cmd("ip addr | grep " + iface + " | grep " + ipv4Addr).exec().isSuccess()) {
            configureAddress(ipv4Addr, iface);
            configureRoute(ipv4Addr, iface);
        } else {
            // FIXME: this case is not handled
            Log.i("TetherTPROXY", "Interface already configured!?!");
        }
    }

    static void startDnsmasq(String ipv4Addr, String iface, String appData) {
        String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
        shellCommand("rm " + appData + "/dnsmasq.leases");
        shellCommand("rm " + appData + "/dnsmasq.pid");
        shellCommand(appData + "/dnsmasq." + Build.SUPPORTED_ABIS[0] + " --interface=" + iface + " --keep-in-foreground --no-resolv --no-poll --domain-needed --bogus-priv --dhcp-authoritative --port=5353 --dhcp-alternate-port=6767,68 --dhcp-range=" + ipv4Prefix + ".10," + ipv4Prefix + ".99,1h --dhcp-option=option:dns-server," + ipv4Addr + " --server=8.8.8.8 --server=8.8.4.4 --dhcp-leasefile=" + appData + "/dnsmasq.leases --pid-file=" + appData + "/dnsmasq.pid &");
    }

    static void stopDnsmasq() {
        shellCommand("killall dnsmasq." + Build.SUPPORTED_ABIS[0]);
    }

    static String getDnsmasqPid(String appData) {
        if (Shell.cmd("[ -f " + appData + "/dnsmasq.pid ]").exec().isSuccess()) {
            for (String message : Shell.cmd("cat " + appData + "/dnsmasq.pid").exec().getOut()) {
                return message;
            }
        }
        return null;
    }

    static boolean isDnsmasqRunning(String pid) {
        return Shell.cmd("[ -d /proc/" + pid + " ]").exec().isSuccess();
    }
}
