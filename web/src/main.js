import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const originalFetch = window.fetch.bind(window)

function maskSensitive(value) {
	if (!value || typeof value !== 'object') return value

	if (Array.isArray(value)) {
		return value.map((item) => maskSensitive(item))
	}

	const masked = {}
	for (const [key, fieldValue] of Object.entries(value)) {
		const isSecretField = /password|token|secret|authorization/i.test(key)
		masked[key] = isSecretField ? '***' : maskSensitive(fieldValue)
	}
	return masked
}

function parseBody(body) {
	if (!body) return null
	if (typeof body !== 'string') return '[non-string body]'

	try {
		return maskSensitive(JSON.parse(body))
	} catch {
		return body
	}
}

function isApiRequest(url) {
	try {
		const resolvedUrl = new URL(url, window.location.origin)
		return resolvedUrl.href.startsWith(apiBaseUrl) || resolvedUrl.pathname.startsWith('/api/')
	} catch {
		return false
	}
}

window.fetch = async (input, init = {}) => {
	const requestUrl = typeof input === 'string' ? input : input?.url || ''
	if (!isApiRequest(requestUrl)) {
		return originalFetch(input, init)
	}

	const method = (init.method || 'GET').toUpperCase()
	const startedAt = performance.now()
	const requestBody = parseBody(init.body)
	const requestHeaders = init.headers || {}

	console.groupCollapsed(`[API] ${method} ${requestUrl}`)
	console.log('Запрос:', {
		method,
		url: requestUrl,
		headers: requestHeaders,
		body: requestBody,
	})

	try {
		const response = await originalFetch(input, init)
		const durationMs = Math.round(performance.now() - startedAt)

		let responseBody = null
		const contentType = response.headers.get('content-type') || ''
		const clonedResponse = response.clone()

		if (contentType.includes('application/json')) {
			responseBody = maskSensitive(await clonedResponse.json().catch(() => null))
		} else {
			const textBody = await clonedResponse.text().catch(() => '')
			responseBody = textBody ? textBody.slice(0, 300) : null
		}

		console.log('Ответ:', {
			status: response.status,
			ok: response.ok,
			durationMs,
			body: responseBody,
		})
		console.groupEnd()
		return response
	} catch (error) {
		console.error('Ошибка запроса:', error)
		console.groupEnd()
		throw error
	}
}

createApp(App).use(router).mount('#app')
