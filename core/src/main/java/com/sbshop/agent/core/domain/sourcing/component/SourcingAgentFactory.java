package com.sbshop.agent.core.domain.sourcing.component;

import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SourcingAgentFactory {

    private final Map<SourcingSiteCode, SourcingAgent> agentMap;

    public SourcingAgentFactory(List<SourcingAgent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(SourcingAgent::getSiteCode, Function.identity()));
    }

    /**
     * 사이트 코드에 맞는 SourcingAgent 반환
     */
    public SourcingAgent getAgent(SourcingSiteCode siteCode) {
        SourcingAgent agent = agentMap.get(siteCode);
        if (agent == null) {
            throw new IllegalArgumentException("지원하지 않는 소싱처 코드입니다: " + siteCode);
        }
        return agent;
    }
    
    /**
     * URL을 기반으로 적절한 SourcingAgent 추론 후 반환
     */
    public SourcingAgent getAgentByUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL이 null입니다.");
        }
        
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("iherb.com")) {
            return getAgent(SourcingSiteCode.IHERB);
        } else if (lowerUrl.contains("amazon.com")) {
            return getAgent(SourcingSiteCode.AMAZON_US);
        } else if (lowerUrl.contains("amazon.co.uk")) {
            return getAgent(SourcingSiteCode.AMAZON_UK);
        } else if (lowerUrl.contains("ocado.com")) {
            return getAgent(SourcingSiteCode.OCADO);
        } else if (lowerUrl.contains("tesco.com")) {
            return getAgent(SourcingSiteCode.TESCO);
        }
        
        throw new IllegalArgumentException("URL에 해당하는 소싱 에이전트를 찾을 수 없습니다: " + url);
    }
}
