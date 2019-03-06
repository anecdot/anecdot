package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import info.anecdot.content.Webdav;
import info.anecdot.servlet.FileLoaderDecorator;
import info.anecdot.servlet.FileResourceResolver;
import org.apache.catalina.connector.Connector;
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
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.RequestDispatcher;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;

@SpringBootApplication(exclude = {
		ThymeleafAutoConfiguration.class})
@PropertySources({
		@PropertySource("default.properties")})
//		@PropertySource(ignoreResourceNotFound = true, value = {
//				"file:./anecdot.properties",
//				"file:/etc/anecdot/anecdot.properties"})})
@Import({Launcher.Web.class})
@EnableCaching
@EnableAsync(proxyTargetClass=true)
public class Launcher implements ApplicationRunner {

	private static List<String> getProperties(PropertyResolver propertyResolver, String key, List<String> defaultValues) {
		class StringArrayList extends ArrayList<String> {
			private StringArrayList(Collection<? extends String> c) {
				super(c);
			}

			public StringArrayList() { }
		}

		return propertyResolver.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
	}

	private static List<String> getProperties(PropertyResolver propertyResolver, String key) {
		return getProperties(propertyResolver, key, Collections.emptyList());
	}

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
			return new FileLoaderDecorator(new FileLoader(), siteService);
		}
	}

	@Autowired
	private SiteService siteService;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		Environment environment = applicationContext.getEnvironment();
		List<String> keys = getProperties(environment, "anecdot.sites");

		List<Site> sites = new ArrayList<>();

		for (String key : keys) {
			String prefix = String.format("anecdot.site.%s", key);
			String host = environment.getProperty(prefix + ".host");

//			Cache cache = cacheManager.getCache("sites");
//			cache.evict(name);

			Site site = siteService.findSiteByHost(host);
			if (site == null) {
				site = new Site();
				site.setHost(host);
//				site = siteService.saveSite(site);
			}

//            List<String> names = getProperties(propertyResolver, prefix + ".aliases");
//            site.getAliases().addAll(names);

			String theme = environment.getProperty(prefix + ".theme");
			if (StringUtils.hasText(theme)) {
				site.setTheme(Paths.get(theme));
			}

			String home = environment.getProperty(prefix + ".home", "/home");
			site.setHome(home);

			Locale locale = environment.getProperty(prefix + ".locale", Locale.class);
			site.setLocale(locale);

			Webdav webdav = new Webdav();
			String url = environment.getProperty(prefix + ".webdav.url");
			webdav.setUrl(url);
			String username = environment.getProperty(prefix + ".webdav.username");
			webdav.setUsername(username);
			String password = environment.getProperty(prefix + ".webdav.password");
			webdav.setPassword(password);
			site.setWebdav(webdav);

			site.setHome(home);

			site = siteService.saveSite(site);
			sites.add(site);

			siteService.sync(site);
		}

		siteService.deleteSitesNotIn(sites);
	}

	public static void main(String[] args) {
		SpringApplication.run(Launcher.class, args);
	}
}
