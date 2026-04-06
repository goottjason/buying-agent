/*💡 리액트 초보자를 위한 핵심 개념 3가지 (주석 보충 설명)
위 코드를 보시면서 딱 3가지만 이해하시면 리액트의 절반을 아신 겁니다.

1. useState (상태 관리):

 - 일반 자바스크립트에서는 변수(let url = '')의 값을 바꿔도 화면이 자동으로 안 바뀝니다.

 - 하지만 리액트에서 const [url, setUrl] = useState('') 라고 선언한 뒤, setUrl('새로운값')을 호출하면 리액트가 "어? 값이 바뀌었네? 화면 다시 새로고침해서 예쁘게 그려줄게!" 하고 알아서 UI를 업데이트해 줍니다. 이게 리액트의 가장 큰 마법입니다.

2. onChange (이벤트):

 - 사용자가 키보드를 칠 때마다 그 값을 가로채서 방금 배운 setUrl(입력한값)으로 넘겨줍니다. 그러면 타이핑할 때마다 화면이
  기억(State)을 갱신합니다.
3. 조건부 렌더링 ({ 조건 && (그릴 내용) }):

- 코드 중간에 {localProducts.length > 0 && (<Card>...</Card>)} 라는 부분이 있습니다.

- "크롤링한 상품이 1개라도 있을 때만 Step 2 카드를 화면에 보여줘라"는 뜻입니다. 처음에는 숨어있다가, 파란 버튼을 눌러 데이터가
 생기면 뿅! 하고 튀어나옵니다.

프론트엔드 프로젝트를 실행(npm start 또는 yarn start)하셔서 신규 등록 페이지에 들어가 보세요!
  아무 URL이나 여러 개 적고 첫 번째 파란 버튼을 누르면, 1.5초 뒤에 가짜 데이터 표가 나타나고 마켓 전송 버튼이 생길 겁니다.*/

/*💡 리액트 초보자를 위한 꿀팁 해설 (어떻게 자동 계산이 될까?)
화면을 실행해 보시고 **"묶음수"**를 2로 바꿔보거나 **"마진율"**을 50으로 바꿔보세요. 옆에 있는 **"최종 마켓 판매가"**가 실시간으로 샤르륵 계산되어 바뀌는 것을 볼 수 있습니다.

  리액트가 이런 마법을 부리는 원리는 다음과 같습니다.

  상태 불변성 (State Immutability): * handleProductChange 함수에서 [...crawledProducts] 처럼 점 3개(...)를 찍어 배열을 통째로 복사합니다.

  리액트는 원본이 직접 바뀌면 "화면을 다시 그려야 하나?" 하고 눈치를 채지 못합니다. 아예 새로운 복사본 박스를 만들어서 건네줘야 비로소 "어! 박스가 바뀌었네! 화면 다시 그릴게!" 하고 반응합니다.

  실시간 렌더링 공식:

  코드 중간에 있는 const finalPrice = ... 부분은 State(useState)가 아닙니다.

  crawledProducts State가 변경되어 화면이 다시 그려질 때마다 위에서부터 아래로 코드가 다시 쭉 실행되면서 매번 새롭게 수학 공식을 계산해서 화면에 뿌려주는 것입니다. (이것을 파생 상태, Derived State 라고 부릅니다)*/

// 1. 필요한 도구들을 가져옵니다 (import)
import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
// antd: 디자인이 예쁘게 완성된 UI 컴포넌트(버튼, 입력창 등)들을 가져옵니다.
import {
  Input,
  Button,
  Card,
  Checkbox,
  message,
  Typography,
  InputNumber,
  Table,
  Modal,
  Space,
  Tag,
  Divider,
  Select,
  ConfigProvider,
} from "antd";
import { sourceFromIherbApi } from "../api/sourcingApi";
import { saveProductsBulk } from "../api/productApi";

