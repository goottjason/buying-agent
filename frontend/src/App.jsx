import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Card, Typography } from "antd";

// 레이아웃
import MainLayout from "./components/layout/MainLayout";

// 기존 페이지
import ProductPage from "./pages/ProductPage";
import ProductRegisterPage from "./pages/ProductRegisterPage.jsx";

// 신규 페이지 (Phase 1)
import SourcingProductPage from "./pages/SourcingProductPage";
import MarginPolicyPage from "./pages/MarginPolicyPage";
import SyncLogPage from "./pages/SyncLogPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          {/* 홈 (대시보드) */}
          <Route
            index
            element={
              <div className="glass-panel" style={{ padding: '24px' }}>
                <Typography.Title level={3} className="gradient-text">AntiGravity Sync Dashboard</Typography.Title>
                <Typography.Paragraph type="secondary">
                  글로벌 구매대행 마켓 연동 및 다중 소싱처 상태를 한눈에 모니터링합니다.
                </Typography.Paragraph>
              </div>
            }
          />

          {/* 소싱 관리 (Sourcing) */}
          <Route path="sourcing-products" element={<SourcingProductPage />} />
          <Route path="margin-policy" element={<MarginPolicyPage />} />
          <Route path="sync-logs" element={<SyncLogPage />} />

          {/* 마켓 관리 (Market) */}
          <Route path="products" element={<ProductPage />} />
          <Route path="register" element={<ProductRegisterPage />} />
          <Route
            path="orders"
            element={
              <div className="glass-panel" style={{ padding: '24px' }}>
                <Typography.Title level={3}>마켓 주문 관리</Typography.Title>
                <Typography.Paragraph type="secondary">
                  준비 중입니다.
                </Typography.Paragraph>
              </div>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
