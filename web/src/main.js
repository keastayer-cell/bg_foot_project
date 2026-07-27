import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { installApiDebugLogger } from './utils/apiDebugLogger'
import './style.css'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const debugApiLogs = import.meta.env.VITE_DEBUG_API_LOGS === 'true'

installApiDebugLogger({ apiBaseUrl, enabled: debugApiLogs })

createApp(App).use(router).mount('#app')
