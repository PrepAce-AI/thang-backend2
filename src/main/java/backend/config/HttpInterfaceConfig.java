//package backend.config;
//import backend.client.GoogleUserInfoClient;
//import backend.client.GoogleTokenClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.service.invoker.HttpServiceProxyFactory;
//import org.springframework.web.reactive.function.client.support.WebClientAdapter;
//
//@Configuration
//public class HttpInterfaceConfig {
//    @Bean
//    GoogleTokenClient googleTokenClient() {
//        WebClient webClient = WebClient.create();
//        HttpServiceProxyFactory factory =
//                HttpServiceProxyFactory
//                        .builderFor(WebClientAdapter.create(webClient))
//                        .build();
//        return factory.createClient(GoogleTokenClient.class);
//    }
//
//    @Bean
//    GoogleUserInfoClient googleUserInfoClient() {
//        WebClient webClient = WebClient.create();
//        HttpServiceProxyFactory factory =
//                HttpServiceProxyFactory
//                        .builderFor(WebClientAdapter.create(webClient))
//                        .build();
//        return factory.createClient(GoogleUserInfoClient.class);
//    }
//}
