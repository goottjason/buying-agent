import axios from "axios";

// 백엔드의 Sourcing Controller 기본 주소
const BASE_URL = "http://localhost:8099/api/v1/sourcing";

// 1. 아이허브 상품 데이터 수집 (크롤링)
// urls 파라미터는 ['url1', 'url2'] 형태의 배열입니다.
export const sourceFromIherbApi = (urls) => {
  return axios.post(`${BASE_URL}/iherb`, { urls });
};

// 💡 (참고용) 나중에 만들 2. 마켓 일괄 등록 API 미리 구상
// export const registerToMarketsApi = (productDataList, targetMarkets) => {
//     return axios.post(`${BASE_URL}/publish`, { products: productDataList, markets: targetMarkets });
// };
