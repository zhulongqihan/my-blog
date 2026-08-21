---
name: My-blog Design System
description: A calm engineering archive with a small, playful pixel-sheep signature.
colors:
  primary: '#8b7355'
  primary-light: '#a68b6a'
  secondary: '#5c6b4a'
  secondary-light: '#7a8b68'
  background: '#f8f8f8'
  surface: '#f0ede8'
  surface-strong: '#e8e4dc'
  ink: '#2d2d2d'
  ink-muted: '#5c5c5c'
  ink-subtle: '#6f6a62'
  border: '#e0dcd4'
  border-subtle: '#ebe8e2'
typography:
  display: "'Playfair Display', Georgia, serif"
  body: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
  mono: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace"
rounded:
  small: 4px
  medium: 10px
  large: 12px
  pill: 999px
spacing:
  xs: 0.25rem
  sm: 0.5rem
  md: 1rem
  lg: 1.5rem
  xl: 2rem
  2xl: 3rem
  3xl: 4rem
---

# My-blog Design System

## Overview

**Creative North Star:「工程现场的安静入口」**

页面像一个晚上仍亮着灯的工程工作台：信息可靠、路径清楚，边角留有一点像素羊带来的轻松感。视觉不追求把每个功能都展示出来，而是让文章、系统关系和真实证据先占据注意力。

**Key Characteristics:**

- 大地色和低饱和表面承载长期阅读，深色主题保持同样的克制。
- Playfair Display 负责作者感和标题层级，Inter 负责长文和控件，等宽字体只用于系统语气。
- 平面优先，边框和短阴影只在状态或层级需要时出现。
- 像素羊是签名组件，不是主导航；它可以被发现，但不应遮挡文章。

**The Reading-First Rule.** 每个页面首先回答“下一步读什么或做什么”，再展示低频工具。

**The Evidence-Over-Decoration Rule.** 真实标题、摘要、日期、系统链路和可验证状态优先于装饰性数字和重复卡片。

## Colors

主色沿用现有 rust/olive 体系。Rust 用于链接、主行动和阅读进度；olive 用于成功、稳定和系统状态；中性表面负责长时间阅读。颜色语义必须跨前台和后台一致，但后台可以使用 Element Plus 的状态组件承载操作反馈。

**The Muted-But-Readable Rule.** 辅助文字必须保持可读，不以低对比度换取“安静”。浅色主题正文和占位符按 WCAG AA 校验，深色主题不使用纯黑背景上的灰色小字。

## Typography

标题使用 Playfair Display，正文使用 Inter，系统状态和代码使用 JetBrains Mono。正文行宽控制在 65–75ch；标题使用 `text-wrap: balance`，长文章使用 `text-wrap: pretty`。标题不使用过紧的负字距，移动端优先保证中文断行自然。

## Elevation

平面优先。默认容器使用表面色或细边框，不同时叠加宽阴影；阴影只表示悬浮、弹层或明确的交互反馈。浮层使用统一的语义 z-index 层级，不使用任意的 9999。

## Components

### Buttons

主按钮使用 rust 色或透明边框变体，圆角保持 4–12px；标签和紧凑操作可以使用 pill。所有按钮必须有 hover、focus-visible、disabled、loading 状态，触控场景保证至少 44px 高度。

### Cards / Containers

文章卡片优先使用列表和留白表达层级，只有精选文章、系统桥接和后台模块需要容器。容器使用 10–12px 圆角，不嵌套同质卡片，不用渐变和装饰性侧条。

### Navigation

前台导航以首页、系统、归档、统计、书签、关于和搜索为主；移动端必须提供可见搜索入口。后台导航按内容、互动、系统和 AI 可观测性分组，折叠后仍保留图标语义和 tooltip。

### Pixel Sheep

像素羊是签名组件。它常驻普通阅读路径，但面板默认收起，展开面板必须避开正文和固定工具。点击、拖动、右键、键盘和减少动效状态都必须可理解、可恢复。

## Do's and Don'ts

### Do:

- Do let the first meaningful action lead to a real article.
- Do use real article metadata and system evidence to create hierarchy.
- Do keep the pixel sheep visible as a quiet, discoverable signature.
- Do use clear Chinese copy for loading, error, empty and recovery states.
- Do keep front-end personality and admin efficiency distinct but semantically aligned.

### Don't:

- Don't build a generic SaaS dashboard as the public homepage.
- Don't use gradient text, decorative grid backgrounds, glassmorphism or repeated numbered sections.
- Don't let the pixel sheep, AI lab, achievements and random features compete with reading.
- Don't use fake metrics or unverified AI retrieval/performance claims.
- Don't hide focus, animate layout properties casually, or rely on color alone for meaning.
