import axios from 'axios';

// 백엔드의 MarketSyncController 주소
const BASE_URL = 'http://localhost:8080/api/admin/sync';

export const syncApi = {
    /**
     * 카페24 전체 상품 동기화를 백그라운드에서 실행하도록 트리거합니다.
     */
    syncAllCafe24: async () => {
        try {
            const response = await axios.post(`${BASE_URL}/cafe24/all`);
            return response.data;
        } catch (error) {
            console.error("카페24 동기화 요청 실패:", error);
            throw error;
        }
    },

    // 나중에 여기에 syncAllSmartStore, syncAllCoupang 등을 추가하시면 됩니다!
};