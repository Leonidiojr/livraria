package com.example.livraria.config;

import com.example.livraria.security.UsuarioLogado;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Liga a auditoria do banco ao usuario da aplicacao: toda conexao entregue pelo pool recebe a
 * variavel de sessao {@code app.usuario} com o e-mail do usuario logado (ou "sistema"/"anonimo"),
 * que o trigger {@code fn_auditoria} grava na tabela de auditoria.
 */
@Configuration
public class AuditoriaDataSourceConfig {

    @Bean
    static BeanPostProcessor auditoriaDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && !(bean instanceof AuditoriaDataSource)) {
                    return new AuditoriaDataSource(ds);
                }
                return bean;
            }
        };
    }

    static class AuditoriaDataSource extends DelegatingDataSource {

        AuditoriaDataSource(DataSource alvo) {
            super(alvo);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return identificar(super.getConnection());
        }

        @Override
        public Connection getConnection(String usuario, String senha) throws SQLException {
            return identificar(super.getConnection(usuario, senha));
        }

        private static Connection identificar(Connection conexao) throws SQLException {
            String usuario = UsuarioLogado.email()
                    .orElse(Thread.currentThread().getName().toLowerCase().contains("main") ? "sistema" : "anonimo");
            try (PreparedStatement ps = conexao.prepareStatement("select set_config('app.usuario', ?, false)")) {
                ps.setString(1, usuario);
                ps.execute();
            }
            return conexao;
        }
    }
}
