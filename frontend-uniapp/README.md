# Fitness Platform UniApp

用户端手机前端基础工程，首期面向 H5 手机浏览器与微信小程序。

## 启动

```bash
npm install
npm run dev:h5
```

默认 H5 地址：`http://localhost:5175`。开发代理：`/app -> http://127.0.0.1:8081`。

## 构建

```bash
npm run build:h5
npm run dev:mp-weixin
npm run build:mp-weixin
```

微信小程序本地调试时，如使用本机后端接口，需要在微信开发者工具里关闭域名校验；正式环境请通过 `VITE_APP_BASE_URL` 配置 HTTPS API 域名。
