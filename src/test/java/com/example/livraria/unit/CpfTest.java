package com.example.livraria.unit;

import com.example.livraria.service.Cpf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfTest {

    @ParameterizedTest(name = "{0} e valido")
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
    @DisplayName("CPFs com digitos verificadores corretos sao aceitos")
    void validos(String cpf) {
        assertThat(Cpf.valido(cpf)).isTrue();
    }

    @ParameterizedTest(name = "{0} e invalido")
    @ValueSource(strings = {"529.982.247-26", "111.111.111-11", "000.000.000-00", "123", "529.982.247-2X"})
    @DisplayName("CPFs com digitos errados, repetidos ou tamanho incorreto sao rejeitados")
    void invalidos(String cpf) {
        assertThat(Cpf.valido(cpf)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Nulo ou vazio e invalido")
    void vazio(String cpf) {
        assertThat(Cpf.valido(cpf)).isFalse();
    }
}
