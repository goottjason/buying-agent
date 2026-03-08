package com.sbshop.agent.api.product.dto;

public record PriceStockUpdateRequest(
    Integer price,
    Integer stock
) {}