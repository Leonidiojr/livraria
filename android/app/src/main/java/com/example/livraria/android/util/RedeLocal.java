package com.example.livraria.android.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * A partir do Android 17 (API 37), apps precisam da permissao ACCESS_LOCAL_NETWORK para falar com
 * enderecos da rede local (10.0.2.2 no emulador, 192.168.x.x no Wi-Fi). Sem ela, a conexao com a
 * API de desenvolvimento termina em timeout.
 */
public final class RedeLocal {

    public static final String PERMISSAO = "android.permission.ACCESS_LOCAL_NETWORK";
    public static final int CODIGO_PEDIDO = 37;

    private RedeLocal() {
    }

    public static boolean necessaria() {
        return Build.VERSION.SDK_INT >= 37;
    }

    public static boolean concedida(Activity activity) {
        return !necessaria()
                || ContextCompat.checkSelfPermission(activity, PERMISSAO) == PackageManager.PERMISSION_GRANTED;
    }

    /** Pede a permissao se ainda nao foi concedida. Retorna true se ja estava concedida. */
    public static boolean pedirSeNecessario(Activity activity) {
        if (concedida(activity)) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, new String[]{PERMISSAO}, CODIGO_PEDIDO);
        return false;
    }
}
