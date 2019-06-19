package com.dub.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 
	@Value("myapp.allowed.origins")
	private String allowedOrigins;
	
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
 
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) { 
    	registry.addEndpoint("/chat").setAllowedOrigins(allowedOrigins).withSockJS();
    	registry.addEndpoint("/service").setAllowedOrigins(allowedOrigins).withSockJS();
       	registry.addEndpoint("/private").setAllowedOrigins(allowedOrigins).withSockJS();
        
    }
    
}