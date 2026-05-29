package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.telegram.tgnet.ConnectionsManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ProxyManager {
    private static final String PROXY_LIST_URL_DEFAULT = "https://raw.githubusercontent.com/ktoto1300/Proxy-s/main/proxies.txt";
    private static boolean started = false;

    public static void start(Context context) {
        if (started) return;
        started = true;

        new Thread(() -> {
            while (true) {
                if (SharedConfig.proxyAutoUpdate) {
                    fetchAndApplyProxies();
                }
                try {
                    // Интервал проверки берется из настроек (в часах)
                    Thread.sleep(Math.max(1, SharedConfig.proxyUpdateInterval) * 60 * 60 * 1000L);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

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
                    if (info != null) {
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

    private static SharedConfig.ProxyInfo parseProxyLine(String line) {
        try {
            // Поддержка ссылок tg://proxy и https://t.me/proxy
            if (line.startsWith("tg://proxy") || line.startsWith("https://t.me/proxy")) {
                Uri uri = Uri.parse(line.replace("https://t.me/proxy", "tg://proxy"));
                String server = uri.getQueryParameter("server");
                String portStr = uri.getQueryParameter("port");
                String secret = uri.getQueryParameter("secret");
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");

                if (server != null && portStr != null) {
                    return new SharedConfig.ProxyInfo(server, Integer.parseInt(portStr), user != null ? user : "", pass != null ? pass : "", secret != null ? secret : "");
                }
            } 
            
            // Поддержка формата server:port:secret или server:port:user:pass
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String server = parts[0];
                int port = Integer.parseInt(parts[1]);
                if (parts.length == 3) {
                    // MTProto secret
                    return new SharedConfig.ProxyInfo(server, port, "", "", parts[2]);
                } else if (parts.length == 4) {
                    // Socks5 user/pass
                    return new SharedConfig.ProxyInfo(server, port, parts[2], parts[3], "");
                } else if (parts.length == 2) {
                    // Simple server:port
                    return new SharedConfig.ProxyInfo(server, port, "", "", "");
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static void updateTelegramProxyList(ArrayList<SharedConfig.ProxyInfo> newProxies) {
        boolean changed = false;
        for (SharedConfig.ProxyInfo newProxy : newProxies) {
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
            checkAllProxies();
        }
    }

    public static void checkAllProxies() {
        if (SharedConfig.proxyList.isEmpty()) return;

        for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
            final SharedConfig.ProxyInfo info = SharedConfig.proxyList.get(i);
            ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(info.address, info.port, info.username, info.password, info.secret, (time) -> {
                if (time == -1) {
                    // Прокси мертв - удаляем из списка Telegram
                    SharedConfig.deleteProxy(info);
                    FileLog.d("ProxyManager: Removed dead proxy " + info.address);
                    
                    // Если удалили текущий - ищем замену
                    if (SharedConfig.currentProxy == info) {
                        SharedConfig.currentProxy = null;
                        switchToNextWorkingProxy();
                    }
                } else {
                    // Прокси живой
                    if (SharedConfig.currentProxy == null || !SharedConfig.isProxyEnabled()) {
                        applyProxy(info);
                    }
                }
            });
        }
    }

    private static void switchToNextWorkingProxy() {
        if (SharedConfig.proxyList.isEmpty()) {
            showNoProxiesAlert();
            return;
        }
        // Берем первый из списка
        applyProxy(SharedConfig.proxyList.get(0));
    }

    private static void applyProxy(SharedConfig.ProxyInfo info) {
        SharedConfig.currentProxy = info;
        
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        preferences.edit()
            .putBoolean("proxy_enabled", true)
            .putString("proxy_ip", info.address)
            .putInt("proxy_port", info.port)
            .putString("proxy_user", info.username)
            .putString("proxy_pass", info.password)
            .putString("proxy_secret", info.secret)
            .apply();

        ConnectionsManager.getInstance(UserConfig.selectedAccount).setProxySettings(info);
    }

    private static void showNoProxiesAlert() {
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didUpdateConnectionState);
    }

    public static boolean isPromoBlocked() {
        return SharedConfig.noSponsor;
    }
}
