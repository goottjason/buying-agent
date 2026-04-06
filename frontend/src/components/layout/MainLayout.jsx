import React from "react";
import { Layout, Menu, Typography, Space } from "antd";
import { ShopOutlined } from "@ant-design/icons";
import { useNavigate, useLocation, Outlet } from "react-router-dom";

const { Header, Content } = Layout;
const { Title } = Typography;

export default function MainLayout() {
  const navigate = useNavigate(); // 🚀 URL 이동을 도와주는 훅
  const location = useLocation(); // 🚀 현재 URL이 뭔지 알려주는 훅

  // URL 주소를 key로 사용합니다.
  const menuItems = [
    { key: "/", label: "홈 (대시보드)" },
    { key: "/products", label: "상품관리" },
    { key: "/orders", label: "주문관리" },
    { key: "/register", label: "신규등록" },
  ];

  return (
    <Layout style={{ minHeight: "100vh", background: "#f0f2f5" }}>
      {/* 상단 헤더 */}
      <Header
        style={{
          display: "flex",
          alignItems: "center",
          background: "#001529",
          padding: "0 20px",
          height: "50px",
        }}
      >
        <Space style={{ marginRight: "40px" }}>
          <ShopOutlined style={{ fontSize: "20px", color: "#fff" }} />
          <Title level={5} style={{ color: "#fff", margin: 0 }}>
            Buying Agent
          </Title>
        </Space>

        {/* 메뉴 클릭 시 해당 URL로 이동(navigate) 하도록 설정 */}
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          onClick={(e) => navigate(e.key)}
          items={menuItems}
          style={{ flex: 1, lineHeight: "50px", background: "transparent" }}
        />
      </Header>

      {/* 🚀 메인 컨텐츠 영역 (이 곳에 페이지들이 렌더링됩니다!) */}
      <Content style={{ padding: "24px 30px" }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
