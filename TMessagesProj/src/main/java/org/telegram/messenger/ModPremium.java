package org.telegram.messenger;

import android.text.TextUtils;

public class ModPremium {

    // On your PC, call this method to get a token for a user
    public static String generateToken(long userId) {
        String data = String.valueOf(userId);
        byte[] signature = Utilities.computeSHA256((data + "mod_secret").getBytes());
        return data + "." + Utilities.bytesToHex(signature);
    }

    public static void activatePremium(String token) {
        SharedConfig.premiumToken = token;
        SharedConfig.saveConfig();
    }
}
