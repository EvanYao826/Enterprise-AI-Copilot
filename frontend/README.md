# AI Knowledge System - Frontend

这是一个基于 React + Vite 的 AI 知识库系统前端。

## 技术栈

*   **React 18**
*   **Vite**
*   **React Router DOM**
*   **Axios**
*   **Ant Design** (假设使用了，根据常见 Vue/React 项目推测，或者类似的 UI 库，如果没有特定 UI 库，则为纯 CSS)

## 目录结构

*   `src/api/`: API 接口封装
*   `src/pages/`: 页面组件
*   `src/App.jsx`: 根组件
*   `vite.config.js`: Vite 配置 (包含代理设置)

## 运行步骤

1.  **安装依赖**:
    ```bash
    npm install
    ```

2.  **启动开发服务器**:
    ```bash
    npm run dev
    ```
    前端服务将在 `http://localhost:5173` (默认) 启动。

## 配置

在 `vite.config.js` 中配置了代理，将 `/api` 开头的请求转发到 Java 后端 `http://localhost:8080`。
