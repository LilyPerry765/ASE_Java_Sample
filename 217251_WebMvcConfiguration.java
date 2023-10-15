package io.pivotal.security.config;

import io.pivotal.security.audit.AuditInterceptor;
import io.pivotal.security.controller.v1.CurrentUserAccessControlEntryResolver;
import io.pivotal.security.controller.v1.RequestUuidArgumentResolver;
import io.pivotal.security.controller.v1.UserContextArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {
  private final CurrentUserAccessControlEntryResolver currentUserAccessControlEntryResolver;
  private final UserContextArgumentResolver userContextArgumentResolver;
  private final RequestUuidArgumentResolver requestUuidArgumentResolver;
  private final AuditInterceptor auditInterceptor;

  @Autowired
  public WebMvcConfiguration(
      CurrentUserAccessControlEntryResolver currentUserAccessControlEntryResolver,
      UserContextArgumentResolver userContextArgumentResolver,
      RequestUuidArgumentResolver requestUuidArgumentResolver,
      AuditInterceptor auditInterceptor
  ) {
    this.currentUserAccessControlEntryResolver = currentUserAccessControlEntryResolver;
    this.userContextArgumentResolver = userContextArgumentResolver;
    this.requestUuidArgumentResolver = requestUuidArgumentResolver;
    this.auditInterceptor = auditInterceptor;
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.favorPathExtension(false);
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(false);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(currentUserAccessControlEntryResolver);
    argumentResolvers.add(userContextArgumentResolver);
    argumentResolvers.add(requestUuidArgumentResolver);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    super.addInterceptors(registry);
    registry.addInterceptor(auditInterceptor).excludePathPatterns("/info", "/health");
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.build();
  }
}
