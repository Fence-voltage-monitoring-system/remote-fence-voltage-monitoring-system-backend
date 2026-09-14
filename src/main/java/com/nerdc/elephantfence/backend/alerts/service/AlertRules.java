package com.nerdc.elephantfence.backend.alerts.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AlertRules {
    private final SystemConfigurationRepository configurations;
    private JsonNode value(String section){return configurations.findBySection(section).map(c->c.getConfigData()).orElse(JsonNodeFactory.instance.objectNode());}
    public int number(String name,int fallback){return Math.max(1,Math.min(10080,value("alerts").path(name).asInt(fallback)));}
    public boolean enabled(String name,boolean fallback){return value("alerts").path(name).asBoolean(fallback);}
    public boolean notifications(){return value("notifications").path("inAppEnabled").asBoolean(true)&&enabled("inAppEnabled",true);}
    public double voltage(String name,double fallback){double v=value("voltage").path(name).asDouble(fallback);return Double.isFinite(v)&&v>=0?v:fallback;}
}

