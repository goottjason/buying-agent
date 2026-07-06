import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { Card, Typography, Button, Space, Tag, Input, Select, message } from 'antd';
import { SyncOutlined, SearchOutlined, GlobalOutlined } from '@ant-design/icons';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css'; // 기본 alpine 테마 사용

const { Title, Text } = Typography;
const { Option } = Select;

export default function SourcingProductPage() {
  const [gridApi, setGridApi] = useState(null);
  const [searchText, setSearchText] = useState('');
  
  // 실제 백엔드 API 연동
  const [rowData, setRowData] = useState([]);
  
  const fetchProducts = useCallback(async () => {
    try {
      const response = await fetch('http://localhost:8099/api/v1/sourcing-products');
      if (response.ok) {
        const json = await response.json();
        setRowData(json.data || []);
      }
    } catch (error) {
      console.error("데이터 로드 실패:", error);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const onGridReady = (params) => {
    setGridApi(params.api);
    params.api.sizeColumnsToFit();
  };

  const onFilterTextChange = (e) => {
    setSearchText(e.target.value);
    if (gridApi) {
      gridApi.setQuickFilter(e.target.value);
    }
  };

  const handleManualSync = useCallback(async (id) => {
    message.loading({ content: '소싱처와 동기화 중...', key: 'sync' });
    try {
      await fetch(`http://localhost:8099/api/v1/sync/single/${id}`, { method: 'POST' });
      message.success({ content: '수동 동기화 완료!', key: 'sync', duration: 2 });
      fetchProducts(); // 재조회
    } catch (e) {
      message.error({ content: '동기화 실패!', key: 'sync', duration: 2 });
    }
  }, [fetchProducts]);

  const columnDefs = useMemo(() => [
    {
      headerName: "이미지",
      field: "imageUrl",
      width: 90,
      cellRenderer: (params) => (
        <img src={params.value} alt="product" style={{ width: '40px', height: '40px', borderRadius: '4px', objectFit: 'cover', border: '1px solid #e5e7eb' }} />
      )
    },
    { 
      headerName: "소싱처", 
      field: "site", 
      width: 120,
      cellRenderer: (params) => {
        let color = 'default';
        if(params.value === 'iHerb') color = 'green';
        if(params.value === 'Amazon') color = 'orange';
        if(params.value === 'Tesco') color = 'blue';
        if(params.value === 'Ocado') color = 'purple';
        return <Tag color={color}>{params.value}</Tag>;
      }
    },
    { headerName: "상품명", field: "name", flex: 1, filter: true },
    { headerName: "소싱 원가", field: "sourcePrice", width: 110 },
    { headerName: "마켓 판매가", field: "targetPrice", width: 110 },
    { 
      headerName: "재고 상태", 
      field: "stock", 
      width: 130,
      cellRenderer: (params) => (
        <Tag color={params.value === 'IN_STOCK' ? 'cyan' : 'red'}>
          {params.value === 'IN_STOCK' ? '정상 판매중' : '품절'}
        </Tag>
      )
    },
    { headerName: "최근 동기화", field: "lastSync", width: 160 },
    {
      headerName: "관리",
      width: 120,
      cellRenderer: (params) => (
        <Button 
          type="primary" 
          size="small" 
          icon={<SyncOutlined />} 
          onClick={() => handleManualSync(params.data.id)}
          style={{ background: 'var(--primary-gradient)', border: 'none' }}
        >
          동기화
        </Button>
      )
    }
  ], [handleManualSync]);

  return (
    <div className="glass-panel animate-fade-in" style={{ padding: '24px', display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <Title level={3} style={{ margin: 0 }} className="gradient-text">
            <GlobalOutlined style={{ marginRight: '8px' }} />
            다중 소싱처 상품 조회
          </Title>
          <Text type="secondary" style={{ marginTop: '4px', display: 'block' }}>
            다양한 글로벌 소싱처의 상품 목록을 조회하고 실시간 동기화를 제어합니다.
          </Text>
        </div>
        <Space>
          <Input
            placeholder="상품명 또는 소싱처 검색..."
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={onFilterTextChange}
            style={{ width: 250, borderRadius: '8px' }}
          />
          <Button type="primary" style={{ background: '#10b981', border: 'none' }}>
            일괄 동기화 (Batch)
          </Button>
        </Space>
      </div>

      <Card 
        bodyStyle={{ padding: 0 }} 
        style={{ flex: 1, borderRadius: '12px', overflow: 'hidden', border: '1px solid #e5e7eb', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }}
      >
        {/* ag-Grid 영역 */}
        <div className="ag-theme-alpine" style={{ height: '500px', width: '100%' }}>
          <AgGridReact
            rowData={rowData}
            columnDefs={columnDefs}
            onGridReady={onGridReady}
            rowHeight={60}
            headerHeight={50}
            pagination={true}
            paginationPageSize={10}
            animateRows={true}
            defaultColDef={{
              sortable: true,
              resizable: true,
            }}
          />
        </div>
      </Card>
    </div>
  );
}
