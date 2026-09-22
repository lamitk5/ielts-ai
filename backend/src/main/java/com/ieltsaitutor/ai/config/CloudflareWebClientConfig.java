package com.ieltsaitutor.ai.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class CloudflareWebClientConfig {
    @Bean
    @Qualifier("cloudflareWebClient")
    WebClient cloudflareWebClient(AiProviderProperties properties) {
        var cloudflare = properties.getCloudflare();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) cloudflare.getConnectTimeout().toMillis())
                .responseTimeout(cloudflare.getResponseTimeout());
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
