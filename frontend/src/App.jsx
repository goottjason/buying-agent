import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Card, Typography } from "antd";

// 우리가 쪼개놓은 레이아웃과 페이지 불러오기
import MainLayout from "./components/layout/MainLayout";
import ProductPage from "./pages/ProductPage";
import ProductRegisterPage from "./pages/ProductRegisterPage.jsx";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 모든 경로는 MainLayout의 껍데기를 덮어씁니다 */}
        <Route path="/" element={<MainLayout />}>
          {/* URL이 '/' 일 때 (홈) */}
          <Route
            index
            element={
              <Card>
                <Typography.Title level={4}>
                  대시보드 준비 중...
                </Typography.Title>
              </Card>
            }
          />

          {/* URL이 '/products' 일 때 (우리가 만든 상품관리 페이지) */}
          <Route path="products" element={<ProductPage />} />

          {/* URL이 '/orders' 일 때 */}
          <Route
            path="orders"
            element={
              <Card>
                <Typography.Title level={4}>
                  주문관리 준비 중...
                </Typography.Title>
              </Card>
            }
          />

          {/* URL이 '/register' 일 때 */}
          <Route path="register" element={<ProductRegisterPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
