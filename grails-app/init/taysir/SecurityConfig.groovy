package taysir
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.session.HttpSessionEventPublisher

@Configuration
@EnableWebSecurity
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(keycloakAuthenticationProvider())
    }

    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl())
    }

    @Bean
    ServletListenerRegistrationBean<HttpSessionEventPublisher> getHttpSessionEventPublisher() {
        new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher())
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure http
        http
                .csrf().disable()
                .logout()
                .logoutSuccessUrl("/sso/login") // Override Keycloak's default '/'
                .and()
                .authorizeRequests()
                .antMatchers("/assets/*").permitAll()
                .antMatchers("/index").fullyAuthenticated()
                .anyRequest().authenticated()
    }


}
