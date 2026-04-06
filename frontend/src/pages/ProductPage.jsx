import React, { useState, useEffect } from "react";
import {
  Card,
  Button,
  Space,
  message,
  Modal,
  Descriptions,
  Spin,
  InputNumber,
  Typography,
  Upload,
  Input,
  Pagination,
  Tooltip,
  ConfigProvider,
} from "antd";
import {
  SyncOutlined,
  PictureOutlined,
  PlusOutlined,
  DeleteOutlined,
  LinkOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { AgGridReact } from "ag-grid-react";
import {
  ModuleRegistry,
  AllCommunityModule,
  themeQuartz,
} from "ag-grid-community";

import {
  fetchProductsApi,
  fetchProductDetailApi,
  fetchMarketLocalApi,
  syncMarketLiveApi,
  updatePriceStockApi,
  uploadProductImagesApi,
  publishToMarketApi,
  crawlProductImagesApi,
  uploadProductImagesByUrlApi,
} from "../api/productApi";

// AG-Grid 모듈 등록
ModuleRegistry.registerModules([AllCommunityModule]);

const { Dragger } = Upload;

// 🚀 [신규] 시크한 블랙 앤 화이트 + AG-Grid 블랙 테마 설정
const modernBlackStyle = (
  <style>
    {`
      /* Ant Design 입력창 테두리 회색톤 */
      .ant-input:focus, .ant-input-focused, .ant-input-number:focus, .ant-input-number-focused {
          border-color: #d9d9d9 !important;
          box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04) !important;
      }
      
      /* AG Grid 전용 블랙 앤 화이트 테마 오버라이딩 */
      .ag-theme-quartz {
        font-size: 13px !important; /* 🚀 전체 글씨 크기 약간 축소 */
        --ag-active-color: #000000; /* 활성화/체크박스 색상을 까맣게 */
        --ag-checkbox-checked-color: #000000;
        --ag-selected-row-background-color: #f3f4f6; /* 선택된 행 배경 (연한 회색) */
        --ag-row-hover-color: #f9fafb; /* 마우스 올렸을 때 배경 */
        --ag-column-hover-color: #f9fafb;
      }
      /* 🚀 [신규] AG Grid 안의 모든 링크(a태그) 색상을 차분한 다크그레이로 변경 */
      .ag-theme-quartz a {
        color: #434343 !important;
        transition: color 0.2s;
      }
      .ag-theme-quartz a:hover {
        color: #000000 !important;
      }
    `}
  </style>
);

export default function ProductPage() {
  // =====================================================================
  // 1. 상태(State) 선언부
  // =====================================================================
  const [rowData, setRowData] = useState([]);
  const [loading, setLoading] = useState(false);

  // 서버 사이드 페이징 & 검색용 상태
  const [currentPage, setCurrentPage] = useState(0); // Spring은
  // 0페이지부터
  // 시작
  const [pageSize, setPageSize] = useState(50); // 한
  // 페이지당
  // 개수
  const [totalElements, setTotalElements] = useState(0); // DB 전체 데이터 개수
  // (Spring의
  // PageImpl에서
  // 줌)
  const [searchKeyword, setSearchKeyword] = useState(""); // 검색어

  // 마켓 상세 모달용 상태
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalData, setModalData] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [syncLoading, setSyncLoading] = useState(false);
  const [activeIds, setActiveIds] = useState({
    productId: null,
    marketType: null,
  });

  // 가격/재고 일괄 수정 모달용 상태
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState({
    id: null,
    name: "",
    price: 0,
    stock: 0,
  });
  const [editLoading, setEditLoading] = useState(false);

  // 이미지 관리 모달용 상태
  const [isImageModalOpen, setIsImageModalOpen] = useState(false);
  const [imageTarget, setImageTarget] = useState({ id: null, name: "" });
  const [fileList, setFileList] = useState([]); // 업로드할 파일들을
  // 담아두는 배열
  const [imageLoading, setImageLoading] = useState(false);

  // 🚀 [신규] 아이허브 이미지 크롤링 관련 상태
  const [crawlLoading, setCrawlLoading] = useState(false);
  const [crawledUrls, setCrawledUrls] = useState([]); // 크롤링된 원본 URL 보관

  // 🚀 [신규] 상세 HTML 모달용 상태 및 함수
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [detailTargetHtml, setDetailTargetHtml] = useState("");

  const openDetailModal = (htmlContent) => {
    setDetailTargetHtml(htmlContent || "상세 정보가 없습니다.");
    setIsDetailModalOpen(true);
  };

  // 🚀 [신규] 상품 전체 상세 정보(ProductDetailResponse) 모달용 상태
  const [isProductInfoModalOpen, setIsProductInfoModalOpen] = useState(false);
  const [productInfoData, setProductInfoData] = useState(null);
  const [productInfoLoading, setProductInfoLoading] = useState(false);

  // 🚀 [신규] 상품명 클릭 시 상세 API 호출 및 모달 오픈
  const openProductInfoModal = async (id) => {
    setIsProductInfoModalOpen(true);
    setProductInfoLoading(true);
    try {
      const response = await fetchProductDetailApi(id);
      if (response.success) {
        setProductInfoData(response.data); // ProductDetailResponse 데이터 저장
      }
    } catch (error) {
      message.error("상품 상세 정보를 불러오는데 실패했습니다.");
      setIsProductInfoModalOpen(false);
    } finally {
      setProductInfoLoading(false);
    }
  };

  // =====================================================================
  // 2. API 호출 함수들
  // =====================================================================

    // 🚀 [신규] 마켓 전송 처리 함수 (상태 선언부 아래쪽에 추가)
    const handlePublish = (productId, fieldName) => {
        // 컬럼 필드명을 백엔드 MarketType Enum 규격으로 변환
        const marketMap = {
            coupangCode: "COUPANG",
            cafe24Code: "CAFE24",
            smartstoreCode: "SMARTSTORE",
            elevenstCode: "ELEVENST",
        };
        const marketType = marketMap[fieldName];

        Modal.confirm({
                          title: `${marketType} 마켓에 상품을 등록하시겠습니까?`,
                          content: "등록이 완료되면 마켓의 상품 코드가 자동으로 연동됩니다.",
                          okText: "등록",
                          cancelText: "취소",
                          async onOk() {
                              try {
                                  message.loading({ content: `${marketType} 전송 중...`, key: "publish" });
                                  const response = await publishToMarketApi(productId, marketType);
                                  if (response.data.success) {
                                      message.success({ content: "마켓 등록 완료!", key: "publish" });
                                      // 💡 리스트를 새로고침해서 방금 받아온 마켓 코드가 화면에 뜨도록 함
                                      fetchProducts(currentPage, pageSize, searchKeyword);
                                  }
                              } catch (error) {
                                  message.error({ content: "마켓 등록에 실패했습니다.", key: "publish" });
                              }
                          },
                      });
    };

  // 그리드 목록 조회
  const fetchProducts = async (
    page = currentPage,
    size = pageSize,
    keyword = searchKeyword,
  ) => {
    setLoading(true);
    try {
      const response = await fetchProductsApi(page, size, keyword);
      if (response.data.success) {
        setRowData(response.data.data.content);
        // Spring Data JPA의 Page 객체가 주는 totalElements를 저장해야 페이지네이션 숫자가
        // 그려집니다.
        setTotalElements(response.data.data.totalElements || 0);
        setCurrentPage(page);
      }
    } catch (error) {
      console.error("상품 목록 조회 실패:", error);
      message.error("상품 데이터를 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트가 처음 켜질 때 한 번만 실행
  useEffect(() => {
    fetchProducts(0, pageSize, ""); // 처음 화면 뜰 때는 0페이지, 빈 검색어
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // 🚀 [신규] 검색어 입력 후 엔터 쳤을 때
  const handleSearch = (value) => {
    setSearchKeyword(value);
    fetchProducts(0, pageSize, value); // 검색하면 무조건 1페이지(0 index)부터 다시 보여줘야 함
  };

  // 🚀 [신규] 하단 페이지 번호나 보기 개수(size)를 변경했을 때
  const handleTableChange = (page, size) => {
    setPageSize(size);
    fetchProducts(page - 1, size, searchKeyword); // Antd(1-based) ->
    // Spring(0-based) 변환 로직
  };

  // 마켓 로컬 데이터 조회 (모달창 열기)
  const openMarketModal = async (productId, fieldName) => {
    const marketMap = {
      coupangCode: "COUPANG",
      cafe24Code: "CAFE24",
      smartstoreCode: "SMARTSTORE",
      elevenstCode: "ELEVENST",
    };
    const marketType = marketMap[fieldName];
    if (!marketType) {
      return;
    }

    setIsModalOpen(true);
    setModalLoading(true);
    setActiveIds({ productId, marketType });

    try {
      const response = await fetchMarketLocalApi(productId, marketType);
      if (response.data.success) {
        setModalData(response.data.data);
      }
    } catch (error) {
      console.error("마켓 데이터 조회 실패:", error);
      message.error("마켓 데이터를 불러올 수 없습니다.");
      setIsModalOpen(false);
    } finally {
      setModalLoading(false);
    }
  };

  // 마켓 실시간 데이터 강제 동기화
  const syncLiveMarketData = async () => {
    setSyncLoading(true);
    try {
      const response = await syncMarketLiveApi(
        activeIds.productId,
        activeIds.marketType,
      );
      if (response.data.success) {
        setModalData(response.data.data);
        message.success("최신 마켓 데이터로 동기화되었습니다!");
      }
    } catch (error) {
      console.error("마켓 강제 동기화 실패:", error);
      message.error("마켓 동기화 중 오류가 발생했습니다.");
    } finally {
      setSyncLoading(false);
    }
  };

  // 일괄 수정 모달 열기
  const openEditModal = (data) => {
    setEditTarget({
      id: data.id,
      name: data.name,
      price: data.price || 0,
      stock: data.stock || 0,
    });
    setIsEditModalOpen(true);
  };

  // 일괄 수정 API 전송 (브로드캐스트)
  const submitEditAndBroadcast = async () => {
    setEditLoading(true);
    try {
      const response = await updatePriceStockApi(editTarget.id, {
        price: editTarget.price,
        stock: editTarget.stock,
      });

      if (response.data.success) {
        message.success(
          "로컬 수정 및 모든 마켓 연동이 성공적으로 완료되었습니다! 🚀",
        );
        setIsEditModalOpen(false);
        fetchProducts(); // 변경된 데이터로 그리드 새로고침
      }
    } catch (error) {
      console.error("일괄 수정 실패:", error);
      message.error("마켓 동기화 중 오류가 발생했습니다.");
    } finally {
      setEditLoading(false);
    }
  };

  // 🚀 [신규] 이미지 모달 열기
  const openImageModal = (data) => {
    setImageTarget({ id: data.id, name: data.name, sourceUrl: data.sourceUrl });

    // setFileList([]); // 모달을 열 때마다 업로드 목록 초기화

    // 💡 핵심: 백엔드에서 받은 기존 이미지 URL들을 Ant Design Upload 규격에 맞게 변환하여 넣어줍니다.
    if (
      data.imageInfo?.hostedImages &&
      data.imageInfo.hostedImages.length > 0
    ) {
      const existingFiles = data.imageInfo.hostedImages.map((url, index) => ({
        uid: `-1-${index}`, // 고유 식별자
        name: `기존 이미지 ${index + 1}`, // 화면에 표시될 임시 이름
        status: "done", // 이미 업로드 완료된 상태로 표시
        url: url, // 화면에 보여줄 썸네일 URL
      }));
      setFileList(existingFiles);
    } else {
      setFileList([]); // 등록된 이미지가 없으면 빈 칸
    }

    setCrawledUrls([]); // 크롤링 URL 초기화
    setIsImageModalOpen(true);
  };

  // 🚀 [신규] 전체 이미지 비우기 및 확인 모달
  const handleClearAll = () => {
    Modal.confirm({
      title: "전체 이미지 비우기 확인",
      icon: <DeleteOutlined style={{ color: "red" }} />,
      content:
        "정말 모든 이미지를 비우시겠습니까? 이 작업은 취소할 수 없으며, 등록된 모든 이미지가 삭제됩니다.",
      okText: "비우기",
      okType: "danger",
      cancelText: "취소",
      onOk() {
        // 모든 파일 리스트를 초기화 -> 화면은 대형 업로드 영역으로 전환됨
        setFileList([]);
        message.success(
          "모든 이미지가 비워졌습니다. 새로운 이미지를 등록해 주세요.",
        );
      },
    });
  };

  // 🚀 [신규] 아이허브 이미지 크롤링 핸들러
  const handleCrawlImages = async () => {
    if (!imageTarget.sourceUrl) {
      message.warning("해당 상품에 소싱 URL(아이허브 링크)이 등록되어 있지 않습니다.");
      return;
    }

    setCrawlLoading(true);
    try {
      const result = await crawlProductImagesApi(imageTarget.id);
      if (result.success && result.data && result.data.length > 0) {
        const urls = result.data;
        setCrawledUrls(urls); // 원본 URL 보관 (제출 시 서버에 전달할 용도)

        // 크롤링된 이미지를 fileList에 미리보기로 세팅
        const crawledFiles = urls.map((url, index) => ({
          uid: `crawled-${index}`,
          name: `아이허브 이미지 ${index + 1}`,
          status: "done",
          url: url, // 미리보기 이미지 URL
        }));
        setFileList(crawledFiles);
        message.success(`아이허브에서 ${urls.length}장의 이미지를 불러왔습니다!`);
      } else {
        message.warning("아이허브에서 이미지를 찾을 수 없습니다.");
      }
    } catch (error) {
      console.error("아이허브 이미지 크롤링 실패:", error);
      message.error("아이허브 이미지 크롤링에 실패했습니다.");
    } finally {
      setCrawlLoading(false);
    }
  };

  // 🚀 [수정] 이미지 파일 전송 로직 (multipart vs URL 분기)
  const submitImageUpload = async () => {
    if (fileList.length === 0) {
      message.warning("업로드할 이미지를 최소 1장 이상 선택해주세요.");
      return;
    }

    setImageLoading(true);

    try {
      // 🚀 [분기] 크롤링된 URL이 있으면 URL 기반 업로드 수행
      if (crawledUrls.length > 0) {
        const response = await uploadProductImagesByUrlApi(imageTarget.id, crawledUrls);
        if (response.data.success) {
          message.success(
            "아이허브 이미지가 클라우드에 업로드되고 마켓에 동기화되었습니다! 🚀",
          );
          setIsImageModalOpen(false);
          setCrawledUrls([]); // 크롤링 URL 초기화
          fetchProducts();
        }
      } else {
        // 기존 로직: 로컬 파일 multipart 업로드
        const formData = new FormData();
        fileList.forEach((file) => {
          if (file.originFileObj) {
            formData.append("images", file.originFileObj);
          }
        });

        const response = await uploadProductImagesApi(imageTarget.id, formData);
        if (response.data.success) {
          message.success(
            "이미지가 성공적으로 클라우드에 업로드되고 마켓에 동기화되었습니다! 🚀",
          );
          setIsImageModalOpen(false);
          fetchProducts();
        }
      }
    } catch (error) {
      console.error("이미지 업로드 실패:", error);
      message.error("이미지 업로드 및 동기화에 실패했습니다.");
    } finally {
      setImageLoading(false);
    }
  };

  // =====================================================================
  // 3. AG-Grid 컬럼 및 렌더러 정의
  // =====================================================================
  // 🚀 [신규] 썸네일 이미지 렌더러 (자바의 NPE 방지를 위해 옵셔널 체이닝 '?.' 사용)
  const imageCellRenderer = (params) => {
    // imageInfo 안의 hostedImages 배열의 첫 번째 값(대표 이미지)을 꺼냅니다.
    const imageUrl = params.data.imageInfo?.hostedImages?.[0];

    // 이미지가 없을 때도 클릭해서 추가할 수 있게 만듭니다.
    if (!imageUrl) {
      return (
        <Tooltip title="클릭하여 이미지 추가">
          <div
            onClick={() => openImageModal(params.data)}
            style={{
              color: "#ccc",
              fontSize: "11px",
              textAlign: "center",
              cursor: "pointer",
              height: "100%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            No Image
          </div>
        </Tooltip>
      );
    }

    // 이미지가 있을 때는 썸네일을 클릭하게 만듭니다.
    return (
      <Tooltip title="클릭하여 이미지 관리">
        <div
          onClick={() => openImageModal(params.data)}
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            height: "100%",
            cursor: "pointer",
          }}
        >
          <img
            src={imageUrl}
            alt="썸네일"
            style={{
              width: 40,
              height: 40,
              objectFit: "cover",
              borderRadius: 4,
              border: "1px solid #e8e8e8",
            }}
          />
        </div>
      </Tooltip>
    );
  };

  const marketCellRenderer = (params) => {
    if (!params.value) {
        return (
            <div style={{ display: 'flex', alignItems: 'center', height: '100%', justifyContent: 'flex-start' }}>
          <Button
              size="small"
              type="primary"
              ghost
              style={{ fontSize: '11px', padding: '0 8px' }}
              onClick={() => handlePublish(params.data.id, params.colDef.field)}
          >
            등록
          </Button>
        </div>
        );
    }
    return (
      <a
        onClick={() => openMarketModal(params.data.id, params.colDef.field)}
        style={{
          textDecoration: "underline",
          cursor: "pointer",
        }}
      >
        {params.value}
      </a>
    );
  };

  // 🚀 [수정] 원본 링크 렌더러 ('원본' 글씨 제거 및 컬러 차분하게)
  const sourceLinkCellRenderer = (params) => {
    if (!params.value)
      return <div style={{ color: "#ccc", textAlign: "center" }}>-</div>;

    return (
      <a
        href={params.value}
        target="_blank"
        rel="noopener noreferrer"
        style={{
          display: "flex",
          alignItems: "center",
          height: "100%",
          justifyContent: "center",
          fontSize: "16px", // 💡 아이콘 크기 살짝 키움
        }}
      >
        <LinkOutlined />
      </a>
    );
  };

  const columnDefs = [
    {
      field: "image",
      headerName: "이미지",
      width: 80,
      pinned: "left",
      cellRenderer: imageCellRenderer,
      sortable: false,
      filter: false,
    },
    { field: "sku", headerName: "관리코드(SKU)", width: 140, pinned: "left" },
    {
      field: "name",
      headerName: "상품명",
      flex: 1,
      minWidth: 300,
      cellRenderer: (params) => (
        <a
          onClick={() => openProductInfoModal(params.data.id)}
          style={{
            cursor: "pointer",
            fontWeight: "bold",
            textDecoration: "underline",
          }}
        >
          {params.value}
        </a>
      ),
    },
    {
      field: "sourceUrl",
      headerName: "링크",
      width: 65, // 💡 아이콘만 있으므로 넓이 축소
      cellRenderer: sourceLinkCellRenderer,
      sortable: false,
      filter: false,
    },
    {
      field: "price",
      headerName: "판매가",
      width: 100,
      cellStyle: { textAlign: "right" },
      valueFormatter: (params) =>
        params.value ? `${params.value.toLocaleString()}원` : "-",
    },
    {
      field: "stock",
      headerName: "재고",
      width: 80,
      cellStyle: (params) => ({
        color: params.value > 0 ? "#389e0d" : "#cf1322", // 🚀 [수정] 쨍한 빨강 대신 살짝 톤다운된 레드/그린 적용
        fontWeight: params.value > 0 ? "normal" : "bold",
        textAlign: "center",
      }),
      valueFormatter: (params) =>
        params.value > 0 ? `${params.value}개` : "품절",
    },
    {
      field: "coupangCode",
      headerName: "쿠팡",
      width: 120,
      cellRenderer: marketCellRenderer,
    },
    {
      field: "cafe24Code",
      headerName: "카페24",
      width: 100,
      cellRenderer: marketCellRenderer,
    },
    {
      field: "smartstoreCode",
      headerName: "스마트스토어",
      width: 130,
      cellRenderer: marketCellRenderer,
    },
    {
      field: "elevenstCode",
      headerName: "11번가",
      width: 110,
      cellRenderer: marketCellRenderer,
    },
    // 🚀 [신규] 상세 설명(HTML) 모달 띄우기 컬럼
    {
      headerName: "상세",
      width: 70,
      sortable: false,
      filter: false,
      cellRenderer: (params) => (
        <Button
          size="small"
          onClick={() => openDetailModal(params.data.detailHtml)}
        >
          보기
        </Button>
      ),
    },
    {
      headerName: "관리",
      width: 90,
      pinned: "right",
      cellRenderer: (params) => (
        <Space>
          <Button size="small" onClick={() => openEditModal(params.data)}>
            가격/재고
          </Button>
        </Space>
      ),
    },
  ];

  // =====================================================================
  // 4. 화면 렌더링
  // =====================================================================
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#000000", // 🚀 모든 Primary 컬러를 시크한 블랙으로!
          controlItemBgActive: "#f3f4f6", // Select 박스 활성화 색상
          controlItemBgHover: "#f9fafb", // Select 박스 호버 색상
        },
      }}
    >
      {modernBlackStyle} {/* 🚀 스타일 주입 */}
      {/* 🚀 바깥 여백을 주고 디자인 정렬 */}
      <div style={{ padding: "24px", maxWidth: "1600px", margin: "0 auto" }}>
        {/* 🚀 1. 상품 목록 그리드 카드 */}
        <Card
          title="상품 관리"
          extra={
            <Space>
              <Input.Search
                placeholder="상품명 또는 SKU 검색"
                allowClear
                enterButton="검색"
                size="middle"
                onSearch={handleSearch}
                style={{ width: 300 }}
              />
              <Button
                icon={<SyncOutlined />}
                onClick={() =>
                  fetchProducts(currentPage, pageSize, searchKeyword)
                }
                loading={loading}
              >
                새로고침
              </Button>
            </Space>
          }
          styles={{ body: { padding: 0 } }}
        >
          <div style={{ height: "calc(100vh - 270px)", width: "100%" }}>
            <AgGridReact
              theme={themeQuartz}
              rowData={rowData}
              columnDefs={columnDefs}
              // 🚀 [수정] 이미지가 잘리지 않도록 행 높이를 50px로 키워줍니다.
              rowHeight={50}
              defaultColDef={{ sortable: true, filter: true, resizable: true }}
              rowSelection={{ mode: "multiRow" }}
              animateRows={true}
            />
          </div>
          {/* 🚀 [신규] 하단 Ant Design 페이징 컨트롤바 */}
          <div
            style={{
              padding: "15px 20px",
              textAlign: "right",
              borderTop: "1px solid #f0f0f0",
            }}
          >
            <Pagination
              current={currentPage + 1} // AntD는 1부터 시작하므로 +1
              pageSize={pageSize}
              total={totalElements}
              showSizeChanger
              pageSizeOptions={["50", "100", "200"]}
              onChange={handleTableChange}
              showTotal={(total) => `총 ${total}개의 상품`}
            />
          </div>
        </Card>

        {/* 🚀 2. 마켓 상세 정보 모달 */}
        <Modal
          title={`${activeIds.marketType || ""} 마켓 연동 정보`}
          open={isModalOpen}
          onCancel={() => setIsModalOpen(false)}
          width={700}
          footer={[
            <Button key="close" onClick={() => setIsModalOpen(false)}>
              닫기
            </Button>,
            <Button
              key="sync"
              type="primary"
              icon={<SyncOutlined />}
              loading={syncLoading}
              onClick={syncLiveMarketData}
            >
              최신 상태 불러오기 🔄
            </Button>,
          ]}
        >
          {modalLoading ? (
            <div
              style={{
                textAlign: "center",
                padding: "50px 0",
              }}
            >
              <Spin size="large" />
            </div>
          ) : modalData ? (
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="마켓 등록명">
                {modalData.marketName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="매핑 키 (SKU)">
                {modalData.mappingKey || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="마켓 판매가">
                {modalData.salePrice
                  ? `${modalData.salePrice.toLocaleString()}원`
                  : "-"}
              </Descriptions.Item>
              <Descriptions.Item label="마켓 재고">
                {modalData.stock > 0 ? (
                  <span style={{ color: "green" }}>{modalData.stock}개</span>
                ) : (
                  <span style={{ color: "red", fontWeight: "bold" }}>품절</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="브랜드">
                {modalData.brand || "-"}
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <p>표시할 데이터가 없습니다.</p>
          )}
        </Modal>

        {/* 🚀 3. 상품 가격/재고 일괄 수정 모달 */}
        <Modal
          title="💰 상품 가격/재고 일괄 수정"
          open={isEditModalOpen}
          onOk={submitEditAndBroadcast}
          onCancel={() => setIsEditModalOpen(false)}
          confirmLoading={editLoading}
          okText="수정 및 마켓 전체 브로드캐스트"
          cancelText="취소"
        >
          <div
            style={{
              padding: "10px 0",
              marginBottom: 20,
              borderBottom: "1px solid #f0f0f0",
            }}
          >
            <Typography.Text strong>대상 상품: </Typography.Text>
            <Typography.Text>{editTarget.name}</Typography.Text>
          </div>

          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            <div>
              <div style={{ marginBottom: 8 }}>
                <Typography.Text strong>판매가 (원)</Typography.Text>
              </div>
              <InputNumber
                style={{ width: "100%" }}
                value={editTarget.price}
                formatter={(value) =>
                  `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                }
                parser={(value) => value?.replace(/\$\s?|(,*)/g, "")}
                onChange={(val) => setEditTarget({ ...editTarget, price: val })}
              />
            </div>
            <div>
              <div style={{ marginBottom: 8 }}>
                <Typography.Text strong>재고 수량 (개)</Typography.Text>
              </div>
              <InputNumber
                style={{ width: "100%" }}
                value={editTarget.stock}
                min={0}
                onChange={(val) => setEditTarget({ ...editTarget, stock: val })}
              />
            </div>
          </Space>

          <div
            style={{
              marginTop: 24,
              padding: 12,
              background: "#fff2f0",
              border: "1px solid #ffccc7",
              borderRadius: 4,
            }}
          >
            <Typography.Text type="danger" style={{ fontSize: "13px" }}>
              * 경고: 하단의 <b>'수정 및 마켓 전체 브로드캐스트'</b> 버튼을
              누르면, 로컬 DB가 변경됨과 동시에 연동된{" "}
              <b>모든 마켓(쿠팡, 카페24, 스마트스토어, 11번가)</b>의 상품 정보가
              즉시 업데이트됩니다.
            </Typography.Text>
          </div>
        </Modal>

        {/* ===================================================================== */}
        {/* 🚀 [업그레이드] 이미지 업로드 모달 */}
        {/* ===================================================================== */}
        <Modal
          // 🚀 [변경] 모달 헤더 우측에 '전체 비우기' 버튼 추가
          title={
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                width: "95%",
              }}
            >
              <span>
                <PictureOutlined /> 상품 이미지 관리 및 HTML 동기화
              </span>
              {/* 기존 이미지가 있을 때만 비우기 버튼을 보여줍니다. */}
              {fileList.some((file) => file.status === "done") && (
                <Button
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={handleClearAll}
                  size="small"
                  style={{ fontWeight: "normal", fontSize: "13px" }}
                >
                  전체 이미지 비우기
                </Button>
              )}
            </div>
          }
          open={isImageModalOpen}
          onCancel={() => setIsImageModalOpen(false)}
          width={750}
          footer={[
            <Button key="cancel" onClick={() => setIsImageModalOpen(false)}>
              취소
            </Button>,
            <Button
              key="upload"
              type="primary"
              loading={imageLoading}
              onClick={submitImageUpload}
            >
              클라우드 업로드 및 마켓 전체 전파 🚀
            </Button>,
          ]}
        >
          <div
            style={{
              padding: "10px 0",
              marginBottom: 20,
              borderBottom: "1px solid #f0f0f0",
            }}
          >
            <Typography.Text strong>대상 상품: </Typography.Text>
            <Typography.Text>{imageTarget.name}</Typography.Text>
          </div>
          <div style={{ marginBottom: 16 }}>
            <Typography.Text type="secondary">
              * <b>기존 이미지</b>를 지우고 새로운 사진을 추가할 수 있습니다.
              <br />* <b>첫 번째 사진</b>이 마켓의 <b>🌟대표 썸네일</b>로 자동
              지정되며, 나머지는 상세 설명(HTML)에 치환됩니다.
            </Typography.Text>
          </div>

          {/* 🚀 [신규] 아이허브 이미지 불러오기 버튼 */}
          <div style={{ marginBottom: 16 }}>
            <Button
              icon={<SearchOutlined />}
              loading={crawlLoading}
              onClick={handleCrawlImages}
              disabled={!imageTarget.sourceUrl}
              style={{ width: "100%" }}
            >
              {crawlLoading ? "아이허브에서 이미지 크롤링 중..." : "🔍 아이허브 이미지 불러오기"}
            </Button>
            {!imageTarget.sourceUrl && (
              <Typography.Text type="warning" style={{ fontSize: "12px", display: "block", marginTop: 4 }}>
                ⚠️ 해당 상품에 소싱 URL(아이허브 링크)이 등록되어 있지 않습니다.
              </Typography.Text>
            )}
          </div>

          {/* ===================================================================== */}
          {/* 🚀 [변경] 조건부 렌더링: 이미지가 아예 비워진 경우 대형 업로드 영역 표시 */}
          {/* ===================================================================== */}
          {fileList.length === 0 ? (
            <Dragger
              fileList={fileList}
              onChange={({ fileList: newFileList }) => setFileList(newFileList)}
              beforeUpload={() => false}
              multiple={true}
              maxCount={10}
              style={{
                width: "100%",
                height: "350px",
              }}
            >
              <p className="ant-upload-drag-icon">
                <PlusOutlined style={{ fontSize: "48px", color: "#1890ff" }} />
              </p>
              <p className="ant-upload-text">사진 추가</p>
              <p className="ant-upload-hint">
                여기를 클릭하거나 파일을 드래그 앤 드롭하여 <br />
                상품 이미지를 한 번에 추가할 수 있습니다. (최대 10장)
              </p>
            </Dragger>
          ) : (
            <Upload
              listType="picture-card"
              fileList={fileList}
              onChange={({ fileList: newFileList }) => setFileList(newFileList)}
              beforeUpload={() => false}
              multiple={true}
              maxCount={10}
            >
              {fileList.length >= 10 ? null : (
                <div>
                  <PlusOutlined />
                  <div style={{ marginTop: 8 }}>사진 추가</div>
                </div>
              )}
            </Upload>
          )}
        </Modal>

        {/* ===================================================================== */}
        {/* 🚀 [신규] 상품 상세 설명(HTML) 대왕 모달 */}
        {/* ===================================================================== */}
        <Modal
          title="📄 상품 상세 설명 (HTML)"
          open={isDetailModalOpen}
          onCancel={() => setIsDetailModalOpen(false)}
          width={900} // 💡 대왕 모달 사이즈
          footer={[
            <Button key="close" onClick={() => setIsDetailModalOpen(false)}>
              닫기
            </Button>,
          ]}
        >
          <div
            style={{
              padding: "24px",
              borderRadius: "8px",
              maxHeight: "70vh", // 화면의 70%까지만 커지고 그 이상은 스크롤
              overflowY: "auto",
              background: "#fafafa",
              border: "1px solid #f0f0f0",
            }}
            dangerouslySetInnerHTML={{ __html: detailTargetHtml }}
          />
        </Modal>

        {/* ===================================================================== */}
        {/* 🚀 [신규] 상품 상세 정보 (API 연동 대왕 모달) */}
        {/* ===================================================================== */}
        <Modal
          title="📦 상품 상세 정보"
          open={isProductInfoModalOpen}
          onCancel={() => setIsProductInfoModalOpen(false)}
          width={1000} // 💡 대왕 모달 사이즈
          centered // 🚀 모달을 화면 한가운데에 완벽하게 수직 정렬!
          footer={[
            <Button
              key="close"
              onClick={() => setIsProductInfoModalOpen(false)}
            >
              닫기
            </Button>,
          ]}
        >
          {productInfoLoading ? (
            <div style={{ textAlign: "center", padding: "50px 0" }}>
              <Spin size="large" tip="데이터를 불러오는 중입니다..." />
            </div>
          ) : productInfoData ? (
            <div
              style={{
                maxHeight: "75vh", // 전체 모달 내용이 길어져도 화면 밖으로 안 나가게 스크롤
                overflowY: "auto",
                paddingRight: "10px",
              }}
            >
              {/* 1. 기본 정보 */}
              <Descriptions
                title="기본 정보"
                bordered
                column={2}
                size="small"
                style={{ marginBottom: 24 }}
              >
                <Descriptions.Item label="DB 고유 ID">
                  {productInfoData.id}
                </Descriptions.Item>
                <Descriptions.Item label="SKU">
                  {productInfoData.sku}
                </Descriptions.Item>
                <Descriptions.Item label="브랜드">
                  {productInfoData.brand}
                </Descriptions.Item>
                <Descriptions.Item label="카테고리">
                  {productInfoData.category}
                </Descriptions.Item>
                <Descriptions.Item label="최종 상품명" span={2}>
                  <Typography.Text strong>
                    {productInfoData.name}
                  </Typography.Text>
                </Descriptions.Item>
                <Descriptions.Item label="핵심 상품명(KR)" span={2}>
                  {productInfoData.baseName}
                </Descriptions.Item>
                <Descriptions.Item label="원어 상품명(EN)" span={2}>
                  {productInfoData.originalName}
                </Descriptions.Item>
                <Descriptions.Item label="검색 키워드" span={2}>
                  {productInfoData.searchKeywords}
                </Descriptions.Item>
              </Descriptions>

              {/* 2. 소싱 및 물류 정보 */}
              <Descriptions
                title="소싱 및 물류 정보"
                bordered
                column={2}
                size="small"
                style={{ marginBottom: 24 }}
              >
                <Descriptions.Item label="소싱처">
                  {productInfoData.sourcingInfo?.vendor || "-"}
                </Descriptions.Item>
                <Descriptions.Item label="원산지">
                  {productInfoData.sourcingInfo?.origin || "-"}
                </Descriptions.Item>
                <Descriptions.Item label="HS Code">
                  {productInfoData.sourcingInfo?.hsCode || "-"}
                </Descriptions.Item>
                <Descriptions.Item label="무게">
                  {productInfoData.logisticsInfo?.weight} kg
                </Descriptions.Item>
                <Descriptions.Item label="용량 및 단위">
                  {productInfoData.productSpec?.capacity}{" "}
                  {productInfoData.productSpec?.measureUnit}
                </Descriptions.Item>
                <Descriptions.Item label="묶음 수량">
                  {productInfoData.logisticsInfo?.bundleQuantity} 개
                </Descriptions.Item>
              </Descriptions>

              {/* 3. 가격 정보 */}
              <Descriptions
                title="가격 정보"
                bordered
                column={2}
                size="small"
                style={{ marginBottom: 24 }}
              >
                <Descriptions.Item label="원가 (소싱가)">
                  {productInfoData.priceInfo?.costPrice?.toLocaleString()}원
                </Descriptions.Item>
                <Descriptions.Item label="마진율">
                  {productInfoData.priceInfo?.marginRate}%
                </Descriptions.Item>
                <Descriptions.Item label="최종 판매가" span={2}>
                  <Typography.Text type="danger" strong>
                    {productInfoData.priceInfo?.salePrice?.toLocaleString()}원
                  </Typography.Text>
                </Descriptions.Item>
              </Descriptions>

              {/* 🚀 4. 상세 HTML 정보 (아이프레임 룩앤필) */}
              <div>
                <Typography.Text
                  strong
                  style={{
                    fontSize: "15px",
                    display: "block",
                    marginBottom: "12px",
                  }}
                >
                  상세 설명 (HTML)
                </Typography.Text>
                <div
                  style={{
                    height: "500px", // 💡 요청하신 세로 500픽셀 고정 크기
                    overflowY: "auto", // 내용이 넘치면 내부에서만 스크롤!
                    background: "#ffffff",
                    border: "1px solid #d9d9d9",
                    borderRadius: "6px",
                    padding: "20px",
                    boxShadow: "inset 0 1px 4px rgba(0,0,0,0.05)", // 아이프레임처럼 살짝 파인 느낌의 그림자 효과
                  }}
                  // 💡 HTML 문자열을 브라우저에 직접 렌더링
                  dangerouslySetInnerHTML={{
                    __html:
                      productInfoData.detailHtml ||
                      "<div style='color:#999; text-align:center; margin-top:50px;'>등록된 상세 설명이 없습니다.</div>",
                  }}
                />
              </div>
            </div>
          ) : (
            <div
              style={{ textAlign: "center", padding: "50px 0", color: "#999" }}
            >
              데이터를 불러올 수 없습니다.
            </div>
          )}
        </Modal>
      </div>
    </ConfigProvider>
  );
}
