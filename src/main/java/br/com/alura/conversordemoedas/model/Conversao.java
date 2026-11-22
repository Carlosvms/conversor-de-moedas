package br.com.alura.conversordemoedas.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa um registro imutável de conversão monetária,
 * armazenando moedas, valores, taxa aplicada e data/hora da operação.
 */
public record Conversao(
        String moedaOrigem,
        String moedaDestino,
        double valorOriginal,
        double taxaUtilizada,
        double valorConvertido,
        LocalDateTime dataHora
) {
    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Construtor auxiliar que padroniza os códigos das moedas em maiúsculas
     * e registra automaticamente o momento exato da conversão.
     */
    public Conversao(String moedaOrigem, String moedaDestino, double valorOriginal, double taxaUtilizada, double valorConvertido) {
        this(
                moedaOrigem != null ? moedaOrigem.trim().toUpperCase() : "",
                moedaDestino != null ? moedaDestino.trim().toUpperCase() : "",
                valorOriginal,
                taxaUtilizada,
                valorConvertido,
                LocalDateTime.now()
        );
    }

    /**
     * Gera uma representação textual formatada da conversão para exibição no console.
     */
    public String formatado() {
        return String.format("[%s] %.2f [%s] => %.2f [%s] (Taxa: %.6f)",
                dataHora.format(FORMATADOR_DATA_HORA),
                valorOriginal,
                moedaOrigem,
                valorConvertido,
                moedaDestino,
                taxaUtilizada);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
