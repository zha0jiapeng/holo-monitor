package org.dromara.common.core.utils.sd400mp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Component
public class GlbProperties implements ApplicationContextAware {

    public static String uri;

    public static String username;

    public static String password;
    
    @Value("${glb.uri}")
    public void setUri(String glbUri) {
        uri = glbUri;
    }
    
    @Value("${glb.username}")
    public void setUsername(String glbUsername) {
        username = glbUsername;
    }
    
    @Value("${glb.password}")
    public void setPassword(String glbPassword) {
        password = glbPassword;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {}
}