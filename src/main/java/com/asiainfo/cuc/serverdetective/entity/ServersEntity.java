package com.asiainfo.cuc.serverdetective.entity;


import com.asiainfo.cuc.serverdetective.config.ServerBeanFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Setter
@Getter
@PropertySource(value = {"classpath:/servers.yml"}, encoding = "utf-8",factory = ServerBeanFactory.class)
@ConfigurationProperties(prefix = "settings.servers")
public class ServersEntity {
    private List<Server> list = new ArrayList<>();
}
