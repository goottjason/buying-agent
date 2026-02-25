package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/* 💡 왜 @RequiredArgsConstructor를 쓰면 안 될까요?
@RequiredArgsConstructor는 스프링이 주입해 주는 List<Port>를 그저 필드에 **있는 그대로 '대입'**해 줄 뿐입니다.

하지만 우리는 리스트를 그냥 가지고 있는 게 아니라, 꺼내 쓰기 편하게 **MarketType을 Key로 가지는 Map으로 변환(가공)**해야 합니다.
만약 @RequiredArgsConstructor를 쓰면서 Map으로 만들려면 필드에서 final 키워드를 빼고 @PostConstruct 같은 별도의 초기화 메서드를 써야 하는데,
이러면 '객체의 불변성(Immutability)'이 깨지게 됩니다.

그래서 **"생성자 내부에서 List를 Map으로 싹 변환한 뒤, 안전하게 final 필드에 꽂아 넣는 방식"**이 멀티스레드 환경에서도 버그를 만들지 않는 가장 완벽한 설계입니다! */


@Component
public class MarketPortFactory {

  // 🚀 final로 선언하여 불변성(안전성) 보장!
  private final Map<MarketType, MarketProductReaderPort> readerPortMap;
  private final Map<MarketType, MarketDataExtractorPort> extractorPortMap;
  private final Map<MarketType, MarketCommandPort> commandPortMap;

  // 스프링이 각 인터페이스를 구현한 어댑터들을 List로 싹 다 긁어와서 주입해 줍니다.
  // (예: Cafe24ProductAdapter가 3개 다 구현했다면 3곳 모두에 주입됨)
  public MarketPortFactory(
      List<MarketProductReaderPort> readerPorts,
      List<MarketDataExtractorPort> extractorPorts,
      List<MarketCommandPort> commandPorts
  ) {
    this.readerPortMap = readerPorts.stream()
        .collect(Collectors.toMap(MarketProductReaderPort::getSupportedMarket, Function.identity()));

    this.extractorPortMap = extractorPorts.stream()
        .collect(Collectors.toMap(MarketDataExtractorPort::getSupportedMarket, Function.identity()));

    this.commandPortMap = commandPorts.stream()
        .collect(Collectors.toMap(MarketCommandPort::getSupportedMarket, Function.identity()));
  }

  // =========================================================================
  // 🚀 용도별로 정확한 포트를 꺼내주는 게터(Getter) 메서드들
  // =========================================================================

  public MarketProductReaderPort getReaderPort(MarketType marketType) {
    MarketProductReaderPort port = readerPortMap.get(marketType);
    if (port == null) {
      throw new IllegalArgumentException("아직 지원하지 않는 마켓입니다 (Reader): " + marketType);
    }
    return port;
  }

  public MarketDataExtractorPort getExtractorPort(MarketType marketType) {
    MarketDataExtractorPort port = extractorPortMap.get(marketType);
    if (port == null) {
      throw new IllegalArgumentException("아직 지원하지 않는 마켓입니다 (Extractor): " + marketType);
    }
    return port;
  }

  public MarketCommandPort getCommandPort(MarketType marketType) {
    MarketCommandPort port = commandPortMap.get(marketType);
    if (port == null) {
      throw new IllegalArgumentException("아직 지원하지 않는 마켓입니다 (Command): " + marketType);
    }
    return port;
  }
}