const modernBlackStyle = (
  <style>
    {`
      /* 1. 테이블 체크박스 색상 변경 (새파란색 -> 검정색 계열) */
      .ant-checkbox-checked .ant-checkbox-inner {
        background-color: #262626 !important;
        border-color: #262626 !important;
      }
      .ant-checkbox-wrapper:hover .ant-checkbox-inner,
      .ant-checkbox:hover .ant-checkbox-inner,
      .ant-checkbox-input:focus + .ant-checkbox-inner {
        border-color: #262626 !important;
      }
      
      /* 2. 테이블 데이터 행(Row) 배경색 제거 및 모던한 호버(Hover) 색상 적용 */
      /* 종원 님이 극혐하시던 데이터 영역 하늘색 배경을 하얀색으로 날려버립니다. */
      .ant-table-wrapper .ant-table-tbody > tr > td {
        background: #fff !important;
      }
      /* 마우스 갖다 댔을 때 나타나는 짙은 파란색을 아주 연한 그레이(bg-gray-50)로 바꿉니다. */
      .ant-table-wrapper .ant-table-tbody > tr:hover > td {
        background: #f9fafb !important; /* bg-gray-50 */
      }

      /* 3. 입력창 포커스(클릭) 시 나타나는 파란색 테두리를 차분한 회색 테두리로 변경 */
      .ant-input:focus, .ant-input-focused,
      .ant-input-number:focus, .ant-input-number-focused,
      .ant-select-single:not(.ant-select-customize-input) .ant-select-selector:focus,
      .ant-select-focused:not(.ant-select-disabled).ant-select-single:not(.ant-select-customize-input) .ant-select-selector {
          border-color: #d9d9d9 !important;
          box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04) !important;
      }
      
      html { overflow-y: scroll; }
      body { padding-right: 0 !important; overflow-y: scroll !important; }
    `}
  </style>
);

// 2. 화면을 구성하는 핵심 함수(컴포넌트)를 선언합니다.
const { TextArea } = Input;
const { Title, Text } = Typography;

