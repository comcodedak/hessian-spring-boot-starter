package com.codedak.hessian.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.codedak.hessian.spring.annotation.EnableHessianAutoScan;
import com.codedak.hessian.spring.annotation.HessianClient;
import com.codedak.hessian.spring.annotation.HessianService;


/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Venil Noronha
 * @author hunter
 * 2017 08 20
 */
public class HessianAutoScanRegistrar implements ImportBeanDefinitionRegistrar,
		ResourceLoaderAware, BeanClassLoaderAware {


	private ResourceLoader resourceLoader;

	private ClassLoader classLoader;

	public HessianAutoScanRegistrar() {
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);

		

		Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableHessianAutoScan.class.getName());
		//0.设置基础包
		Set<String> basePackages=getBasePackages(attrs,metadata.getClassName());
		//1.设置过滤包　　防止重复注册
		Class<?>[] clients = attrs == null ? null: (Class<?>[]) attrs.get("clients");
		Class<?>[] services = attrs == null ? null: (Class<?>[]) attrs.get("services");
		final Set<String> classes = new HashSet<>();
		if(clients != null && clients.length != 0) {
			for (Class<?> clazz : clients) {
				basePackages.add(ClassUtils.getPackageName(clazz));
				classes.add(clazz.getCanonicalName());
			}
		}
		if(services != null && services.length != 0) {
			for (Class<?> clazz : services) {
				basePackages.add(ClassUtils.getPackageName(clazz));
				classes.add(clazz.getCanonicalName());
			}
		}
		List<TypeFilter>  filters=new ArrayList<>();
		/*
		 * ３.添加EnableHessianAutoScan的clients扫描
		 */
		filters.add(new AnnotationTypeFilter(HessianClient.class));
		/**
		 * ４.添加EnableHessianAutoScan的services扫描
		 */
		filters.add(new AnnotationTypeFilter(HessianService.class));
		if(!classes.isEmpty()) {
			AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
				@Override
				protected boolean match(ClassMetadata metadata) {
					String cleaned = metadata.getClassName().replaceAll("\\$", ".");
					return classes.contains(cleaned);
				}
			};
			filters.add(filter);
		}
		scanner.addIncludeFilter(new AllTypeFilter(filters));
		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner
					.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					
					
					if(annotationMetadata.isAnnotated(HessianClient.class.getName())) {
						Assert.isTrue(annotationMetadata.isInterface(),"@HessianClient can only be specified on an interface");
						registerClient(annotationMetadata,registry);
					}else if(annotationMetadata.isAnnotated(HessianService.class.getName())) {
						Assert.isTrue(annotationMetadata.isInterface(),"@HessianService can only be specified on an interface");
						registerService(annotationMetadata, registry);
					}

				}
			}
		}
		
		
	}
	/**
	   * 发布hessian服务
	   * @param importingClassMetadata
	   * @param registry
	   */
	  private void registerService(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		  AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(HessianService.class.getName()));
			String name = annoAttrs.getString("name");
			String  clsName=importingClassMetadata.getClassName();
			//注册当前bean
			GenericBeanDefinition def = new GenericBeanDefinition();  
			def.setBeanClassName(clsName);
			def.setSynthetic(true);   
		    registry.registerBeanDefinition(name, def);  
			
		   
		    //发布hessian服务
		    Class<?> interfaces = annoAttrs.getClass("interfaces");
			BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(HessianServiceExporter.class);
			definition.addPropertyValue("serviceInterface", interfaces);
			definition.addPropertyValue("service", def);
			
			AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
			beanDefinition.setPrimary(true);//设置优先
			
			BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, "/"+name,
					new String[] { });
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	  }
	  /**
	   * 注册客户端
	   * @param importingClassMetadata
	   * @param registry
	   */
		private void registerClient(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			AnnotationAttributes annoAttrs = AnnotationAttributes
					.fromMap(importingClassMetadata.getAnnotationAttributes(HessianClient.class.getName()));
			Class<?> clsName = null;
			try {
				clsName = Class.forName(importingClassMetadata.getClassName());
			}catch (Exception e) {
				// TODO: handle exception
			}
			if(clsName==null) {
				return;
			}
			String name = annoAttrs.getString("name");
			String url = annoAttrs.getString("url");
			String linkUrl = annoAttrs.getString("linkUrl");
			

			// 注册当前bean
			BeanDefinitionBuilder definition = BeanDefinitionBuilder
					.genericBeanDefinition(LbHessianProxyFactoryBean.class);
			definition.addPropertyValue("serviceUrl", "http://" + linkUrl + "/" + name);
			definition.addPropertyValue("serviceInterface",clsName);

			AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
			beanDefinition.setPrimary(true);// 设置优先

			BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, name, new String[] {});
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
		}

	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false) {

			@Override
			protected boolean isCandidateComponent(
					AnnotatedBeanDefinition beanDefinition) {
				if (beanDefinition.getMetadata().isIndependent()) {
					// TODO until SPR-11711 will be resolved
					if (beanDefinition.getMetadata().isInterface()
							&& beanDefinition.getMetadata()
									.getInterfaceNames().length == 1
							&& Annotation.class.getName().equals(beanDefinition
									.getMetadata().getInterfaceNames()[0])) {
						try {
							Class<?> target = ClassUtils.forName(
									beanDefinition.getMetadata().getClassName(),
									HessianAutoScanRegistrar.this.classLoader);
							return !target.isAnnotation();
						}
						catch (Exception ex) {
							this.logger.error(
									"Could not load target class: "
											+ beanDefinition.getMetadata().getClassName(),
									ex);

						}
					}
					return true;
				}
				return false;

			}
		};
	}

	protected Set<String> getBasePackages(Map<String, Object> attributes,String importClassName) {
		Set<String> basePackages = new HashSet<>();
		for (String pkg : (String[]) attributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		if (basePackages.isEmpty()) {
			basePackages.add(
					ClassUtils.getPackageName(importClassName));
		}
		return basePackages;
	}
	
	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates
	 * match.
	 *
	 * @author Oliver Gierke
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 *
		 * @param delegates must not be {@literal null}.
		 */
		public AllTypeFilter(List<TypeFilter> delegates) {

			Assert.notNull(delegates);
			this.delegates = delegates;
		}

		@Override
		public boolean match(MetadataReader metadataReader,
				MetadataReaderFactory metadataReaderFactory) throws IOException {

			for (TypeFilter filter : this.delegates) {
				if (filter.match(metadataReader, metadataReaderFactory)) {
					return true;
				}
			}

			return false;
		}
	}
}
