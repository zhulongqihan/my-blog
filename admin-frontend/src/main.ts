import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElAvatar from 'element-plus/es/components/avatar/index'
import ElBadge from 'element-plus/es/components/badge/index'
import { ElBreadcrumb, ElBreadcrumbItem } from 'element-plus/es/components/breadcrumb/index'
import ElButton from 'element-plus/es/components/button/index'
import ElCard from 'element-plus/es/components/card/index'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus/es/components/checkbox/index'
import ElCol from 'element-plus/es/components/col/index'
import ElColorPicker from 'element-plus/es/components/color-picker/index'
import { ElAside, ElContainer, ElHeader, ElMain } from 'element-plus/es/components/container/index'
import { ElDescriptions, ElDescriptionsItem } from 'element-plus/es/components/descriptions/index'
import ElDialog from 'element-plus/es/components/dialog/index'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index'
import ElEmpty from 'element-plus/es/components/empty/index'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index'
import ElIcon from 'element-plus/es/components/icon/index'
import ElImage from 'element-plus/es/components/image/index'
import ElInput from 'element-plus/es/components/input/index'
import ElInputNumber from 'element-plus/es/components/input-number/index'
import ElLoading from 'element-plus/es/components/loading/index'
import { ElMenu, ElMenuItem, ElMenuItemGroup } from 'element-plus/es/components/menu/index'
import { ElOption } from 'element-plus/es/components/select/index'
import ElPagination from 'element-plus/es/components/pagination/index'
import ElPopconfirm from 'element-plus/es/components/popconfirm/index'
import ElProgress from 'element-plus/es/components/progress/index'
import ElRow from 'element-plus/es/components/row/index'
import ElSelect from 'element-plus/es/components/select/index'
import ElSpace from 'element-plus/es/components/space/index'
import { ElStep, ElSteps } from 'element-plus/es/components/steps/index'
import ElSwitch from 'element-plus/es/components/switch/index'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index'
import ElTag from 'element-plus/es/components/tag/index'
import ElText from 'element-plus/es/components/text/index'
import ElUpload from 'element-plus/es/components/upload/index'
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
app.use(ElLoading)

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
