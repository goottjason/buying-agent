import React, { useState } from 'react';
import { Card, Typography, Form, InputNumber, Button, Row, Col, Divider, Select, message, Tabs } from 'antd';
import { SaveOutlined, SettingOutlined, DollarOutlined, PercentageOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { Option } = Select;

export default function MarginPolicyPage() {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('iHerb');

  const onFinish = (values) => {
    message.loading({ content: '정책 저장 중...', key: 'save' });
    setTimeout(() => {
      message.success({ content: `${activeTab} 마진 정책이 성공적으로 저장되었습니다.`, key: 'save', duration: 2 });
    }, 1000);
  };

  const renderPolicyForm = (siteName, currency) => (
    <Form
      form={form}
      layout="vertical"
      onFinish={onFinish}
      initialValues={{
        exchangeRate: currency === 'USD' ? 1400 : 1750,
        marginRate: 30,
        shippingFee: 10000,
        commissionRate: 11
      }}
    >
      <Row gutter={24}>
        <Col span={12}>
          <Card size="small" title="환율 및 배송비 설정" bordered={false} style={{ background: 'rgba(255,255,255,0.5)', borderRadius: '8px' }}>
            <Form.Item label="적용 환율 (원)" name="exchangeRate" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} prefix={<DollarOutlined />} formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} />
            </Form.Item>
            <Form.Item label="기본 배송비 (원)" name="shippingFee" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} />
            </Form.Item>
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" title="마진 및 수수료 설정" bordered={false} style={{ background: 'rgba(255,255,255,0.5)', borderRadius: '8px' }}>
            <Form.Item label="목표 마진율 (%)" name="marginRate" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} prefix={<PercentageOutlined />} min={0} max={100} />
            </Form.Item>
            <Form.Item label="오픈마켓 수수료율 (%)" name="commissionRate" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} prefix={<PercentageOutlined />} min={0} max={100} />
            </Form.Item>
          </Card>
        </Col>
      </Row>
      
      <Divider />
      
      <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '24px' }}>
        <Text strong style={{ color: '#475569' }}>💡 최종 판매가 계산 공식:</Text>
        <br />
        <Text type="secondary" style={{ fontFamily: 'monospace' }}>
          (소싱 원가 × 환율 + 배송비) × (1 + 마진율) × (1 + 오픈마켓 수수료율)
        </Text>
      </div>

      <Form.Item style={{ textAlign: 'right', marginBottom: 0 }}>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} size="large" style={{ background: 'var(--primary-gradient)', border: 'none' }}>
          정책 저장 및 즉시 적용
        </Button>
      </Form.Item>
    </Form>
  );

  const items = [
    { key: 'iHerb', label: 'iHerb (미국/USD)', children: renderPolicyForm('iHerb', 'USD') },
    { key: 'Amazon US', label: 'Amazon (미국/USD)', children: renderPolicyForm('Amazon US', 'USD') },
    { key: 'Amazon UK', label: 'Amazon (영국/GBP)', children: renderPolicyForm('Amazon UK', 'GBP') },
    { key: 'Ocado', label: 'Ocado (영국/GBP)', children: renderPolicyForm('Ocado', 'GBP') },
    { key: 'Tesco', label: 'Tesco (영국/GBP)', children: renderPolicyForm('Tesco', 'GBP') },
  ];

  return (
    <div className="glass-panel animate-fade-in" style={{ padding: '24px', maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ marginBottom: '24px' }}>
        <Title level={3} style={{ margin: 0 }} className="gradient-text">
          <SettingOutlined style={{ marginRight: '8px' }} />
          마진 정책 및 시스템 설정
        </Title>
        <Text type="secondary" style={{ marginTop: '4px', display: 'block' }}>
          다중 소싱처별 환율, 배송비 및 마진율 정책을 독립적으로 관리합니다.
        </Text>
      </div>

      <Card style={{ borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)', border: '1px solid #e5e7eb' }}>
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab} 
          items={items}
          type="card"
          size="large"
        />
      </Card>
    </div>
  );
}
