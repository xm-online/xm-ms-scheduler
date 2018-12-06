package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.XmWebMvcConfigurerAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig extends XmWebMvcConfigurerAdapter {

    private final ApplicationProperties appProps;

    protected WebMvcConfig(TenantInterceptor tenantInterceptor,
                                  XmLoggingInterceptor xmLoggingInterceptor,
                                  ApplicationProperties appProps) {
        super(tenantInterceptor, xmLoggingInterceptor);
        this.appProps = appProps;
    }

    @Override
    protected void xmAddInterceptors(final InterceptorRegistry registry) {
    }

    @Override
    protected void xmConfigurePathMatch(PathMatchConfigurer configurer) {

    }

    @Override
    protected List<String> getTenantIgnorePathPatterns() {
        return appProps.getTenantIgnoredPathList();
    }
}
