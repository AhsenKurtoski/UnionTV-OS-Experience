package com.ahsenkurtoski.uniontvos.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.Inet4Address;
import java.net.InetAddress;

/** Reads the active network once, on demand. No polling, no permissions. */
public final class NetworkMonitor {

    private NetworkMonitor() {
    }

    public static final class Snapshot {
        public boolean connected;
        public String type = "—";
        public String ssid;
        public String ip = "—";
        public String gateway = "—";
        public String dns = "—";
        public String iface = "—";
        public int downKbps;
        public boolean metered;
    }

    public static Snapshot snapshot(Context context) {
        Snapshot snapshot = new Snapshot();
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return snapshot;
            }
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
            if (caps == null) {
                return snapshot;
            }
            snapshot.connected = true;
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                snapshot.type = "Wi-Fi";
                snapshot.ssid = wifiName(context);
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                snapshot.type = "Ethernet";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                snapshot.type = "Mobile";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                snapshot.type = "VPN";
            } else {
                snapshot.type = "Other";
            }
            snapshot.metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            snapshot.downKbps = caps.getLinkDownstreamBandwidthKbps();

            LinkProperties lp = cm.getLinkProperties(network);
            if (lp != null) {
                if (lp.getInterfaceName() != null) {
                    snapshot.iface = lp.getInterfaceName();
                }
                for (LinkAddress address : lp.getLinkAddresses()) {
                    InetAddress inet = address.getAddress();
                    if (inet instanceof Inet4Address && !inet.isLoopbackAddress()) {
                        snapshot.ip = inet.getHostAddress();
                        break;
                    }
                }
                for (RouteInfo route : lp.getRoutes()) {
                    if (route.isDefaultRoute() && route.getGateway() != null) {
                        snapshot.gateway = route.getGateway().getHostAddress();
                        break;
                    }
                }
                StringBuilder dns = new StringBuilder();
                for (InetAddress server : lp.getDnsServers()) {
                    if (dns.length() > 0) {
                        dns.append("  ·  ");
                    }
                    dns.append(server.getHostAddress());
                    if (dns.length() > 60) {
                        break;
                    }
                }
                if (dns.length() > 0) {
                    snapshot.dns = dns.toString();
                }
            }
        } catch (Throwable ignored) {
            // A missing service should never take the launcher down.
        }
        return snapshot;
    }

    /** Connected Wi-Fi network name, or null when it cannot be read. */
    public static String wifiName(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            WifiManager manager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (manager == null) {
                return null;
            }
            WifiInfo info = manager.getConnectionInfo();
            if (info == null) {
                return null;
            }
            String ssid = info.getSSID();
            if (ssid == null) {
                return null;
            }
            ssid = ssid.trim();
            if (ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            if (ssid.isEmpty() || ssid.startsWith("0x")
                    || "<unknown ssid>".equalsIgnoreCase(ssid)) {
                return null;
            }
            return ssid;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
