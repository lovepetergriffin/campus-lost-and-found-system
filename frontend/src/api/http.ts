import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('lost-found-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data.data,
  (error) => {
    const message = error.response?.data?.message || '网络连接失败，请稍后重试'
    ElMessage.error(message)
    if (error.response?.status === 401) {
      localStorage.removeItem('lost-found-token')
      localStorage.removeItem('lost-found-user')
      if (location.pathname !== '/auth') location.href = `/auth?redirect=${encodeURIComponent(location.pathname)}`
    }
    return Promise.reject(error)
  },
)

export default http
