package com.liugh.config;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Configuration
public class ImageConfig extends WebMvcConfigurerAdapter{

	private static final Log logger = LogFactory.getLog(MethodHandles.lookup().lookupClass());	

	
	@Value("${wx.user.headImagesPath}")
    private String hImagesPath;
	
	@Value("${excel.path}")
	 private String downloadExcelPath;
	
	@Value("${download.dir:/usr/local/download/}")
	private String downloadDir;
		
	/**
	 * 图片地址映射
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {		
        registry.addResourceHandler("/images/**").addResourceLocations("file:"+hImagesPath);
        //registry.addResourceHandler("/index/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/**").addResourceLocations("file:"+downloadExcelPath);
		super.addResourceHandlers(registry);
	}

}
