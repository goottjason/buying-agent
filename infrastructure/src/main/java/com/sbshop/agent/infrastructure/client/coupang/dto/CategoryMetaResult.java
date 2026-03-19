package com.sbshop.agent.infrastructure.client.coupang.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record CategoryMetaResult(
    List<CoupangProductPayload.Item.Attribute> attributes,
    List<CoupangProductPayload.Item.Notice> notices
) {}