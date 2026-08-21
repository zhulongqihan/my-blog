import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElAvatar from 'element-plus/es/components/avatar'
import ElBadge from 'element-plus/es/components/badge'
import { ElBreadcrumb, ElBreadcrumbItem } from 'element-plus/es/components/breadcrumb'
import ElButton from 'element-plus/es/components/button'
import ElCard from 'element-plus/es/components/card'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus/es/components/checkbox'
import ElCol from 'element-plus/es/components/col'
import ElColorPicker from 'element-plus/es/components/color-picker'
import { ElAside, ElContainer, ElHeader, ElMain } from 'element-plus/es/components/container'
import { ElDescriptions, ElDescriptionsItem } from 'element-plus/es/components/descriptions'
import ElDialog from 'element-plus/es/components/dialog'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown'
import ElEmpty from 'element-plus/es/components/empty'
import { ElForm, ElFormItem } from 'element-plus/es/components/form'
import ElIcon from 'element-plus/es/components/icon'
import ElImage from 'element-plus/es/components/image'
import ElInput from 'element-plus/es/components/input'
import ElInputNumber from 'element-plus/es/components/input-number'
import { ElMenu, ElMenuItem, ElMenuItemGroup } from 'element-plus/es/components/menu'
import { ElOption } from 'element-plus/es/components/select'
import ElPagination from 'element-plus/es/components/pagination'
import ElPopconfirm from 'element-plus/es/components/popconfirm'
import ElProgress from 'element-plus/es/components/progress'
import ElRow from 'element-plus/es/components/row'
import ElSelect from 'element-plus/es/components/select'
import ElSpace from 'element-plus/es/components/space'
import { ElStep, ElSteps } from 'element-plus/es/components/steps'
import ElSwitch from 'element-plus/es/components/switch'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table'
import ElTag from 'element-plus/es/components/tag'
import ElText from 'element-plus/es/components/text'
import ElUpload from 'element-plus/es/components/upload'
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

const adminComponents = [
  ElAside,
  ElAvatar,
  ElBadge,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCard,
  ElCheckbox,
  ElCheckboxGroup,
  ElCol,
  ElColorPicker,
  ElContainer,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElIcon,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElMenuItemGroup,
  ElOption,
  ElPagination,
  ElPopconfirm,
  ElProgress,
  ElRow,
  ElSelect,
  ElSpace,
  ElStep,
  ElSteps,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElText,
  ElUpload,
]

for (const component of adminComponents) {
  app.use(component)
}

app.mount('#app')
