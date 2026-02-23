import axios from 'axios';

// 백엔드 기본 주소 (필요에 따라 변경하세요)
const BASE_URL = 'http://localhost:8099/api/users';

export const userApi = {
    /**
     * 특정 메뉴(예: PRODUCT_GRID)의 유저 설정값을 DB에서 불러옵니다.
     * @param {string} menuId - 설정을 불러올 메뉴의 고유 ID
     * @returns {Promise<string>} - 저장된 설정값 (JSON 문자열), 없으면 null
     */
    getPreference: async (menuId) => {
        try {
            // GET /api/users/preferences/PRODUCT_GRID
            // 주의: 아직 로그인 기능이 없다면 백엔드에서 임시로 'ADMIN' 유저를 기준으로 응답하도록 짤 예정입니다.
            const response = await axios.get(`${BASE_URL}/preferences/${menuId}`);

            // 백엔드에서 값이 없으면 빈 문자를 줄 수 있으므로 체크합니다.
            return response.data?.preferenceData || null;
        } catch (error) {
            // 최초 접속이라 DB에 저장된 세팅이 없을 때(404)는 자연스럽게 null을 반환합니다.
            if (error.response && error.response.status === 404) {
                return null;
            }
            console.error("설정 불러오기 실패:", error);
            return null;
        }
    },

    /**
     * 특정 메뉴의 유저 설정값을 DB에 저장합니다.
     * @param {string} menuId - 설정을 저장할 메뉴의 고유 ID
     * @param {string} preferenceData - 저장할 데이터 (AG Grid의 상태 JSON 문자열)
     */
    savePreference: async (menuId, preferenceData) => {
        try {
            // PUT /api/users/preferences
            const response = await axios.put(`${BASE_URL}/preferences`, {
                menuId: menuId,
                preferenceData: preferenceData
            });
            return response.data;
        } catch (error) {
            console.error("설정 저장 실패:", error);
            throw error;
        }
    }
};