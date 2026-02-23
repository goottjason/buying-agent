package com.sbshop.agent.api.user.controller;

import com.sbshop.agent.api.user.processor.UserPreferenceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/preferences")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserPreferenceProcessor processor;

  // 프론트엔드에서 보낼 JSON 데이터를 받을 DTO (컨트롤러 내부에 간단히 생성)
  public record PreferenceSaveRequest(String menuId, String preferenceData) {}

  /**
   * 특정 메뉴의 설정값 조회 API
   * GET /api/users/preferences/{menuId}
   */
  @GetMapping("/{menuId}")
  public ResponseEntity<?> getPreference(@PathVariable("menuId") String menuId) {
    log.info("설정 조회 요청 - 메뉴: {}", menuId);

    return processor.getPreferenceData(menuId)
        // 설정이 있으면 JSON 응답 (프론트에서 response.data.preferenceData 로 받음)
        .map(data -> ResponseEntity.ok(Map.of("preferenceData", data)))
        // 아직 저장한 적이 없으면 HTTP 404 Not Found 응답 (프론트에서 null로 처리됨)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * 특정 메뉴의 설정값 저장 API
   * PUT /api/users/preferences
   */
  @PutMapping
  public ResponseEntity<?> savePreference(@RequestBody PreferenceSaveRequest request) {
    log.info("설정 저장 요청 - 메뉴: {}", request.menuId());

    processor.saveOrUpdatePreference(request.menuId(), request.preferenceData());

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "설정이 성공적으로 저장되었습니다."
    ));
  }
}