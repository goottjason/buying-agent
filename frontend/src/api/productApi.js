// 백엔드로 치면 ProductRepository 또는 FeignClient 역할
import axios from 'axios';

// 백엔드 서버의 기본 주소를 설정합니다.
const BASE_URL = 'http://localhost:8080/api/products';

export const productApi = {
    // 1. 다건 조회 및 검색 API 호출 (Step 1의 핵심!)
    // 파라미터로 keyword와 category를 받아서 백엔드에 전달합니다.
    fetchProducts: async (keyword = '', category = '') => {
        const response = await axios.get(BASE_URL, {
            params: {
                size: 5000,
                keyword: keyword,   // 검색어 조건
                category: category  // 카테고리 조건
            }
        });
        return response.data; // 스프링에서 보내준 Page 객체를 그대로 반환
    },

    // 2. 일괄 수정 (PATCH) API 호출
    bulkUpdate: async (skus, memo) => {
        const response = await axios.patch(`${BASE_URL}/bulk`, {
            skus: skus,
            memo: memo
        });
        return response.data;
    },

    // 3. 일괄 삭제 (DELETE) API 호출
    bulkDelete: async (skus) => {
        const response = await axios.delete(`${BASE_URL}/bulk`, {
            params: { skus: skus.join(",") } // 배열을 콤마 문자열로 변환
        });
        return response.data;
    },

    // 4. 단건 등록 (POST)
    createProduct: async (productData) => {
        // 아직 백엔드에 POST /api/products 가 없으므로 흉내만 냅니다.
        console.log("백엔드로 보낼 등록 데이터:", productData);
        alert("백엔드 POST API 연동이 필요합니다!");
        // 실제로는 아래 주석을 풉니다.
        // const response = await axios.post(BASE_URL, productData);
        // return response.data;
    },

    // 5. 단건 수정 (PUT)
    updateProduct: async (sku, productData) => {
        // 아직 백엔드에 PUT /api/products/{sku} 가 없으므로 흉내만 냅니다.
        console.log(`백엔드로 보낼 [${sku}] 수정 데이터:`, productData);
        alert("백엔드 PUT API 연동이 필요합니다!");
        // 실제로는 아래 주석을 풉니다.
        // const response = await axios.put(`${BASE_URL}/${sku}`, productData);
        // return response.data;
    },

    // 6. 엑셀 인라인 에디팅 일괄 저장 (PUT)
    bulkEdit: async (productDataList) => {
        const response = await axios.put(`${BASE_URL}/bulk-edit`, productDataList);
        return response.data;
    }
};