const ProductRegisterPage = () => {
  // =====================================================================
  // [State 영역] 화면에서 기억해야 할 데이터들을 정의
  // const [현재값, 값을 변경하는함수] = useState(초기값); 형태를 가집니다.
  // =====================================================================

  // 💡 2. 페이지 이동을 위한 네비게이터 선언
  const navigate = useNavigate();

  // 사용자가 입력할 아이허브 URL들을 텍스트로 기억하는 공간입니다.
  const [urls, setUrls] = useState("");
  // 크롤링(로컬 생성) 중인지, 마켓 전파 중인지 등 '로딩(로고 뱅글뱅글)' 상태를 기억합니다.
  const [isLoading, setIsLoading] = useState(false);
  // Step 1에서 수집해 온 '수정 가능한' 상품 데이터들을 배열로 기억합니다.
  const [products, setProducts] = useState([]);

  // 일괄 마진율 제어용 State
  const [globalMarginRate, setGlobalMarginRate] = useState(30);

  // 💡 [신규] 테이블 체크박스 선택 상태 관리
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  // 모달(팝업창)을 띄우고 닫기 위한 상태입니다.
  const [isModalVisible, setIsModalVisible] = useState(false);
  // 모달에 어떤 상품의 상세 정보를 띄울지 기억하는 공간입니다.
  const [currentDetailProduct, setCurrentDetailProduct] = useState(null);

  // =====================================================================
  // [함수 영역] 버튼을 눌렀을 때 실행될 행동들을 정의합니다.
  // =====================================================================

  // 1. 데이터 수집
  const handleCrawl = async () => {
    if (!urls.trim())
      return message.warning("아이허브 상품 URL을 입력해주세요!");
    setIsLoading(true);

    try {
      const urlList = urls.split("\n").filter((url) => url.trim() !== "");

      // 백엔드 API 호출! (경로가 맞는지 확인해 주세요)
      const response = await sourceFromIherbApi(urlList);

      const result = response.data;

      if (result.success) {
        // 백엔드에서 id가 안 넘어오므로 프론트에서 임시 UUID를 부여합니다.
        const dataWithIds = result.data.map((item) => ({
          ...item,
          id: crypto.randomUUID(),
        }));

        setProducts(dataWithIds);
        // 새로 수집할 때마다 체크박스 초기화 및 전체 선택
        setSelectedRowKeys(dataWithIds.map((item) => item.id));

        message.success(`${dataWithIds.length}개 데이터 수집 성공!`);
      } else {
        message.error("수집 실패: " + result.message);
      }
    } catch (error) {
      console.error(error);
      message.error("서버 통신 중 에러가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  // 🚀 2. 일괄 마진율 적용 로직
  const applyGlobalMargin = () => {
    if (products.length === 0) return;

    // 모든 상품의 marginRate를 입력한 globalMarginRate로 일괄 변경합니다.
    const newData = products.map((product) => ({
      ...product,
      marginRate: globalMarginRate,
    }));
    setProducts(newData);
    message.success(
      `전체 상품의 마진율이 ${globalMarginRate}%로 일괄 적용되었습니다.`,
    );
  };

  // 2. 테이블 내의 Input 값이 바뀔 때마다 배열(State)을 업데이트하는 함수
  // id: 어떤 상품인지 식별 / field: 어떤 항목(예: marginRate)을 바꿀지 / value: 바뀐 값
  const handleProductChange = (id, field, value) => {
    const newData = products.map((product) =>
      product.id === id ? { ...product, [field]: value } : product,
    );
    setProducts(newData);
  };

  const saveToLocalDB = async () => {
    if (selectedRowKeys.length === 0) {
      return message.warning("저장할 상품을 하나 이상 체크해주세요.");
    }

    // 선택된 데이터만 필터링
    const selectedProducts = products.filter((p) =>
      selectedRowKeys.includes(p.id),
    );

    message.loading({ content: "상품 관리에 저장 중...", key: "save" });

    try {
      // 🚀 3. 드디어 진짜 백엔드 API 호출! (프론트에서 수정한 배열을 통째로 던짐)
      await saveProductsBulk(selectedProducts);

      // 4. 통신 성공 시 축하 메시지!
      message.success({
        content: `${selectedProducts.length}개 상품이 성공적으로 저장되었습니다!`,
        key: "save",
        duration: 1.5,
      });

      // 5. 상품 관리 리스트 페이지로 이동 (방금 저장한 게 맨 위에 뜰 겁니다!)
      navigate("/products");
    } catch (error) {
      console.error("저장 실패 상세 원인:", error); // 💡 변수를 사용했으므로 에러 해결!
      // 🚨 통신 실패 시 에러 메시지
      message.error({
        content: "서버 저장 중 오류가 발생했습니다. 다시 시도해주세요.",
        key: "save",
        duration: 2,
      });
    }

    setTimeout(() => {
      message.success({
        content: `${selectedProducts.length}개 상품이 상품 관리에 성공적으로 저장되었습니다!`,
        key: "save",
        duration: 3,
      });

      // 4. 화면 초기화 대신 바로 '상품 관리' 페이지로 이동! (경로는 실제 라우팅 주소에 맞게 수정해주세요)
      navigate("/products");
      // 저장 완료 후, 저장된 상품은 목록에서 제거하거나 화면 초기화
      // setProducts([]);
      // setUrls("");
      // setSelectedRowKeys([]);
    }, 1000);
  };

  // 3. 모달 열기 함수 (상세 정보 보기 버튼 클릭 시)
  const showModal = (product) => {
    setCurrentDetailProduct(product); // 클릭한 상품의 데이터를 모달용 State에 넣고
    setIsModalVisible(true); // 모달을 화면에 띄웁니다!
  };

  // 4. 모달 닫기 함수
  const handleModalClose = () => {
    setIsModalVisible(false);
    setCurrentDetailProduct(null);
  };

  // 💡 단위 영문을 한글로 변환해주는 헬퍼 함수
  const getUnitLabel = (unitCode) => {
    const unitMap = { TABLET: "정(타블렛)", CAPSULE: "캡슐", EA: "개" };
    return unitMap[unitCode] || "";
  };

  // 💡 실시간 쿠팡 상품명 조립 함수
  const assemblePreviewName = (record) => {
    const brand = record.brand || "";
    const baseName = record.baseName || "";
    const capacity = record.capacity || "";
    const unitDesc = getUnitLabel(record.measureUnit);
    const bundleQty = record.bundleQuantity || 1; // 변수명 일치 (bundleQuantity)

    // 🚀 1개일 때는 텍스트 생략, 2개 이상일 때만 ', 2개' 붙이기
    const bundleText = bundleQty > 1 ? `, ${bundleQty}개` : "";

    return `${brand} ${baseName}, ${capacity}${unitDesc}${bundleText}`
      .replace(/ ,/g, ",")
      .replace(/  +/g, " ")
      .replace(/,$/, "") // 맨 끝에 쉼표가 남으면 제거
      .trim();
  };

  // 🚀 [신규] 문자열 바이트 수 계산 함수 (한글 2바이트, 영문/숫자/공백 1바이트)
  const getByteLength = (str) => {
    let byteLength = 0;
    for (let i = 0; i < str.length; i++) {
      // 한글 등 유니코드는 2바이트, 나머지는 1바이트 처리
      byteLength += str.charCodeAt(i) > 127 ? 2 : 1;
    }
    return byteLength;
  };

  const columns = [
    {
      title: "이미지",
      dataIndex: "sourceImages",
      width: 80,
      align: "center",
      render: (images) => {
        // 💡 배열의 첫 번째 이미지를 대표 이미지로 사용
        const src = images && images.length > 0 ? images[0] : "";
        return (
          <img
            src={src}
            alt="썸네일"
            style={{ width: "60px", borderRadius: "4px" }}
          />
        );
      },
    },
    {
      title: "상품 기본 정보 (수정 가능)",
      key: "info",
      width: 450,
      render: (_, record) => (
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {/* 🚀 수정된 브랜드 영역 (직접 수정 가능하도록 Input으로 변경) */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              marginBottom: "12px",
              gap: "8px",
            }}
          >
            <Input
              value={record.brand}
              onChange={(e) =>
                handleProductChange(record.id, "brand", e.target.value)
              }
              style={{ width: "180px", fontWeight: "bold", color: "#555" }}
              placeholder="브랜드명"
            />
            {record.expirationDate && (
              <Tag color="orange" variant="filled" style={{ margin: 0 }}>
                유통기한: {record.expirationDate}
              </Tag>
            )}
          </div>

          {/* 💡 2, 8. 고정 px(50px) 라벨 적용 및 addonBefore 제거 */}
          <div style={{ display: "flex", alignItems: "center" }}>
            <div style={{ width: "50px", fontWeight: "bold", color: "#555" }}>
              KR
            </div>
            <Input
              value={record.baseName}
              onChange={(e) =>
                handleProductChange(record.id, "baseName", e.target.value)
              }
              style={{ flex: 1 }}
            />
          </div>
          <div style={{ display: "flex", alignItems: "center" }}>
            <div style={{ width: "50px", fontWeight: "bold", color: "#555" }}>
              EN
            </div>
            <Input
              value={record.originalName}
              onChange={(e) =>
                handleProductChange(record.id, "originalName", e.target.value)
              }
              style={{ flex: 1 }}
            />
          </div>
          <div style={{ display: "flex", alignItems: "center" }}>
            <div style={{ width: "50px", fontWeight: "bold", color: "#555" }}>
              용량
            </div>
            <Space.Compact style={{ flex: 1 }}>
              <InputNumber
                value={record.capacity}
                onChange={(val) =>
                  handleProductChange(record.id, "capacity", val)
                }
                style={{ width: "50%" }}
              />
              <Select
                value={record.measureUnit}
                onChange={(val) =>
                  handleProductChange(record.id, "measureUnit", val)
                }
                style={{ width: "50%" }}
                options={[
                  { value: "TABLET", label: "정(타블렛)" },
                  { value: "CAPSULE", label: "캡슐" },
                  { value: "EA", label: "개" },
                ]}
              />
            </Space.Compact>
          </div>
          {/* 🚀 [수정] 최종 상품명 실시간 미리보기 + 바이트 카운팅 */}
          {/* 🚀 [수정] 최종 상품명 실시간 미리보기 + 디자인 개선 */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              marginTop: "8px",
              padding: "12px",
              background: "#f8fafc",
              borderRadius: "6px",
              border: "1px solid #e2e8f0",
            }}
          >
            {/* 💡 타이틀을 '최종'으로 변경하고, 위아래 중앙 정렬 적용 */}
            <div
              style={{
                width: "40px",
                fontWeight: "bold",
                color: "#000",
                fontSize: "12px",
              }}
            >
              최종
            </div>

            {/* 💡 컨텐츠 영역을 flex-row로 배치하여 상품명은 왼쪽, 글자수는 오른쪽에 배치 */}
            <div
              style={{
                flex: 1,
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                gap: "12px",
              }}
            >
              {/* 💡 진한 네이비 블루(#002766) 적용 및 가독성 증가 */}
              <div
                style={{
                  color: "#002766",
                  fontWeight: "900",
                  lineHeight: "1.4",
                  fontSize: "13px",
                }}
              >
                {assemblePreviewName(record)}
              </div>

              {/* 바이트 수 실시간 렌더링 */}
              <div
                style={{
                  fontSize: "11px",
                  textAlign: "right",
                  color:
                    getByteLength(assemblePreviewName(record)) > 100
                      ? "#ff4d4f"
                      : "#8c8c8c",
                  whiteSpace: "nowrap",
                }}
              >
                {getByteLength(assemblePreviewName(record))} / 100 Bytes
              </div>
            </div>
          </div>
        </div>
      ),
    },
    {
      title: "가격 계산",
      key: "calculator",
      width: 280,
      render: (_, record) => {
        const finalPrice = Math.floor(
          record.costPrice *
            record.bundleQuantity *
            (1 + record.marginRate / 100),
        );

        return (
          <div
            style={{
              background: "#fafafa",
              padding: "16px",
              borderRadius: "8px",
              border: "1px solid #f0f0f0",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: "12px",
              }}
            >
              <Text type="secondary">원가:</Text>
              <Text strong>{record.costPrice.toLocaleString()}원</Text>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                gap: "16px",
                marginBottom: "16px",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "8px" }}
              >
                <Text type="secondary">묶음</Text>
                <InputNumber
                  min={1}
                  value={record.bundleQuantity}
                  onChange={(val) =>
                    handleProductChange(record.id, "bundleQuantity", val)
                  }
                  style={{ width: "60px" }}
                />
              </div>
              <div
                style={{ display: "flex", alignItems: "center", gap: "8px" }}
              >
                <Text type="secondary">마진(%)</Text>
                <InputNumber
                  min={0}
                  value={record.marginRate}
                  onChange={(val) =>
                    handleProductChange(record.id, "marginRate", val)
                  }
                  style={{ width: "70px" }}
                />
              </div>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                borderTop: "1px dashed #d9d9d9",
                paddingTop: "8px",
              }}
            >
              <Text strong color="#1890ff">
                최종가:
              </Text>
              <Text strong style={{ color: "#ff4d4f", fontSize: "16px" }}>
                {finalPrice.toLocaleString()}원
              </Text>
            </div>
          </div>
        );
      },
    },
    {
      title: "상세",
      key: "action",
      width: 100,
      render: (_, record) => (
        <Button onClick={() => showModal(record)}>상세 보기</Button>
      ),
    },
  ];

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#000000", // 기본 메인 컬러는 시크한 블랙
          controlItemBgActive: "#f3f4f6", // 💡 셀렉트박스 선택된 항목 배경 (연한 회색)
          controlItemBgHover: "#f9fafb", // 💡 마우스 올렸을 때 배경 (더 연한 회색)
        },
      }}
    >
      <div style={{ padding: "24px", maxWidth: "1400px", margin: "0 auto" }}>
        {modernBlackStyle} {/* 아까 추가했던 CSS 변수 */}
        {/* 💡 여기에 스타일을 추가해서 프로젝트에 적용합니다. */}
        <Title level={3}>신규 상품 수집</Title>
        {/* Step 1: 수집 */}
        <Card
          title="Step 1. 소싱 URL 입력 (현재 서비스 가능 소싱처: IHerb)"
          style={{ marginBottom: "32px", borderRadius: "8px" }}
        >
          <TextArea
            rows={3}
            value={urls}
            onChange={(e) => setUrls(e.target.value)}
          />
          <div style={{ marginTop: "16px", textAlign: "right" }}>
            <Button
              loading={isLoading}
              onClick={handleCrawl}
              style={{
                backgroundColor: "#000", // 새까만 블랙
                color: "#fff", // 하얀 글씨
                border: "1px solid #000",
                fontWeight: "bold",
                padding: "20px 20px",
                borderRadius: "6px",
              }}
              // hover시 회색으로 변하게 하는 테일윈드 클래스 추가
              className="hover:bg-gray-800"
            >
              상품정보 수집하기
            </Button>
          </div>
        </Card>
        {/* Step 2: 검토 및 저장 */}
        {products.length > 0 && (
          <Card title="Step 2. 상품 정보 검토 및 저장">
            {/* 🚀 일괄 마진율 제어 패널 */}
            <div
              style={{
                marginBottom: "16px",
                padding: "16px",
                background: "#f8fafc",
                borderRadius: "8px",
                border: "1px solid #e2e8f0", // 연한 회색 테두리
                display: "flex",
                justifyContent: "space-between",
              }}
            >
              <Space>
                <Text type="secondary">전체 마진율(%):</Text>
                <InputNumber
                  value={globalMarginRate}
                  onChange={setGlobalMarginRate}
                />
                <Button onClick={applyGlobalMargin}>일괄 적용</Button>
              </Space>
            </div>

            <Table
              // Ant Design 내장 체크박스 기능 활성화
              rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
              columns={columns}
              dataSource={products}
              rowKey="id"
              pagination={false}
              bordered
            />

            <Divider />

            <div style={{ textAlign: "right", marginTop: "24px" }}>
              <Button
                size="large"
                onClick={saveToLocalDB}
                style={{
                  backgroundColor: "#000", // 새까만 블랙
                  color: "#fff", // 하얀 글씨
                  border: "1px solid #000",
                  fontWeight: "bold",
                  padding: "20px 20px",
                  borderRadius: "6px",
                }}
                // hover시 회색으로 변하게 하는 테일윈드 클래스 추가
                className="hover:bg-gray-800"
              >
                선택한 상품을 상품관리에 저장
              </Button>
            </div>
          </Card>
        )}
        {/* 모달 창 (상세 HTML 렌더링 확인용) */}
        <Modal
          title="상세 템플릿 미리보기"
          open={isModalVisible}
          onCancel={handleModalClose}
          footer={[
            <Button key="close" onClick={handleModalClose}>
              닫기
            </Button>,
          ]}
          width={900}
        >
          {currentDetailProduct && (
            <div
              style={{
                padding: "16px",
                borderRadius: "8px",
                maxHeight: "600px",
                overflowY: "auto",
              }}
              dangerouslySetInnerHTML={{
                __html: currentDetailProduct.rawSourceHtml,
              }}
            />
          )}
        </Modal>
      </div>
    </ConfigProvider>
  );
};

// 3. 밖에서 이 컴포넌트를 쓸 수 있도록 내보냅니다.
export default ProductRegisterPage;
