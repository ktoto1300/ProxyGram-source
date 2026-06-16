package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyManager {
    private static final String PROXY_LIST_URL_DEFAULT = "https://raw.githubusercontent.com/ktoto1300/Proxy-s/main/proxies.txt";
    private static final String PREF_NAME = "proxygram_manager";
    // Флаг: юзер сам когда-либо менял прокси вручную — тогда не трогаем автоматически
    private static final String PREF_USER_CONFIGURED = "proxy_user_configured";

    private static boolean started = false;
    private static boolean isChecking = false;
    public static boolean startupDialogShown = false;

    // ─── Start ────────────────────────────────────────────────────────────────

    public static void start(Context context) {
        if (started) return;
        started = true;

        new Thread(() -> {
            // Шаг 1: сразу применяем первый прокси из сохранённого списка (без пинга!)
            // Только если юзер ещё не настраивал прокси вручную
            if (!isUserConfigured() && !SharedConfig.proxyList.isEmpty()) {
                SharedConfig.ProxyInfo first = SharedConfig.proxyList.get(0);
                applyProxy(first);
                FileLog.d("ProxyManager: auto-applied saved proxy on startup: " + first.address);
            }

            // Шаг 2: ждём 10 секунд пока сеть поднимется
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}

            // Шаг 3: фетчим свежий список прокси
            if (SharedConfig.proxyAutoUpdate) {
                fetchAndApplyProxies();
            }

            // Шаг 4: периодически обновляем список
            while (true) {
                try {
                    Thread.sleep(Math.max(1, SharedConfig.proxyUpdateInterval) * 60 * 1000L);
                } catch (InterruptedException ignored) {}
                if (SharedConfig.proxyAutoUpdate) {
                    fetchAndApplyProxies();
                }
            }
        }).start();
    }

    public static void showStartupLoadingDialog(final android.app.Activity activity) {
        if (startupDialogShown || !SharedConfig.proxyStartupLoading) return;
        startupDialogShown = true;

        final org.telegram.ui.ActionBar.AlertDialog progressDialog = new org.telegram.ui.ActionBar.AlertDialog(activity, 2); // 2 = ALERT_TYPE_LOADING
        progressDialog.setTitle("Загрузка прокси");
        progressDialog.setCanCancel(false);
        progressDialog.show();
        progressDialog.setProgress(0);

        new Thread(() -> {
            AndroidUtilities.runOnUIThread(() -> progressDialog.setProgress(10));
            try {
                String urlStr = SharedConfig.proxyListUrl != null ? SharedConfig.proxyListUrl : PROXY_LIST_URL_DEFAULT;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(conn.getInputStream()));
                ArrayList<SharedConfig.ProxyInfo> fetched = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    SharedConfig.ProxyInfo info = parseProxyLine(line);
                    if (info != null) fetched.add(info);
                }
                reader.close();

                if (!fetched.isEmpty()) {
                    if (SharedConfig.isPremium()) {
                        fetched.add(new SharedConfig.ProxyInfo("premium.proxygram.net", 443, "", "", "ee112233445566778899aabbccddeeff"));
                    }
                    SharedConfig.proxyList.clear();
                    SharedConfig.proxyList.addAll(fetched);
                    SharedConfig.saveProxyList();
                }
            } catch (Exception e) {
                FileLog.e("ProxyManager: sync fetch failed - " + e.getMessage());
            }

            AndroidUtilities.runOnUIThread(() -> progressDialog.setProgress(30));

            if (!SharedConfig.proxyList.isEmpty()) {
                ArrayList<SharedConfig.ProxyInfo> list = new ArrayList<>(SharedConfig.proxyList);
                final Object lock = new Object();
                final SharedConfig.ProxyInfo[] bestProxy = new SharedConfig.ProxyInfo[1];
                final long[] bestPing = new long[]{Long.MAX_VALUE};
                
                final int total = list.size();
                final int[] current = new int[]{0};

                ExecutorService executor = Executors.newFixedThreadPool(20);
                for (SharedConfig.ProxyInfo info : list) {
                    executor.submit(() -> {
                        long ping = pingProxy(info.address, info.port);
                        synchronized (lock) {
                            if (ping != -1 && ping < bestPing[0]) {
                                bestPing[0] = ping;
                                bestProxy[0] = info;
                            }
                            current[0]++;
                            int progress = 30 + (int)((current[0] / (float)total) * 70);
                            AndroidUtilities.runOnUIThread(() -> progressDialog.setProgress(progress));
                        }
                    });
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(15, TimeUnit.SECONDS);
                } catch (Exception ignored) {}

                if (bestProxy[0] != null) {
                    // Force apply, since it's the startup loading and the user wants it to automatically enable and select best proxy
                    applyProxy(bestProxy[0]);
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                progressDialog.setProgress(100);
                progressDialog.dismiss();
            });
        }).start();
    }

    // ─── Флаг ручной настройки ────────────────────────────────────────────────

    /** Вызывается когда юзер сам меняет прокси в настройках */
    public static void markUserConfigured() {
        ApplicationLoader.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_USER_CONFIGURED, true).apply();
    }

    private static boolean isUserConfigured() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_USER_CONFIGURED, false);
    }

    // ─── Fetch ────────────────────────────────────────────────────────────────

    public static void fetchAndApplyProxies() {
        new Thread(() -> {
            try {
                String urlStr = SharedConfig.proxyListUrl != null
                        ? SharedConfig.proxyListUrl : PROXY_LIST_URL_DEFAULT;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                ArrayList<SharedConfig.ProxyInfo> fetched = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    SharedConfig.ProxyInfo info = parseProxyLine(line);
                    if (info != null) fetched.add(info);
                }
                reader.close();

                if (!fetched.isEmpty()) {
                    addNewProxiesToList(fetched);
                }
            } catch (Exception e) {
                FileLog.e("ProxyManager: fetch failed - " + e.getMessage());
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
                    return new SharedConfig.ProxyInfo(
                            server, Integer.parseInt(portStr),
                            user != null ? user : "",
                            pass != null ? pass : "",
                            secret != null ? secret : "");
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
        } catch (Exception ignored) {}
        return null;
    }

    // ─── Add new to list ──────────────────────────────────────────────────────

    private static void addNewProxiesToList(ArrayList<SharedConfig.ProxyInfo> fetched) {
        if (SharedConfig.isPremium()) {
            fetched.add(new SharedConfig.ProxyInfo("premium.proxygram.net", 443, "", "",
                    "ee112233445566778899aabbccddeeff"));
        }

        boolean changed = false;
        for (SharedConfig.ProxyInfo newProxy : fetched) {
            boolean exists = false;
            for (SharedConfig.ProxyInfo existing : SharedConfig.proxyList) {
                if (existing.address.equalsIgnoreCase(newProxy.address)
                        && existing.port == newProxy.port) {
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
                    NotificationCenter.getGlobalInstance()
                            .postNotificationName(NotificationCenter.proxySettingsChanged));
            // После добавления новых — пингуем чтобы найти рабочий
            checkAndApplyBestProxy();
        }
    }

    // ─── Ping & найти лучший ──────────────────────────────────────────────────

    /**
     * Пингует все прокси, нерабочие удаляет из списка (но не навсегда — при
     * следующем фетче они вернутся). Если юзер не трогал настройки вручную —
     * применяет первый рабочий.
     */
    public static void checkAndApplyBestProxy() {
        if (SharedConfig.proxyList.isEmpty() || isChecking) return;
        isChecking = true;

        new Thread(() -> {
            ArrayList<SharedConfig.ProxyInfo> toRemove = new ArrayList<>();
            SharedConfig.ProxyInfo bestProxy = null;

            for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
                SharedConfig.ProxyInfo info = SharedConfig.proxyList.get(i);
                long ping = pingProxy(info.address, info.port);
                if (ping == -1) {
                    // Недоступен — убираем из текущего списка
                    toRemove.add(info);
                    FileLog.d("ProxyManager: unreachable, removing from list: "
                            + info.address + ":" + info.port);
                } else if (bestProxy == null) {
                    bestProxy = info;
                }
            }

            // Удаляем нерабочие из текущего списка
            if (!toRemove.isEmpty()) {
                for (SharedConfig.ProxyInfo bad : toRemove) {
                    SharedConfig.proxyList.remove(bad);
                }
                SharedConfig.saveProxyList();
                AndroidUtilities.runOnUIThread(() ->
                        NotificationCenter.getGlobalInstance()
                                .postNotificationName(NotificationCenter.proxySettingsChanged));
            }

            // Автоприменяем только если юзер не настраивал вручную
            if (bestProxy != null && !isUserConfigured()) {
                if (SharedConfig.currentProxy == null || !SharedConfig.isProxyEnabled()) {
                    applyProxy(bestProxy);
                }
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

    // ─── Apply ────────────────────────────────────────────────────────────────

    private static void applyProxy(SharedConfig.ProxyInfo info) {
        SharedConfig.currentProxy = info;
        AndroidUtilities.runOnUIThread(() -> {
            SharedPreferences prefs = MessagesController.getGlobalMainSettings();
            prefs.edit()
                    .putBoolean("proxy_enabled", true)
                    .putString("proxy_ip", info.address)
                    .putInt("proxy_port", info.port)
                    .putString("proxy_user", info.username)
                    .putString("proxy_pass", info.password)
                    .putString("proxy_secret", info.secret)
                    .apply();
            ConnectionsManager.setProxySettings(true, info.address, info.port,
                    info.username, info.password, info.secret);
            FileLog.d("ProxyManager: applied proxy " + info.address + ":" + info.port);
        });
    }

    // ─── Misc ─────────────────────────────────────────────────────────────────

    public static boolean isPromoBlocked() {
        return SharedConfig.noSponsor;
    }
}
