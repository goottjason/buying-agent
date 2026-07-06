import React, { useState, useEffect, useCallback } from 'react';
import { Card, Typography, Table, Tag, Input, Space, DatePicker, Select } from 'antd';
import { HistoryOutlined, SearchOutlined, CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

export default function SyncLogPage() {
  const [searchText, setSearchText] = useState('');

  // 실제 API 연동
  const [data, setData] = useState([]);

  const fetchLogs = useCallback(async () => {
    try {
      const response = await fetch('http://localhost:8099/api/v1/sync-logs');
      if (response.ok) {
        const json = await response.json();
        setData(json.data || []);
      }
    } catch (error) {
      console.error("로그 데이터 로드 실패:", error);
    }
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const columns = [
    { title: '시간', dataIndex: 'time', key: 'time', width: 180 },
    { 
      title: '소싱처', 
      dataIndex: 'site', 
      key: 'site',
      width: 120,
      render: (text) => {
        let color = 'default';
        if(text.includes('iHerb')) color = 'green';
        if(text.includes('Amazon')) color = 'orange';
        if(text.includes('Tesco')) color = 'blue';
        if(text.includes('Ocado')) color = 'purple';
        return <Tag color={color}>{text}</Tag>;
      }
    },
    { title: '상품명', dataIndex: 'product', key: 'product', width: 250, ellipsis: true },
    { 
      title: '동기화 타입', 
      dataIndex: 'type', 
      key: 'type',
      width: 130,
      render: (text) => <Tag>{text}</Tag>
    },
    { 
      title: '상태', 
      dataIndex: 'status', 
      key: 'status',
      width: 150,
      render: (status) => {
        if (status === 'SUCCESS') return <Tag icon={<CheckCircleOutlined />} color="success">성공</Tag>;
        if (status === 'CRAWL_ERROR') return <Tag icon={<ExclamationCircleOutlined />} color="warning">크롤링 오류</Tag>;
        return <Tag icon={<CloseCircleOutlined />} color="error">실패</Tag>;
      }
    },
    { title: '메시지', dataIndex: 'msg', key: 'msg' },
  ];

  return (
    <div className="glass-panel animate-fade-in" style={{ padding: '24px', display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <Title level={3} style={{ margin: 0 }} className="gradient-text">
            <HistoryOutlined style={{ marginRight: '8px' }} />
            동기화 이력 로그
          </Title>
          <Text type="secondary" style={{ marginTop: '4px', display: 'block' }}>
            다중 소싱처의 가격, 재고, 이미지 동기화 내역 및 크롤링 오류(DOM 변경 등)를 추적합니다.
          </Text>
        </div>
      </div>

      <Card 
        bordered={false}
        style={{ marginBottom: '16px', background: 'rgba(255,255,255,0.6)', borderRadius: '12px' }}
        bodyStyle={{ padding: '16px 24px' }}
      >
        <Space size="middle" wrap>
          <RangePicker style={{ width: 250 }} />
          <Select defaultValue="ALL" style={{ width: 120 }}>
            <Select.Option value="ALL">전체 상태</Select.Option>
            <Select.Option value="SUCCESS">성공</Select.Option>
            <Select.Option value="CRAWL_ERROR">크롤링 오류</Select.Option>
            <Select.Option value="FAIL">실패</Select.Option>
          </Select>
          <Input 
            placeholder="상품명 검색..." 
            prefix={<SearchOutlined />} 
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            style={{ width: 200 }}
          />
        </Space>
      </Card>

      <Card 
        bodyStyle={{ padding: 0 }} 
        style={{ flex: 1, borderRadius: '12px', overflow: 'hidden', border: '1px solid #e5e7eb', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }}
      >
        <Table 
          columns={columns} 
          dataSource={data} 
          pagination={{ pageSize: 10 }}
          scroll={{ y: 500 }}
        />
      </Card>
    </div>
  );
}
