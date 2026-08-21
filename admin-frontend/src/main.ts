import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import {
  ArrowDown,
  Bell,
  Box,
  ChatDotRound,
  Check,
  Coin,
  Connection,
  DataAnalysis,
  Delete,
  Document,
  Expand,
  Folder,
  Fold,
  HomeFilled,
  Edit,
  Key,
  Lock,
  Message,
  Monitor,
  Odometer,
  Plus,
  PriceTag,
  Promotion,
  Refresh,
  Search,
  Switch,
  SwitchButton,
  Tickets,
  TrendCharts,
  Unlock,
  User,
  UserFilled,
  Upload,
  View,
  Warning,
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 只注册模板实际使用的图标，避免把整套图标组件带入后台构建依赖图。
const adminIcons = {
  ArrowDown,
  Bell,
  Box,
  ChatDotRound,
  Check,
  Coin,
  Connection,
  DataAnalysis,
  Delete,
  Document,
  Expand,
  Folder,
  Fold,
  HomeFilled,
  Edit,
  Key,
  Lock,
  Message,
  Monitor,
  Odometer,
  Plus,
  PriceTag,
  Promotion,
  Refresh,
  Search,
  Switch,
  SwitchButton,
  Tickets,
  TrendCharts,
  Unlock,
  User,
  UserFilled,
  Upload,
  View,
  Warning,
}

for (const [key, component] of Object.entries(adminIcons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
