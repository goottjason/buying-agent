// 파일 위치: frontend/src/components/ProductModal.jsx
import React, { useState, useEffect } from "react";

/**
 * @param isOpen : 이 팝업창을 띄울지 말지 결정하는 boolean (true면 보이고 false면 숨김)
 * @param onClose : 팝업창을 닫으라고 부모에게 알리는 함수
 * @param product : 부모가 넘겨준 상품 데이터 (null이면 '새 상품 등록' 모드, 값이 있으면 '수정' 모드)
 * @param onSave : 저장 버튼을 눌렀을 때 실행할 함수
 */
export default function ProductModal({ isOpen, onClose, product, onSave }) {
    // --------------------------------------------------
    // 1. 상태(State) 관리: 입력 폼의 데이터들
    // --------------------------------------------------
    // DTO 클래스를 만들어서 필드값을 관리하는 것과 비슷합니다.
    const [formData, setFormData] = useState({
                                                 sku: "",
                                                 name: "",
                                                 category: "",
                                                 salePrice: 0,
                                                 memo: "",
                                                 detailHtml: ""
                                             });

    // --------------------------------------------------
    // 2. 초기화 (useEffect)
    // --------------------------------------------------
    // 모달창이 열릴 때마다(isOpen) 또는 선택된 상품(product)이 바뀔 때마다 실행됩니다.
    useEffect(() => {
        if (product) {
            // [수정 모드] 부모가 상품을 넘겨줬으면, 그 정보로 입력칸을 채웁니다.
            setFormData({
                            sku: product.sku || "",
                            name: product.name || "",
                            category: product.category || "",
                            salePrice: product.priceInfo?.salePrice || 0, // VO(객체) 안에 있는 값 꺼내기
                            memo: product.memo || "",
                            detailHtml: product.detailHtml || ""
                        });
        } else {
            // [등록 모드] 아무것도 안 넘겨줬으면, 입력칸을 백지 상태로 비웁니다.
            setFormData({
                            sku: "", name: "", category: "", salePrice: 0, memo: "", detailHtml: ""
                        });
        }
    }, [product, isOpen]); // 이 두 변수가 변할 때만 다시 실행하라는 뜻!

    // 만약 isOpen이 false면 아무것도 화면에 그리지 않고 종료(return null)합니다.
    if (!isOpen) return null;

    // --------------------------------------------------
    // 3. 입력값 변경 처리 메서드
    // --------------------------------------------------
    // 사용자가 입력칸에 글씨를 칠 때마다 formData를 업데이트합니다.
    const handleChange = (e) => {
        const { name, value } = e.target;
        // 기존 formData를 복사(...formData)한 뒤, 방금 수정한 필드(name)의 값(value)만 덮어씁니다.
        setFormData({ ...formData, [name]: value });
    };

    // 저장 버튼을 누르면 부모가 준 onSave 메서드에 formData를 담아서 넘겨줍니다.
    const handleSave = () => {
        onSave(formData);
    };

    // --------------------------------------------------
    // 4. 화면 그리기 (모달창 UI)
    // --------------------------------------------------
    return (
        // 바깥쪽 어두운 배경 (클릭 시 창 닫기)
        <div style={overlayStyle} onClick={onClose}>
            {/* 안쪽 하얀색 모달 창 (e.stopPropagation: 안쪽을 클릭했을 땐 창이 안 닫히게 막음) */}
            <div style={modalStyle} onClick={(e) => e.stopPropagation()}>

                {/* 상단 제목 */}
                <h3 style={{ marginTop: 0 }}>
                    {product ? "📝 상품 상세 및 수정" : "✨ 새 상품 등록"}
                </h3>

                {/* 입력 폼 영역 */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    <label>
                        <b>SKU (고유코드)</b><br/>
                        <input type="text" name="sku" value={formData.sku} onChange={handleChange}
                               disabled={product !== null} /* 수정 모드일 땐 SKU 변경 불가! */
                               style={inputStyle} />
                    </label>

                    <label>
                        <b>상품명</b><br/>
                        <input type="text" name="name" value={formData.name} onChange={handleChange} style={inputStyle} />
                    </label>

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <label style={{ flex: 1 }}>
                            <b>카테고리</b><br/>
                            <select name="category" value={formData.category} onChange={handleChange} style={inputStyle}>
                                <option value="">선택</option>
                                <option value="FOOD">FOOD</option>
                                <option value="BEAUTY">BEAUTY</option>
                            </select>
                        </label>
                        <label style={{ flex: 1 }}>
                            <b>판매가 (원)</b><br/>
                            <input type="number" name="salePrice" value={formData.salePrice} onChange={handleChange} style={inputStyle} />
                        </label>
                    </div>

                    <label>
                        <b>메모</b><br/>
                        <input type="text" name="memo" value={formData.memo} onChange={handleChange} style={inputStyle} />
                    </label>

                    <label>
                        <b>상세설명 HTML (Detail HTML)</b><br/>
                        {/* 엑셀에서 길어서 잘렸던 그 긴 문자열을 큰 텍스트박스에서 볼 수 있습니다! */}
                        <textarea name="detailHtml" value={formData.detailHtml} onChange={handleChange}
                                  style={{ ...inputStyle, height: '150px', resize: 'vertical' }} />
                    </label>
                </div>

                {/* 하단 버튼 영역 */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                    <button onClick={onClose} style={cancelBtnStyle}>취소</button>
                    <button onClick={handleSave} style={saveBtnStyle}>저장하기</button>
                </div>
            </div>
        </div>
    );
}

// --- CSS 스타일 (원래 별도 파일로 빼기도 하지만, 편의상 여기에 변수로 둡니다) ---
const overlayStyle = {
    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 999,
    display: 'flex', justifyContent: 'center', alignItems: 'center'
};
const modalStyle = {
    backgroundColor: 'white', padding: '20px', borderRadius: '8px',
    width: '500px', maxHeight: '90vh', overflowY: 'auto',
    boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
};
const inputStyle = { width: '100%', padding: '8px', boxSizing: 'border-box', marginTop: '4px' };
const cancelBtnStyle = { padding: '8px 16px', background: '#ccc', border: 'none', borderRadius: '4px', cursor: 'pointer' };
const saveBtnStyle = { padding: '8px 16px', background: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' };