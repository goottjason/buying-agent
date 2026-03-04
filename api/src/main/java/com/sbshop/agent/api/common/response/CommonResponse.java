package com.sbshop.agent.api.common.response;

import com.sbshop.agent.core.domain.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "API 공통 응답 포맷")
public class CommonResponse<T> {

  // 1. 응답의 기본 필드들 (불변성을 위해 final 사용)
  @Schema(description = "성공 여부", example = "true")
  private final boolean success;

  @Schema(description = "실제 데이터 본문 (성공했을 때만 들어있음)")
  private final T data;

  @Schema(description = "에러 메시지 (성공 시 null)", nullable = true, example = "null")
  private final String message;

  @Schema(description = "에러 코드 (성공 시 null)", nullable = true, example = "null")
  private final String errorCode;

  // 2. private 생성자: 외부에서 new CommonResponse(...) 하지 못하게 막음
  // 대신 아래에 있는 static 메서드(ok, error)들로만 생성하게 강제해서 일관성을 지킴
  private CommonResponse(boolean success, T data, String message, String errorCode) {
    this.success = success;
    this.data = data;
    this.message = message;
    this.errorCode = errorCode;
  }

  // ============================================================================================
  // 3. 성공했을 때 쓰는 메서드들 (static factory methods)
  // ============================================================================================

  /**
   * [성공] 데이터가 있을 때 사용 예: CommonResponse.ok(userDto) 결과: { success: true, data: { ... }, message:
   * null, errorCode: null }
   */
  public static <T> CommonResponse<T> ok(T data) {
    return new CommonResponse<>(true, data, null, null);
  }

  /**
   * [성공] 데이터가 없을 때 사용 (Void) 예: 회원가입 완료, 삭제 성공 등 데이터가 굳이 필요 없을 때 결과: { success: true, data: null,
   * message: null, errorCode: null }
   */
  public static <T> CommonResponse<T> ok() {
    return new CommonResponse<>(true, null, null, null);
  }

  // ============================================================================================
  // 4. 실패했을 때 쓰는 메서드들 (static factory methods)
  // ============================================================================================

  /**
   * [실패] 기본 에러 응답 ErrorCode에 정의된 기본 메시지를 그대로 내보냄 예:
   * CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR) 결과: { success: false, data: null,
   * message: "서버 에러입니다.", errorCode: "S001" }
   */
  public static <T> CommonResponse<T> error(ErrorCode errorCode) {
    return new CommonResponse<>(false, null, errorCode.getMessage(), errorCode.getCode());
  }

  /**
   * [실패] 커스텀 메시지 에러 응답 ErrorCode의 코드는 유지하되, 메시지만 "회사 ID가 필수입니다" 처럼 바꾸고 싶을 때 사용 예:
   * CommonResponse.error(ErrorCode.INVALID_INPUT_VALUE, "ID는 양수여야 합니다") 결과: { success: false, data:
   * null, message: "ID는 양수여야 합니다", errorCode: "C001" }
   */
  public static <T> CommonResponse<T> error(ErrorCode errorCode, String customMessage) {
    return new CommonResponse<>(false, null, customMessage, errorCode.getCode());
  }
}
