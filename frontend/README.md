# Real Agent Console (Front-end)

- Vite + React + TypeScript + Ant Design
- 开发: `npm i` `npm run dev`
- 预设 API: `/api/tools` `/api/tools/execute` 等，见 `src/services/*`

## 目录
- `src/app` 路由与布局
- `src/pages` 页面
- `src/services` API SDK

## 环境变量
- `VITE_API_BASE_URL` 由 `vite.config.ts` 代理 `/api`

## 后端服务
本项目需要后端服务支持，特别是 `/api/agent/chat/react/stream` 接口。
请确保后端服务运行在 `http://localhost:8080` 或通过 `.env` 文件中的 `VITE_API_BASE_URL` 指定的地址。

启动后端服务后再启动前端开发服务器:
```bash
npm run dev
```
