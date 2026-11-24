package tbh.chat_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import tbh.chat_app.config.jwt.CustomUserDetailsService;
import tbh.chat_app.config.jwt.JwtUtils;
import tbh.chat_app.config.jwt.StompAuthenticationInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebsocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    public WebsocketConfiguration(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public TaskScheduler stompHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthenticationInterceptor(jwtUtils, userDetailsService));
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(10 * 1024 * 1024); // 10MB
        registration.setSendBufferSizeLimit(10 * 1024 * 1024); // 10MB
        registration.setSendTimeLimit(20000); // 20 seconds
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(stompHeartbeatScheduler())
                .setHeartbeatValue(new long[] {20_000, 20_000});
    }
}
