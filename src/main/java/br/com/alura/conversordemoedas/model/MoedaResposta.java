package br.com.alura.conversordemoedas.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;

/**
 * Record (DTO) para desserialização do JSON retornado pela ExchangeRate-API.
 * Utiliza @SerializedName para mapear os campos em padrão snake_case para convenções Java.
 */
public record MoedaResposta(
        String result,
        @SerializedName("base_code") String baseCode,
        @SerializedName("conversion_rates") Map<String, Double> conversionRates,
        @SerializedName("error-type") String errorType,
        @SerializedName("time_last_update_utc") String timeLastUpdateUtc
) {

    /**
     * Indica se a requisição foi atendida com sucesso pelo provedor.
     */
    public boolean isSucesso() {
        return "success".equalsIgnoreCase(result);
    }

    /**
     * Obtém a taxa de conversão para uma moeda específica, com busca insensível a maiúsculas/minúsculas.
     */
    public Double obterTaxaPara(String moedaDestino) {
        if (conversionRates == null || moedaDestino == null) {
            return null;
        }
        return conversionRates.get(moedaDestino.trim().toUpperCase());
    }

    /**
     * Retorna o mapa de taxas protegido contra modificações externas.
     */
    @Override
    public Map<String, Double> conversionRates() {
        return conversionRates != null ? Collections.unmodifiableMap(conversionRates) : Collections.emptyMap();
    }
}
