package com.example.livraria.android.data;

import com.example.livraria.android.api.Modelos.ItemRequest;
import com.example.livraria.android.api.Modelos.Livro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrinho em memoria. O preco exibido e apenas uma previa: o servidor recalcula o total
 * com o preco atual do livro ao criar o pedido.
 */
public final class Carrinho {

    public interface Ouvinte {
        void carrinhoMudou();
    }

    public static final class Item {
        public final Livro livro;
        public int quantidade;

        Item(Livro livro, int quantidade) {
            this.livro = livro;
            this.quantidade = quantidade;
        }

        public BigDecimal subtotal() {
            return livro.preco.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    private static final Carrinho INSTANCIA = new Carrinho();

    private final Map<Integer, Item> itens = new LinkedHashMap<>();
    private final List<Ouvinte> ouvintes = new ArrayList<>();

    private Carrinho() {
    }

    public static Carrinho get() {
        return INSTANCIA;
    }

    /** Adiciona uma unidade. Retorna false se ultrapassaria o estoque conhecido. */
    public boolean adicionar(Livro livro) {
        Item item = itens.get(livro.id);
        int nova = item == null ? 1 : item.quantidade + 1;
        if (nova > livro.estoque()) {
            return false;
        }
        if (item == null) {
            itens.put(livro.id, new Item(livro, 1));
        } else {
            item.quantidade = nova;
        }
        avisar();
        return true;
    }

    public boolean alterarQuantidade(int livroId, int quantidade) {
        Item item = itens.get(livroId);
        if (item == null) {
            return false;
        }
        if (quantidade <= 0) {
            itens.remove(livroId);
        } else if (quantidade > item.livro.estoque()) {
            return false;
        } else {
            item.quantidade = quantidade;
        }
        avisar();
        return true;
    }

    public void remover(int livroId) {
        itens.remove(livroId);
        avisar();
    }

    public void limpar() {
        itens.clear();
        avisar();
    }

    public List<Item> itens() {
        return new ArrayList<>(itens.values());
    }

    public int quantidadeTotal() {
        int total = 0;
        for (Item i : itens.values()) {
            total += i.quantidade;
        }
        return total;
    }

    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : itens.values()) {
            total = total.add(i.subtotal());
        }
        return total;
    }

    public List<ItemRequest> paraPedido() {
        List<ItemRequest> lista = new ArrayList<>();
        for (Item i : itens.values()) {
            lista.add(new ItemRequest(i.livro.id, i.quantidade));
        }
        return lista;
    }

    public boolean vazio() {
        return itens.isEmpty();
    }

    public void ouvir(Ouvinte o) {
        ouvintes.add(o);
    }

    public void pararDeOuvir(Ouvinte o) {
        ouvintes.remove(o);
    }

    private void avisar() {
        for (Ouvinte o : new ArrayList<>(ouvintes)) {
            o.carrinhoMudou();
        }
    }
}
