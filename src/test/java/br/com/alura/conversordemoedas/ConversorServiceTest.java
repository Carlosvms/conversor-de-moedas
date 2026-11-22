package br.com.alura.conversordemoedas;

import br.com.alura.conversordemoedas.exception.ErroConsultaApiException;
import br.com.alura.conversordemoedas.model.Conversao;
import br.com.alura.conversordemoedas.model.MoedaResposta;
import br.com.alura.conversordemoedas.service.ConsultaCotacaoService;
import br.com.alura.conversordemoedas.service.ConversorService;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConversorServiceTest {

    @Test
    @DisplayName("Deve converter com sucesso utilizando taxa retornada da moeda base")
    void deveConverterComSucesso() {
        ConsultaCotacaoService serviceFake = new ConsultaCotacaoService("fake-key") {
            @Override
            public MoedaResposta consultarCotacao(String moedaBase) {
                return new MoedaResposta(
                        "success",
                        "USD",
                        Map.of("BRL", 5.50, "EUR", 0.90),
                        null,
                        "Fri, 20 Nov 2026 00:00:00 +0000"
                );
            }
        };

        ConversorService conversor = new ConversorService(serviceFake);
        Conversao conversao = conversor.converter("USD", "BRL", 100.0);

        assertNotNull(conversao);
        assertEquals("USD", conversao.moedaOrigem());
        assertEquals("BRL", conversao.moedaDestino());
        assertEquals(100.0, conversao.valorOriginal(), 0.0001);
        assertEquals(5.50, conversao.taxaUtilizada(), 0.0001);
        assertEquals(550.0, conversao.valorConvertido(), 0.0001);
        assertNotNull(conversao.dataHora());

        assertEquals(1, conversor.getQuantidadeConversoes());
        assertTrue(conversor.temHistorico());
    }

    @Test
    @DisplayName("Deve retornar taxa 1.0 quando origem e destino forem a mesma moeda sem chamar a API")
    void deveRetornarTaxaUmParaMesmaMoeda() {
        ConsultaCotacaoService serviceFake = new ConsultaCotacaoService("fake-key") {
            @Override
            public MoedaResposta consultarCotacao(String moedaBase) {
                fail("Não deveria consultar a API quando moedas são iguais");
                return null;
            }
        };

        ConversorService conversor = new ConversorService(serviceFake);
        Conversao conversao = conversor.converter("BRL", "BRL", 250.0);

        assertEquals(1.0, conversao.taxaUtilizada(), 0.0001);
        assertEquals(250.0, conversao.valorConvertido(), 0.0001);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar converter com moeda de destino inexistente")
    void deveLancarExcecaoParaMoedaDestinoInexistente() {
        ConsultaCotacaoService serviceFake = new ConsultaCotacaoService("fake-key") {
            @Override
            public MoedaResposta consultarCotacao(String moedaBase) {
                return new MoedaResposta(
                        "success",
                        "USD",
                        Map.of("BRL", 5.50),
                        null,
                        null
                );
            }
        };

        ConversorService conversor = new ConversorService(serviceFake);
        assertThrows(ErroConsultaApiException.class, () -> conversor.converter("USD", "XYZ", 100.0));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para valores menores ou iguais a zero")
    void deveLancarExcecaoParaValorNegativoOuZero() {
        ConversorService conversor = new ConversorService(new ConsultaCotacaoService("fake-key"));

        assertThrows(IllegalArgumentException.class, () -> conversor.converter("USD", "BRL", 0.0));
        assertThrows(IllegalArgumentException.class, () -> conversor.converter("USD", "BRL", -50.0));
    }

    @Test
    @DisplayName("Deve desserializar JSON da ExchangeRate-API via Gson corretamente no record MoedaResposta")
    void deveDesserializarJsonCorretamente() {
        String json = """
                {
                    "result": "success",
                    "base_code": "USD",
                    "time_last_update_utc": "Fri, 20 Nov 2026 00:00:00 +0000",
                    "conversion_rates": {
                        "USD": 1.0,
                        "BRL": 5.75,
                        "EUR": 0.92
                    }
                }
                """;

        Gson gson = new Gson();
        MoedaResposta resposta = gson.fromJson(json, MoedaResposta.class);

        assertTrue(resposta.isSucesso());
        assertEquals("USD", resposta.baseCode());
        assertEquals(5.75, resposta.obterTaxaPara("BRL"), 0.0001);
        assertEquals(0.92, resposta.obterTaxaPara("eur"), 0.0001);
        assertNull(resposta.obterTaxaPara("JPY"));
    }

    @Test
    @DisplayName("Deve lançar ErroConsultaApiException quando a chave da API estiver ausente")
    void deveLancarExcecaoQuandoChaveEstiverAusente() {
        ConsultaCotacaoService serviceSemChave = new ConsultaCotacaoService((String) null);
        assertThrows(ErroConsultaApiException.class, () -> serviceSemChave.consultarCotacao("USD"));
    }
}
