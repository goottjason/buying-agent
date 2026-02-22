// 파일 위치: frontend/src/pages/ProductDashboard.jsx

// 1. 외부 라이브러리 및 도구 불러오기 (자바의 import와 동일)
import React, { useState, useEffect, useRef } from "react";
import { AgGridReact } from "ag-grid-react"; // 엑셀 표를 그려주는 외부 라이브러리
import { productApi } from "../api/productApi"; // 우리가 만든 백엔드 통신용 API 클래스

// 2. 엑셀 표의 디자인(CSS) 파일 불러오기
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-alpine.css";

// 3. 메인 화면(컴포넌트) 정의 및 내보내기 (자바의 public class 역할)
export default function ProductDashboard() {

    // ==========================================
    // [1] 상태(State) 관리 영역: 화면을 움직이는 심장
    // ==========================================
    // 리액트에서는 변수 값이 바뀔 때 화면도 같이 바뀌어야 한다면 반드시 useState를 씁니다.
    // 문법: const [변수명, Setter함수] = useState(초기값);

    // 엑셀 표에 그릴 3천 개의 데이터를 담는 List 입니다.
    const [rowData, setRowData] = useState([]);

    // 상단 검색창에 입력하는 '검색어'와 '카테고리'를 기억하는 변수입니다.
    const [searchKeyword, setSearchKeyword] = useState("");
    const [searchCategory, setSearchCategory] = useState("");

    // ★ [핵심] 사용자가 수정한 엑셀 줄들을 모아두는 '바구니' 역할을 하는 Map(객체) 입니다.
    // Key는 상품의 SKU, Value는 수정된 상품 전체 데이터가 들어갑니다.
    const [modifiedRows, setModifiedRows] = useState({});

    // ==========================================
    // [2] 리모컨(Ref) 영역: 컴포넌트 직접 조종기
    // ==========================================
    // 화면을 새로고침하지 않고, 엑셀 표(AG Grid) 자체에 "체크된 항목 가져와!" 같은 명령을 내릴 때 쓰는 리모컨입니다.
    const gridRef = useRef();

    // ==========================================
    // [3] 통신(Fetch) 및 생명주기(Lifecycle) 영역
    // ==========================================

    // 백엔드에서 데이터를 가져와서 화면에 뿌려주는 핵심 메서드입니다.
    const loadData = () => {
        // 우리가 분리해둔 api 파일의 메서드를 호출합니다. (검색어와 카테고리를 파라미터로 던짐)
        productApi.fetchProducts(searchKeyword, searchCategory)
                  .then(data => {
                      // 통신 성공! 받아온 데이터를 rowData에 넣습니다. (이 순간 화면의 엑셀 표가 쫙 그려짐)
                      setRowData(data.content);
                      // 데이터를 새로 불러왔으니, 담아뒀던 '수정 바구니'도 깨끗하게 비워줍니다.
                      setModifiedRows({});
                  })
                  .catch(error => {
                      alert("데이터를 불러오지 못했습니다.");
                      console.error(error);
                  });
    };

    // useEffect는 화면이 '처음 켜질 때' 딱 한 번만 실행하고 싶은 코드를 넣는 곳입니다.
    // 자바 스프링의 @PostConstruct 나 생성자와 비슷한 역할입니다.
    useEffect(() => {
        loadData(); // 화면이 켜지자마자 데이터를 한 번 불러옵니다.
    }, []); // 뒤에 있는 빈 배열 [] 은 "처음 렌더링 될 때 딱 한 번만 실행해!" 라는 리액트의 규칙입니다.

    // ==========================================
    // [4] 사용자 액션(클릭, 수정 등) 처리 영역
    // ==========================================

    // [검색 버튼 클릭 시] -> 검색어를 넣은 채로 데이터를 다시 불러옵니다.
    const handleSearch = () => {
        loadData();
    };

    // ★ [인라인 에디팅 감지] -> 사용자가 엑셀 표 안에서 글자를 수정하고 엔터를 치는 순간 실행됩니다!
    const onCellValueChanged = (event) => {
        // event.data 에는 방금 수정이 완료된 해당 줄의 '전체 데이터'가 들어있습니다.
        const updatedRow = event.data;

        // 수정 바구니(modifiedRows)에 방금 수정한 데이터를 담습니다.
        setModifiedRows(prev => ({
            ...prev,                     // 기존에 바구니에 있던 다른 데이터들은 그대로 유지하고,
            [updatedRow.sku]: updatedRow // 방금 수정한 상품을 SKU를 Key값으로 해서 덮어씌웁니다.
        }));
    };

    // ★ [일괄 저장 버튼 클릭 시] -> 바구니에 모인 데이터를 한 번에 백엔드로 쏩니다!
    const handleSaveChanges = () => {
        // 바구니(객체)에 담긴 Value값들만 쏙쏙 뽑아서 Java의 List(배열) 형태로 만듭니다.
        const modifiedList = Object.values(modifiedRows);

        // 만약 바구니가 비어있다면 함수를 종료합니다.
        if (modifiedList.length === 0) {
            return alert("수정된 내용이 없습니다.");
        }

        // 백엔드 엔티티(VO) 구조와 100% 똑같이 전체 데이터를 매핑해서 보냅니다!
        const requestData = modifiedList.map(row => ({
            sku: row.sku,
            name: row.name,
            originalName: row.originalName,
            brand: row.brand,
            category: row.category,
            sourcingInfo: row.sourcingInfo,     // 소싱 정보 통째로!
            productSpec: row.productSpec,       // 스펙 정보 통째로!
            priceInfo: row.priceInfo,           // 가격 정보 통째로!
            logisticsInfo: row.logisticsInfo,   // 물류/재고 정보 통째로!
            searchKeywords: row.searchKeywords,
            memo: row.memo,
            detailHtml: row.detailHtml
        }));

        // 사용자에게 진짜 저장할 건지 최종 확인을 받습니다.
        if (!window.confirm(`수정된 ${requestData.length}개의 상품을 일괄 저장하시겠습니까?`)) return;

        // api 통신 메서드 호출 (PUT 메서드로 일괄 전송)
        productApi.bulkEdit(requestData)
                  .then(data => {
                      alert(data.message); // "성공적으로 수정되었습니다" 알림 띄우기
                      loadData();          // 저장이 완료되었으니 화면 데이터를 최신화(새로고침) 합니다.
                  })
                  .catch(err => {
                      const errMsg = err.response?.data?.message || err.message;
                      alert("저장 실패 원인: " + errMsg);
                  });
    };

    // [선택 삭제 버튼 클릭 시] -> 좌측 체크박스에 체크된 상품들을 DB에서 지웁니다.
    const handleBulkDelete = () => {
        // 리모컨(gridRef)을 조종해서 현재 엑셀 표에서 체크박스가 선택된 줄의 데이터만 쏙 빼옵니다.
        const selectedNodes = gridRef.current.api.getSelectedNodes();
        const selectedData = selectedNodes.map(node => node.data);

        if (selectedData.length === 0) return alert("삭제할 상품을 체크해 주세요!");
        if (!window.confirm(`${selectedData.length}개 상품을 삭제할까요?`)) return;

        // 선택된 데이터들에서 SKU(고유코드)만 뽑아서 배열로 만듭니다. [ "SKU-1", "SKU-2" ]
        const skus = selectedData.map(item => item.sku);

        // 삭제 통신 API 호출
        productApi.bulkDelete(skus)
                  .then(data => {
                      alert(data.message);
                      loadData(); // 삭제 후 화면 최신화
                  })
                  .catch(err => alert("삭제 실패"));
    };

    // ==========================================
    // [5] 엑셀(AG Grid) 컬럼 디자인 세팅 영역
    // ==========================================
    // defaultColDef: 모든 열(Column)에 기본적으로 적용될 공통 규칙입니다.
    const defaultColDef = {
        resizable: true, // 사용자가 마우스로 열의 너비를 늘리고 줄일 수 있게 허용
        sortable: true,  // 열 제목을 클릭하면 오름차순/내림차순 정렬 기능 허용
        filter: true     // 열 제목에 돋보기(필터) 아이콘 생성 허용
    };

    // colDefs: 화면에 보여줄 엑셀 열(필드)들을 순서대로 정의합니다.
    const [colDefs] = useState([
                                   {
                                       field: "sku",
                                       headerName: "SKU",
                                       checkboxSelection: true,       // 각 줄 맨 앞에 체크박스 만들기
                                       headerCheckboxSelection: true, // 제목 열에 '전체 선택' 체크박스 만들기
                                       width: 150,
                                       pinned: "left"                 // 가로 스크롤을 해도 SKU는 항상 왼쪽에 틀 고정!
                                   },
                                   {
                                       headerName: "이미지",
                                       width: 80,
                                       // cellRenderer: 데이터를 글자가 아니라 그림(img 태그)이나 버튼으로 보여주고 싶을 때 사용합니다.
                                       cellRenderer: (params) => {
                                           // 서버에 올라간 이미지가 있으면 그걸 쓰고, 없으면 소싱처 원본 이미지를 씁니다.
                                           const imgList = params.data.imageInfo?.hostedImages?.length > 0
                                                           ? params.data.imageInfo.hostedImages
                                                           : params.data.imageInfo?.sourceImages;

                                           // 이미지가 하나라도 존재하면 조그만 썸네일(40x40)로 그려줍니다.
                                           if (imgList && imgList.length > 0) {
                                               return <img src={imgList[0]} alt="상품 이미지" style={{ width: "40px", height: "40px", objectFit: "cover", borderRadius: "4px", marginTop: "4px" }} />;
                                           }
                                           return "없음";
                                       }
                                   },
                                   { field: "name", headerName: "상품명", width: 300, editable: true }, // editable: true = 더블클릭해서 수정 가능!
                                   { field: "originalName", headerName: "원어상품명", width: 200, editable: true },
                                   { field: "brand", headerName: "브랜드(기본)", width: 120, editable: true },
                                   {
                                       field: "category",
                                       headerName: "카테고리",
                                       width: 120,
                                       editable: true,
                                       // 더블클릭 시 직접 타이핑하는 대신 콤보박스(Select)가 열리도록 하는 마법의 옵션!
                                       cellEditor: 'agSelectCellEditor',
                                       cellEditorParams: { values: ['FOOD', 'BEAUTY'] }
                                   },
                                   { field: "sourcingInfo.vendor", headerName: "소싱처", width: 100, editable: true },
                                   { field: "sourcingInfo.manufacturer", headerName: "제조사", width: 150, editable: true },
                                   { field: "sourcingInfo.origin", headerName: "원산지", width: 100, editable: true },
                                   { field: "sourcingInfo.hsCode", headerName: "HS 코드", width: 100, editable: true },
                                   {
                                       field: "sourcingInfo.sourceUrl",
                                       headerName: "원본링크",
                                       width: 200,
                                       cellRenderer: (params) => {
                                           // 주소가 있으면 복잡한 URL 대신 '링크 열기'라는 파란색 글씨를 만들어줍니다.
                                           return params.value ? <a href={params.value} target="_blank" rel="noreferrer">링크 열기</a> : "";
                                       }
                                   },
                                   { field: "productSpec.barcode", headerName: "바코드", width: 150, editable: true },
                                   { field: "productSpec.capacity", headerName: "용량", width: 100, editable: true },
                                   { field: "productSpec.measureUnit", headerName: "단위", width: 100, editable: true },
                                   // type: "numericColumn" 은 엑셀처럼 숫자를 오른쪽으로 정렬해 주는 기능입니다.
                                   { field: "priceInfo.costPrice", headerName: "원가", width: 100, type: "numericColumn", editable: true },
                                   { field: "priceInfo.exchangeRate", headerName: "환율", width: 100, type: "numericColumn", editable: true },
                                   { field: "priceInfo.deliveryFee", headerName: "배송비", width: 100, type: "numericColumn", editable: true },
                                   { field: "priceInfo.marginRate", headerName: "마진율(%)", width: 100, type: "numericColumn", editable: true },
                                   { field: "priceInfo.salePrice", headerName: "판매가", width: 100, type: "numericColumn", editable: true },
                                   { field: "logisticsInfo.stock", headerName: "재고", width: 100, type: "numericColumn", editable: true },
                                   { field: "logisticsInfo.weight", headerName: "무게", width: 100, type: "numericColumn", editable: true },
                                   { field: "logisticsInfo.bundleQuantity", headerName: "묶음수량", width: 100, type: "numericColumn", editable: true },
                                   { field: "searchKeywords", headerName: "검색어", width: 200, editable: true },
                                   { field: "memo", headerName: "메모", width: 200, editable: true },
                               ]);

    // ==========================================
    // [6] 화면 그리기 (HTML / JSX 영역)
    // ==========================================
    // 리액트는 return 안쪽에 적힌 HTML 태그들을 화면에 그려줍니다.

    // 바구니에 담긴 수정된 상품이 총 몇 개인지 숫자를 셉니다.
    const modifiedCount = Object.keys(modifiedRows).length;

    return (
        // 전체 화면을 감싸는 가장 바깥쪽 투명 박스 (높이를 화면 전체 100vh로 잡음)
        <div style={{ padding: '20px', height: '100vh', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }}>
            <h2>📦 상품 관리 대시보드 (PIM)</h2>

            {/* --- 상단 검색 필터 영역 --- */}
            <div style={{ display: 'flex', gap: '10px', marginBottom: '15px', padding: '10px', background: '#f8f9fa', borderRadius: '8px' }}>
                <input
                    type="text"
                    placeholder="상품명, 브랜드 등 검색"
                    value={searchKeyword} // 상태 변수 연결 (Getter)
                    onChange={(e) => setSearchKeyword(e.target.value)} // 글자를 칠 때마다 변수에 값을 밀어넣음 (Setter)
                    style={{ padding: '6px', width: '250px' }}
                />
                <select
                    value={searchCategory}
                    onChange={(e) => setSearchCategory(e.target.value)}
                    style={{ padding: '6px' }}
                >
                    <option value="">카테고리 전체</option>
                    <option value="FOOD">식품 (FOOD)</option>
                    <option value="BEAUTY">뷰티 (BEAUTY)</option>
                </select>
                <button onClick={handleSearch} style={{ padding: '6px 15px', background: '#333', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    🔍 검색
                </button>
            </div>

            {/* --- 조작 버튼 및 알림 메시지 영역 --- */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>

                {/* 수정한 항목이 1개라도 있으면 빨간색 경고 문구를 띄우고, 없으면 팁 문구를 띄웁니다. */}
                <div style={{ color: '#d9534f', fontWeight: 'bold' }}>
                    {modifiedCount > 0
                     ? `✏️ ${modifiedCount}개의 상품이 수정되었습니다. 우측의 저장 버튼을 눌러주세요!`
                     : "💡 표 안의 내용을 더블클릭하면 엑셀처럼 바로 수정할 수 있습니다."}
                </div>

                <div style={{ display: 'flex', gap: '10px' }}>
                    {/* [일괄 저장 버튼] 수정한 데이터가 있을 때만 파란색으로 활성화되는 다이나믹 버튼입니다! */}
                    <button
                        onClick={handleSaveChanges}
                        style={{
                            padding: '6px 16px',
                            fontWeight: 'bold',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: modifiedCount > 0 ? 'pointer' : 'not-allowed', // 수정 데이터가 없으면 마우스 커서를 막음
                            background: modifiedCount > 0 ? '#007bff' : '#e9ecef', // 파란색(활성) or 회색(비활성)
                            color: modifiedCount > 0 ? 'white' : '#6c757d'
                        }}
                    >
                        💾 변경사항 일괄 저장
                    </button>

                    <button onClick={handleBulkDelete} style={{ padding: '6px 12px', background: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                        🗑️ 선택 삭제
                    </button>
                </div>
            </div>

            {/* --- 실제 엑셀 표(AG Grid)가 그려지는 영역 --- */}
            <div className="ag-theme-alpine" style={{ flex: 1, width: '100%' }}>
                <AgGridReact
                    ref={gridRef}                  // 리모컨 장착
                    rowData={rowData}              // 표에 뿌릴 데이터 리스트
                    columnDefs={colDefs}           // 표의 열(Column) 구조
                    defaultColDef={defaultColDef}  // 공통 옵션
                    rowSelection="multiple"        // 다중 선택 허용
                    pagination={true}              // 페이징 처리 켜기
                    paginationPageSize={50}        // 한 페이지에 50개씩 보여주기

                    // ★ 누군가 더블클릭해서 셀 값을 바꾸고 엔터를 칠 때마다 실행되는 이벤트 핸들러!
                    onCellValueChanged={onCellValueChanged}
                />
            </div>

        </div>
    );
}