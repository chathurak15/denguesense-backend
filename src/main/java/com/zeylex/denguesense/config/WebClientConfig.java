package com.zeylex.denguesense.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
@Configuration
public class WebClientConfig {

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.timeout.connect-ms}")
    private int connectTimeoutMs;

    @Value("${ai-service.timeout.read-ms}")
    private int readTimeoutMs;

    @Bean
    public WebClient aiServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "telegramWebClient")
    public WebClient telegramWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "openMeteoWebClient")
    public WebClient openMeteoWebClient(
            @Value("${open-meteo.base-url:https://archive-api.open-meteo.com}") String openMeteoBaseUrl,
            @Value("${open-meteo.timeout.connect-ms:10000}") int openMeteoConnectTimeoutMs,
            @Value("${open-meteo.timeout.read-ms:30000}") int openMeteoReadTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, openMeteoConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(openMeteoReadTimeoutMs));

        return WebClient.builder()
                .baseUrl(openMeteoBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
