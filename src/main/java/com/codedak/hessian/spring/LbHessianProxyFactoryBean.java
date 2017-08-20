package com.codedak.hessian.spring;
import java.net.URI;
import java.util.HashMap;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

public class LbHessianProxyFactoryBean extends HessianProxyFactoryBean{
	@Autowired
	private  SpringClientFactory clientFactory;
	
	private volatile HashMap<String, HessianProxyFactoryBean> beans=new HashMap<>();
	
	

	
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		String superUrl=super.getServiceUrl();
		URI asUri = URI.create(superUrl);
		String clientName = asUri.getHost();
		ILoadBalancer lb=clientFactory.getLoadBalancer(clientName);
		Server server= lb.chooseServer("");
		if(beans.containsKey(server.getHostPort())) {
			return beans.get(server.getHostPort()).invoke(invocation);
		}
		HessianProxyFactoryBean bean=new HessianProxyFactoryBean();
		bean.setServiceUrl(getServiceUrl().replace(clientName, server.getHostPort()));
		bean.setServiceInterface(getServiceInterface());
		bean.prepare();
		
		beans.put(server.getHostPort(), bean);
	   return bean.invoke(invocation);
	}
}
