package com.sbshop.agent.core.domain.sourcing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageHashSyncService {

    /**
     * 이미지 Hash가 변경되었을 경우, 해당 이미지들을 Cloudflare R2(S3 호환)에 업로드하고 CDN URL을 반환합니다.
     */
    public String syncMainImage(String sourceMainImageUrl, String newHash) {
        if (sourceMainImageUrl == null || sourceMainImageUrl.isEmpty()) return null;
        
        log.info("새로운 메인 이미지 해시 감지({}). Cloudflare R2에 업로드를 시도합니다: {}", newHash, sourceMainImageUrl);
        // TODO: S3 업로드 로직 (S3Client)
        // 여기서는 Phase 4 데모를 위해 가상의 CDN URL을 반환합니다.
        
        return "https://cdn.antigravity-sync.com/images/main/" + newHash + ".jpg";
    }

    public List<String> syncAdditionalImages(List<String> sourceAdditionalImageUrls, String productCode) {
        if (sourceAdditionalImageUrls == null || sourceAdditionalImageUrls.isEmpty()) return List.of();
        
        log.info("상품코드 [{}]의 추가 이미지 {}장 Cloudflare R2에 업로드 시도.", productCode, sourceAdditionalImageUrls.size());
        
        // TODO: S3 병렬 업로드 로직
        int[] index = {1};
        return sourceAdditionalImageUrls.stream()
                .map(url -> "https://cdn.antigravity-sync.com/images/add/" + productCode + "_" + (index[0]++) + ".jpg")
                .collect(Collectors.toList());
    }
}
