package com.example.livraria;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gera imagens reais (PNG/JPEG) para os testes de upload. */
public final class Imagens {

    private Imagens() {
    }

    public static byte[] png(int largura, int altura) {
        return gerar("png", largura, altura);
    }

    public static byte[] jpeg(int largura, int altura) {
        return gerar("jpg", largura, altura);
    }

    /** Grava um PNG em arquivo temporario (para o campo de upload do navegador nos testes E2E). */
    public static Path arquivoPng() {
        try {
            Path p = Files.createTempFile("capa-e2e-", ".png");
            Files.write(p, png(120, 180));
            p.toFile().deleteOnExit();
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] gerar(String formato, int largura, int altura) {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(11, 11, 12));
        g.fillRect(0, 0, largura, altura);
        g.setColor(new Color(215, 25, 43));
        g.fillRect(0, 0, largura / 10, altura);
        g.setColor(new Color(29, 78, 216));
        g.fillRect(0, altura * 2 / 3, largura, altura / 20);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, formato, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
