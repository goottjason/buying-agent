package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MarketPortFactory {

  private final Map<MarketType, MarketProductPort> portMap;

  // 스프링이 MarketProductPort를 구현한 모든 어댑터(Cafe24, Coupang 등)를 List로 다 모아서 줍니다!
  public MarketPortFactory(List<MarketProductPort> ports) {
    // 이걸 MarketType을 Key로 하는 Map으로 예쁘게 묶어둡니다.
    this.portMap = ports.stream()
        .collect(Collectors.toMap(MarketProductPort::getSupportedMarket, Function.identity()));
  }

  // 마켓 타입을 주면, 그에 맞는 통신 어댑터를 꺼내줍니다.
  public MarketProductPort getPort(MarketType marketType) {
    MarketProductPort port = portMap.get(marketType);
    if (port == null) {
      throw new IllegalArgumentException("아직 지원하지 않는 마켓입니다: " + marketType);
    }
    return port;
  }
}