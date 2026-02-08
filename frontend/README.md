# 个人博客 - 前端项目

基于 React 18 + TypeScript 5 + Vite 5 构建的现代化博客前端。

---

## 🛠️ 技术栈

- **框架**: React 18
- **语言**: TypeScript 5
- **构建工具**: Vite 5
- **路由**: React Router 6
- **HTTP 客户端**: Axios
- **动画**: Framer Motion
- **图标**: Lucide React
- **代码高亮**: React Syntax Highlighter

---

## 📁 项目结构

```
frontend/src/
├── components/          # 可复用组件
│   ├── Header.tsx      # 导航栏
│   ├── Footer.tsx      # 页脚
│   └── ArticleCard.tsx # 文章卡片
├── pages/              # 页面组件
│   ├── HomePage.tsx    # 首页
│   ├── ArticlePage.tsx # 文章详情页
│   ├── AboutPage.tsx   # 关于页
│   └── ArchivePage.tsx # 归档页
├── services/           # API 服务
│   ├── api.ts         # Axios 实例
│   └── index.ts       # API 方法
├── hooks/             # 自定义 Hooks
│   ├── useArticles.ts # 文章数据
│   ├── useCategories.ts # 分类数据
│   └── useAuth.ts     # 认证状态
├── contexts/          # React Context
│   └── AuthContext.tsx # 认证上下文
├── types/             # TypeScript 类型
│   └── index.ts       # 类型定义
├── App.tsx            # 根组件
├── App.css            # 全局样式
├── index.css          # 基础样式
└── main.tsx           # 入口文件
```

---

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问: http://localhost:5173

### 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录。

### 预览生产版本

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

---

## 🎨 设计规范

### 颜色系统

```css
--color-background: #F8F8F8;  /* 温暖的米白色 */
--color-text: #2D2D2D;        /* 炭灰色 */
--color-accent-rust: #8B7355; /* 铁锈色 */
--color-accent-olive: #5C6B4A; /* 橄榄绿 */
```

### 字体系统

- **标题**: Playfair Display (衬线体)
- **正文**: Inter (无衬线体)
- **代码**: JetBrains Mono (等宽字体)

### 设计风格

- 极简主义
- 大地色系
- 复古温暖
- 移动端优先

---

## 📝 开发规范

### 组件规范

- ✅ 使用函数式组件
- ✅ 使用 TypeScript 类型定义
- ✅ 使用 CSS 变量而非硬编码
- ✅ 移动端优先的响应式设计

### 命名规范

- 组件文件: `PascalCase.tsx`
- 样式文件: `PascalCase.css`
- 工具函数: `camelCase.ts`
- 常量: `UPPER_SNAKE_CASE`

### 代码风格

项目使用 Prettier 和 ESLint 统一代码风格。

格式化代码:
```bash
npx prettier --write "src/**/*.{ts,tsx,css}"
```

---

## 🔧 环境变量

### 开发环境 (`.env.development`)

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=个人博客 - 开发环境
VITE_DEBUG=true
```

### 生产环境 (`.env.production`)

```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_APP_TITLE=个人博客
VITE_DEBUG=false
```

---

## 📦 主要依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| react | ^19.2.0 | UI 框架 |
| react-router-dom | ^7.13.0 | 路由管理 |
| axios | ^1.13.3 | HTTP 客户端 |
| framer-motion | ^12.29.2 | 动画库 |
| lucide-react | ^0.563.0 | 图标库 |
| react-syntax-highlighter | ^16.1.0 | 代码高亮 |

---

## 🐛 常见问题

### 1. 端口被占用

修改 `vite.config.ts`:
```typescript
export default defineConfig({
  server: {
    port: 3000, // 改为其他端口
  },
})
```

### 2. API 连接失败

检查 `.env` 文件中的 `VITE_API_BASE_URL` 是否正确。

### 3. 样式不生效

确保导入了对应的 CSS 文件。

---

## 📚 相关文档

- [项目主文档](../README.md)
- [运行指南](../RUNNING_GUIDE.md)
- [开发规范](../.cursorrules)
- [后续计划](../NEXT_STEPS.md)

---

## 🎯 下一步

查看 [NEXT_STEPS.md](../NEXT_STEPS.md) 了解后续开发计划。

---

**祝开发愉快！** 🎉
