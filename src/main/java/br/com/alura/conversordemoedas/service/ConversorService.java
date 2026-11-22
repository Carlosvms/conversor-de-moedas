package br.com.alura.conversordemoedas.service;

import br.com.alura.conversordemoedas.exception.ErroConsultaApiException;
import br.com.alura.conversordemoedas.model.Conversao;
import br.com.alura.conversordemoedas.model.MoedaResposta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serviço de negócio responsável por processar as conversões monetárias,
 * aplicar as taxas obtidas e registrar o histórico de operações da sessão.
 */
public class ConversorService {

    private final ConsultaCotacaoService consultaCotacaoService;
    private final List<Conversao> historico;

    public ConversorService() {
        this(new ConsultaCotacaoService());
    }

    public ConversorService(ConsultaCotacaoService consultaCotacaoService) {
        if (consultaCotacaoService == null) {
            throw new IllegalArgumentException("O serviço de consulta de cotação não pode ser nulo.");
        }
        this.consultaCotacaoService = consultaCotacaoService;
        this.historico = new ArrayList<>();
    }

    /**
     * Realiza a conversão monetária entre as moedas especificadas.
     *
     * @param moedaOrigem Código da moeda de origem (ex: USD)
     * @param moedaDestino Código da moeda de destino (ex: BRL)
     * @param valor Quantia positiva a ser convertida
     * @return Conversao com dados calculados e inserida no histórico
     */
    public Conversao converter(String moedaOrigem, String moedaDestino, double valor) {
        validarParametrosConversao(moedaOrigem, moedaDestino, valor);

        String origemFormatada = moedaOrigem.trim().toUpperCase();
        String destinoFormatada = moedaDestino.trim().toUpperCase();

        double taxa;
        // Otimização: se as moedas forem iguais, evita chamada desnecessária à API
        if (origemFormatada.equals(destinoFormatada)) {
            taxa = 1.0;
        } else {
            MoedaResposta resposta = consultaCotacaoService.consultarCotacao(origemFormatada);
            Double taxaEncontrada = resposta.obterTaxaPara(destinoFormatada);

            if (taxaEncontrada == null) {
                throw new ErroConsultaApiException(
                        "A moeda de destino '" + destinoFormatada + "' não foi encontrada entre as taxas fornecidas pela API."
                );
            }
            taxa = taxaEncontrada;
        }

        // Calcula o valor final convertido
        double valorConvertido = valor * taxa;
        Conversao conversao = new Conversao(
                origemFormatada,
                destinoFormatada,
                valor,
                taxa,
                valorConvertido,
                LocalDateTime.now()
        );

        // Registra a conversão no histórico da sessão
        this.historico.add(conversao);
        return conversao;
    }

    /**
     * Retorna uma visualização imutável do histórico para preservar o estado interno.
     */
    public List<Conversao> getHistorico() {
        return Collections.unmodifiableList(this.historico);
    }

    public boolean temHistorico() {
        return !this.historico.isEmpty();
    }

    public int getQuantidadeConversoes() {
        return this.historico.size();
    }

    public void limparHistorico() {
        this.historico.clear();
    }

    /**
     * Validações básicas de entrada antes de disparar o processamento.
     */
    private void validarParametrosConversao(String moedaOrigem, String moedaDestino, double valor) {
        if (moedaOrigem == null || moedaOrigem.isBlank()) {
            throw new IllegalArgumentException("O código da moeda de origem não pode ser nulo ou vazio.");
        }
        if (moedaDestino == null || moedaDestino.isBlank()) {
            throw new IllegalArgumentException("O código da moeda de destino não pode ser nulo ou vazio.");
        }
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor a ser convertido deve ser estritamente maior que zero.");
        }
    }
}
