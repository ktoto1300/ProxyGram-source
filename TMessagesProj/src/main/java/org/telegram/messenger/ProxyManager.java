package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestTimeDelegate;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ProxyManager {
    private static final String PROXY_LIST_URL_DEFAULT = "https://raw.githubusercontent.com/ktoto1300/Proxy-s/main/proxies.txt";
    private static final String PREF_BLACKLIST = "proxy_blacklist";
    private static final String PREF_NAME = "proxygram_manager";

    private static boolean started = false;
    private static boolean isChecking = false;

    // ─── Blacklist helpers ────────────────────────────────────────────────────

    /** Уникальный ключ прокси для чёрного списка: "host:port:secret" */
    private static String proxyKey(SharedConfig.ProxyInfo p) {
        return (p.address + ":" + p.port + ":" + (p.secret != null ? p.secret : "")).toLowerCase();
    }

    private static Set<String> loadBlacklist() {
        SharedPreferences prefs = ApplicationLoader.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(PREF_BLACKLIST, new HashSet<>()));
    }

    private static void saveBlacklist(Set<String> blacklist) {
        ApplicationLoader.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putStringSet(PREF_BLACKLIST, blacklist).apply();
    }

    private static void addToBlacklist(SharedConfig.ProxyInfo info) {
        Set<String> bl = loadBlacklist();
        bl.add(proxyKey(info));
        saveBlacklist(bl);
        FileLog.d("ProxyManager: blacklisted " + proxyKey(info));
    }

    private static boolean isBlacklisted(SharedConfig.ProxyInfo info) {
        return loadBlacklist().contains(proxyKey(info));
    }

    // ─── Start ────────────────────────────────────────────────────────────────

    public static void start(Context context) {
        if (started) return;
        started = true;

        // НЕ добавляем fallback-прокси — они перезаписывают сохранённый список

        new Thread(() -> {
            // Сначала проверяем уже сохранённые прокси
            checkAllProxies();

            while (true) {
                if (SharedConfig.proxyAutoUpdate) {
                    fetchAndApplyProxies();
                }
                try {
                    Thread.sleep(Math.max(1, SharedConfig.proxyUpdateInterval) * 60 * 1000L);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    // ─── Fetch ────────────────────────────────────────────────────────────────

    public static void fetchAndApplyProxies() {
        new Thread(() -> {
            try {
                URL url = new URL(SharedConfig.proxyListUrl != null ? SharedConfig.proxyListUrl : PROXY_LIST_URL_DEFAULT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                ArrayList<SharedConfig.ProxyInfo> newProxies = new ArrayList<>();
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    SharedConfig.ProxyInfo info = parseProxyLine(line);
                    if (info != null && !isBlacklisted(info)) {
                        newProxies.add(info);
                    }
                }
                reader.close();

                if (!newProxies.isEmpty()) {
                    updateTelegramProxyList(newProxies);
                }

            } catch (Exception e) {
                FileLog.e("ProxyManager: Failed to fetch proxies - " + e.getMessage());
            }
        }).start();
    }

    // ─── Parse ────────────────────────────────────────────────────────────────

    private static SharedConfig.ProxyInfo parseProxyLine(String line) {
        try {
            if (line.startsWith("tg://proxy") || line.startsWith("https://t.me/proxy")) {
                Uri uri = Uri.parse(line.replace("https://t.me/proxy", "tg://proxy"));
                String server = uri.getQueryParameter("server");
                String portStr = uri.getQueryParameter("port");
                String secret = uri.getQueryParameter("secret");
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");

                if (server != null && portStr != null) {
                    return new SharedConfig.ProxyInfo(server, Integer.parseInt(portStr),
                            user != null ? user : "", pass != null ? pass : "", secret != null ? secret : "");
                }
            }

            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String server = parts[0];
                int port = Integer.parseInt(parts[1]);
                if (parts.length == 3) {
                    return new SharedConfig.ProxyInfo(server, port, "", "", parts[2]);
                } else if (parts.length == 4) {
                    return new SharedConfig.ProxyInfo(server, port, parts[2], parts[3], "");
                } else {
                    return new SharedConfig.ProxyInfo(server, port, "", "", "");
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    // ─── Update list ──────────────────────────────────────────────────────────

    private static void updateTelegramProxyList(ArrayList<SharedConfig.ProxyInfo> newProxies) {
        if (SharedConfig.isPremium()) {
            SharedConfig.ProxyInfo premProxy = new SharedConfig.ProxyInfo("premium.proxygram.net", 443, "", "", "ee112233445566778899aabbccddeeff");
            if (!isBlacklisted(premProxy)) newProxies.add(premProxy);
        }

        boolean changed = false;
        Set<String> blacklist = loadBlacklist();

        for (SharedConfig.ProxyInfo newProxy : newProxies) {
            if (blacklist.contains(proxyKey(newProxy))) continue; // пропускаем занесённые в ЧС

            boolean exists = false;
            for (SharedConfig.ProxyInfo existing : SharedConfig.proxyList) {
                if (existing.address.equalsIgnoreCase(newProxy.address) && existing.port == newProxy.port) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                SharedConfig.proxyList.add(newProxy);
                changed = true;
            }
        }

        if (changed) {
            SharedConfig.saveProxyList();
            AndroidUtilities.runOnUIThread(() ->
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged));
            checkAllProxies();
        }
    }

    // ─── Check all proxies ────────────────────────────────────────────────────

    public static void checkAllProxies() {
        if (SharedConfig.proxyList.isEmpty() || isChecking) return;
        isChecking = true;

        new Thread(() -> {
            boolean foundWorking = false;
            Set<String> blacklist = loadBlacklist();
            ArrayList<SharedConfig.ProxyInfo> toRemove = new ArrayList<>();

            for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
                final SharedConfig.ProxyInfo info = SharedConfig.proxyList.get(i);

                // Если уже в чёрном списке — удаляем из активного списка
                if (blacklist.contains(proxyKey(info))) {
                    toRemove.add(info);
                    continue;
                }

                long ping = pingProxy(info.address, info.port);

                if (ping == -1) {
                    // Недоступен — навсегда в чёрный список и удаляем
                    addToBlacklist(info);
                    toRemove.add(info);
                    FileLog.d("ProxyManager: proxy unavailable, blacklisted: " + info.address + ":" + info.port);
                } else {
                    // Рабочий прокси
                    if (!foundWorking) {
                        foundWorking = true;
                        // Автовключение первого рабочего если прокси не выбран или отключён
                        if (SharedConfig.currentProxy == null || !SharedConfig.isProxyEnabled()) {
                            applyProxy(info);
                        }
                    }
                }
            }

            // Удаляем нерабочие из списка
            if (!toRemove.isEmpty()) {
                for (SharedConfig.ProxyInfo bad : toRemove) {
                    SharedConfig.proxyList.remove(bad);
                }
                SharedConfig.saveProxyList();
                AndroidUtilities.runOnUIThread(() ->
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged));
            }

            isChecking = false;
        }).start();
    }

    // ─── Ping ─────────────────────────────────────────────────────────────────

    private static long pingProxy(String ip, int port) {
        long start = System.currentTimeMillis();
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), 3000);
            socket.close();
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    // ─── Apply / Disable ──────────────────────────────────────────────────────

    private static void applyProxy(SharedConfig.ProxyInfo info) {
        SharedConfig.currentProxy = info;
        AndroidUtilities.runOnUIThread(() -> {
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            preferences.edit()
                    .putBoolean("proxy_enabled", true)
                    .putString("proxy_ip", info.address)
                    .putInt("proxy_port", info.port)
                    .putString("proxy_user", info.username)
                    .putString("proxy_pass", info.password)
                    .putString("proxy_secret", info.secret)
                    .apply();
            ConnectionsManager.setProxySettings(true, info.address, info.port, info.username, info.password, info.secret);
            FileLog.d("ProxyManager: applied proxy " + info.address + ":" + info.port);
        });
    }

    private static void disableProxy() {
        SharedConfig.currentProxy = null;
        AndroidUtilities.runOnUIThread(() -> {
            MessagesController.getGlobalMainSettings()
                    .edit().putBoolean("proxy_enabled", false).apply();
            ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didUpdateConnectionState);
        });
    }

    // ─── Misc ─────────────────────────────────────────────────────────────────

    public static boolean isPromoBlocked() {
        return SharedConfig.noSponsor;
    }
}
