package com.sandbox.testproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class TestProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestProxyApplication.class, args);
    }

    static boolean transactional(Object o) {
        var hasTransaction = new AtomicBoolean(false);
        var classes = new ArrayList<Class<?>>();
        classes.add(o.getClass());
        Collections.addAll(classes, o.getClass().getInterfaces());
        classes.forEach(clzz -> ReflectionUtils.doWithMethods(clzz, method -> {
            if (method.getAnnotation(MyTransactional.class) != null) {
                hasTransaction.set(true);
            }
        }));
        return hasTransaction.get();
    }

    @Bean
    DefaultCustomerService defaultCustomerService() {
        return new DefaultCustomerService();
    }

    @Bean
    static MyTransactionalBeanPostProcessor myTransactionalBeanPostProcessor() {
        return new MyTransactionalBeanPostProcessor();
    }

    static class MyTransactionalBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (transactional(bean)) {
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setInterfaces(bean.getClass().getInterfaces());
                proxyFactory.setTarget(bean);
                proxyFactory.addAdvice((MethodInterceptor) invocation -> {
                    var method = invocation.getMethod();
                    var arguments = invocation.getArguments();
                    try {
                        if (method.getAnnotation(TestProxyApplication.MyTransactional.class) != null ) {
                            System.out.println("-- starting transaction for " + method.getName());
                        }
                        return method.invoke(bean, arguments);
                    } finally {
                        if (method.getAnnotation(TestProxyApplication.MyTransactional.class) != null ) {
                            System.out.println("finishing transaction for " + method.getName());
                        }
                    }
                });
                Object proxy = proxyFactory.getProxy(getClass().getClassLoader());
                for(var i : proxy.getClass().getInterfaces()) {
                    System.out.println(i.getName());
                }
                return proxy;

            }
            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
        }
    }

    @Bean
    ApplicationRunner appRunner(CustomerService customerService) {
        return new ApplicationRunner() {

            @Override
            public void run(ApplicationArguments args) throws Exception {
                customerService.createP();
                customerService.addP();
            }
        };

    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @Reflective
    public @interface MyTransactional {
    }

}
