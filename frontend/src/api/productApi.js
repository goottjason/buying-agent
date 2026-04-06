import axios from "axios";

const BASE_URL = "http://localhost:8099/api/v1/products";

// 1. 상품 그리드 목록 조회 (page, size 파라미터 외에 keyword(검색어)도 받을 수 있도록 확장)
export const fetchProductsApi = (page = 0, size = 50, keyword = "") => {
  // 백엔드 API가 /api/products?page=0&size=50&keyword=비타민 형태로 받을 수 있게 셋팅
  return axios.get(`${BASE_URL}?page=${page}&size=${size}&keyword=${keyword}`);
};

/**
 * 🚀 상품 단건 상세 조회 (대왕 모달용)
 */
export const fetchProductDetailApi = async (id) => {
  try {
    const response = await axios.get(`${BASE_URL}/${id}`);
    return response.data;
  } catch (error) {
    console.error("상품 상세 조회 에러:", error);
    throw error;
  }
};

// 2. 마켓 로컬 데이터 조회 (모달창 열 때)
export const fetchMarketLocalApi = (productId, marketType) =>
  axios.get(`${BASE_URL}/${productId}/markets/${marketType}/local`);

// 3. 마켓 라이브 동기화 (모달창에서 동기화 버튼 누를 때)
export const syncMarketLiveApi = (productId, marketType) =>
  axios.post(`${BASE_URL}/${productId}/markets/${marketType}/sync`);

// 4. 가격/재고 일괄 수정 및 브로드캐스트
export const updatePriceStockApi = (productId, data) =>
  axios.put(`${BASE_URL}/${productId}/price-stock`, data);

// 5. 🚀 [신규] 이미지 파일 업로드 및 HTML 동기화 (Multipart-form)
export const uploadProductImagesApi = (productId, formData) =>
  axios.put(`${BASE_URL}/${productId}/images`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

/**
 * 🚀 선택된 상품 배열을 백엔드 DB에 일괄 저장(Bulk Insert)합니다.
 */
export const saveProductsBulk = async (productArray) => {
  try {
    // Axios로 POST 요청 쏘기
    const response = await axios.post(`${BASE_URL}/bulk`, productArray);
    return response.data; // CommonResponse 규격 객체 반환
  } catch (error) {
    console.error("상품 저장 API 호출 중 에러:", error);
    throw error;
  }
};

/**
 * 🚀 특정 마켓으로 상품 신규 등록 요청
 */
export const publishToMarketApi = async (productId, marketType) => {
    return await axios.post(`${BASE_URL}/${productId}/markets/${marketType}`);
};

/**
 * 🚀 [신규] 아이허브 소싱 URL에서 이미지 URL 리스트를 크롤링합니다.
 */
export const crawlProductImagesApi = async (productId) => {
  const response = await axios.get(`${BASE_URL}/${productId}/images/crawl`);
  return response.data; // CommonResponse 규격 { success, data: [...imageUrls] }
};

/**
 * 🚀 [신규] 크롤링된 이미지 URL 리스트를 서버에 전달하여
 * 서버가 다운로드 → R2 업로드 → DB 갱신 → 마켓 전파를 수행합니다.
 */
export const uploadProductImagesByUrlApi = async (productId, imageUrls) => {
  return await axios.put(`${BASE_URL}/${productId}/images/by-url`, imageUrls);
};
