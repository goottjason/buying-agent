import React, { useState } from "react";
import { Layout, Menu, Typography, Avatar, Dropdown, Space, Badge } from "antd";
import { 
  AppstoreOutlined, 
  ShoppingCartOutlined, 
  ShopOutlined, 
  SettingOutlined,
  CloudSyncOutlined,
  HistoryOutlined,
  BellOutlined,
  UserOutlined,
  GlobalOutlined
} from "@ant-design/icons";
import { useNavigate, useLocation, Outlet } from "react-router-dom";

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  // 메뉴 설정 (다중 소싱처 확장을 고려한 프리미엄 관리자 메뉴)
  const menuItems = [
    { key: "/", icon: <AppstoreOutlined />, label: "Dashboard" },
    { type: 'divider' },
    { 
      key: "sourcing", 
      label: "소싱 관리 (Sourcing)", 
      type: 'group',
      children: [
        { key: "/sourcing-products", icon: <GlobalOutlined />, label: "소싱 상품 조회" },
        { key: "/margin-policy", icon: <SettingOutlined />, label: "마진 정책 설정" },
        { key: "/sync-logs", icon: <HistoryOutlined />, label: "동기화 이력 로그" },
      ]
    },
    { type: 'divider' },
    { 
      key: "market", 
      label: "마켓 관리 (Market)", 
      type: 'group',
      children: [
        { key: "/products", icon: <ShopOutlined />, label: "마켓 상품 관리" },
        { key: "/orders", icon: <ShoppingCartOutlined />, label: "마켓 주문 관리" },
      ]
    }
  ];

  return (
    <Layout style={{ minHeight: "100vh", background: "#f3f4f6" }}>
      {/* 🚀 사이드바 (프리미엄 다크 모드) */}
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={(value) => setCollapsed(value)}
        width={260}
        theme="dark"
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
        }}
      >
        <div style={{ 
          height: "64px", 
          margin: "16px", 
          display: "flex", 
          alignItems: "center", 
          justifyContent: "center",
          background: "rgba(255, 255, 255, 0.05)",
          borderRadius: "12px",
          overflow: "hidden"
        }}>
          <CloudSyncOutlined style={{ fontSize: "28px", color: "#6366f1" }} />
          {!collapsed && (
            <Title level={4} style={{ color: "#fff", margin: "0 0 0 12px", whiteSpace: "nowrap" }} className="brand-font">
              AntiGravity Sync
            </Title>
          )}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          onClick={(e) => navigate(e.key)}
          items={menuItems}
        />
      </Sider>

      {/* 🚀 메인 컨텐츠 래퍼 (사이드바 폭만큼 여백 확보) */}
      <Layout style={{ marginLeft: collapsed ? 80 : 260, transition: 'all 0.2s' }}>
        
        {/* 상단 헤더 (글래스모피즘) */}
        <Header>
          <div>
            <Title level={4} style={{ margin: 0, color: "#1f2937" }} className="brand-font">
              {menuItems.flatMap(i => i.children || i).find(i => i.key === location.pathname)?.label || "대시보드"}
            </Title>
          </div>
          
          <Space size="large">
            <Badge count={5} size="small">
              <div style={{ 
                width: 40, height: 40, borderRadius: '50%', 
                background: '#f3f4f6', display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', transition: 'all 0.3s'
              }} className="hover:bg-gray-200">
                <BellOutlined style={{ fontSize: '18px', color: '#4b5563' }} />
              </div>
            </Badge>
            <Dropdown menu={{ items: [{ key: '1', label: '로그아웃' }] }} placement="bottomRight">
              <Space style={{ cursor: 'pointer', background: '#f9fafb', padding: '4px 12px', borderRadius: '24px', border: '1px solid #e5e7eb' }}>
                <Avatar icon={<UserOutlined />} style={{ background: 'var(--primary-gradient)' }} />
                <Text strong style={{ color: '#374151' }}>Admin User</Text>
              </Space>
            </Dropdown>
          </Space>
        </Header>

        {/* 🚀 메인 페이지 컨텐츠가 들어가는 곳 */}
        <Content style={{ margin: '24px 24px 0', overflow: 'initial' }}>
          <div className="page-container">
            <Outlet />
          </div>
        </Content>
        
        <div style={{ textAlign: 'center', padding: '24px', color: '#9ca3af', fontSize: '13px' }}>
          AntiGravity Sync Platform © {new Date().getFullYear()} Created by Jason & AI Crew
        </div>
      </Layout>
    </Layout>
  );
}
