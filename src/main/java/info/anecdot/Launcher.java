package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.SiteService;
import info.anecdot.io.PathWatcher;
import info.anecdot.servlet.FileLoaderDecorator;
import info.anecdot.servlet.FileResourceResolver;
import org.apache.catalina.connector.Connector;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.RequestDispatcher;
import java.io.FileNotFoundException;
import java.util.List;

@SpringBootApplication(exclude = {
		ThymeleafAutoConfiguration.class})
@PropertySources({
		@PropertySource("default.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = {
				"file:./anecdot.properties",
				"file:/etc/anecdot/anecdot.properties"})})
@Import({Launcher.Web.class, Launcher.WebSecurity.class})
@EnableCaching
@EnableAsync(proxyTargetClass=true)
public class Launcher implements ApplicationRunner {

	@Autowired
	private ApplicationContext applicationContext;

	protected class Web implements WebMvcConfigurer {

		@Bean
		@Scope("prototype")
		@ConfigurationProperties(prefix = "server.ajp", ignoreInvalidFields = true)
		protected Connector ajpConnector() {
			return new Connector("AJP/1.3");
		}

		@Bean
		@ConditionalOnProperty(name = "server.ajp.port")
		public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webServerFactoryCustomizer(Connector ajpConnector) {
			return factory -> {
				factory.addAdditionalTomcatConnectors(ajpConnector);
			};
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			FileResourceResolver fileResourceResolver =
					applicationContext.getBean(FileResourceResolver.class);

			registry.addResourceHandler("/**")
					.setCacheControl(CacheControl.empty())
					.resourceChain(false)
					.addResolver(fileResourceResolver);
		}

		@Override
		public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
			resolvers.add((request, response, handler, e) -> {
				Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
				if (status == null) {

					if (e instanceof FileNotFoundException) {
						status = HttpStatus.NOT_FOUND.value();
					} else {
						status = HttpStatus.INTERNAL_SERVER_ERROR.value();
					}
				}

				ModelAndView modelAndView = new ModelAndView();
				modelAndView.setViewName("error");
				modelAndView.addObject("status", status);
				modelAndView.addObject("message", e.getMessage());
				modelAndView.addObject("e", e);

//				RequestLocaleResolver localeResolver = (RequestLocaleResolver) localeResolver();
//                modelAndView.addObject("locale", localeResolver.resolveLocale(request));

				e.printStackTrace();

				return modelAndView;
			});
		}

		@Bean(name = "pebbleLoader")
		protected Loader<?> fileLoaderDecorator(SiteService siteService) {
			return new FileLoaderDecorator(siteService, new FileLoader());
		}
	}

	@EnableWebSecurity
	@ComponentScan(
			basePackageClasses = KeycloakSecurityComponents.class,
			excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
					pattern = "org.keycloak.adapters.springsecurity.management.HttpSessionManager"))
	@ConditionalOnProperty(value = "anecdot.security", havingValue = "enabled")
	protected class WebSecurity extends KeycloakWebSecurityConfigurerAdapter {

		@Bean
		public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
			return new KeycloakSpringBootConfigResolver();
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) {
			KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
			keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
			auth.authenticationProvider(keycloakAuthenticationProvider);
		}

		@Override
		protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
			return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			super.configure(http);
			http.authorizeRequests()
					.antMatchers("/theme/**").permitAll()
					.anyRequest().access("@securityService.hasAccess()");
		}
	}

	@Autowired
	private SiteService siteService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		siteService.config();

		siteService.getAllSites().forEach(site -> {
			PathWatcher watcher = siteService.getWatcher(site);
			siteService.start(watcher);
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(Launcher.class, args);
	}
}
