package com.example.livraria.android.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/** Aplica a mascara 000.000.000-00 enquanto o usuario digita (formato exigido pela API). */
class MascaraCpf implements TextWatcher {

    private final EditText campo;
    private boolean editando;

    MascaraCpf(EditText campo) {
        this.campo = campo;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (editando) {
            return;
        }
        String digitos = s.toString().replaceAll("\\D", "");
        if (digitos.length() > 11) {
            digitos = digitos.substring(0, 11);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 3 || i == 6) {
                sb.append('.');
            } else if (i == 9) {
                sb.append('-');
            }
            sb.append(digitos.charAt(i));
        }
        String formatado = sb.toString();
        if (!formatado.equals(s.toString())) {
            editando = true;
            campo.setText(formatado);
            campo.setSelection(formatado.length());
            editando = false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
