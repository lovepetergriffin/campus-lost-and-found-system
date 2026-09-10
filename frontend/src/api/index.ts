import http from './http'
import type { Category, Claim, Item, ItemPayload, Notification, PageResult, Statistics, User } from '@/types'

export const authApi = {
  register: (payload: { username: string; password: string; contact?: string }) => http.post<never, User>('/auth/register', payload),
  login: (payload: { username: string; password: string }) => http.post<never, { token: string; user: User }>('/auth/login', payload),
  me: () => http.get<never, User>('/auth/me'),
}

export const categoryApi = {
  list: () => http.get<never, Category[]>('/categories'),
  create: (payload: { name: string; sortOrder: number }) => http.post<never, Category>('/categories', payload),
  update: (id: number, payload: { name: string; sortOrder: number }) => http.put<never, Category>(`/categories/${id}`, payload),
  remove: (id: number) => http.delete(`/categories/${id}`),
}

export const itemApi = {
  list: (params: Record<string, unknown>) => http.get<never, PageResult<Item>>('/items', { params }),
  detail: (id: number) => http.get<never, Item>(`/items/${id}`),
  create: (payload: ItemPayload) => http.post<never, Item>('/items', payload),
  update: (id: number, payload: ItemPayload) => http.put<never, Item>(`/items/${id}`, payload),
  remove: (id: number) => http.delete(`/items/${id}`),
  close: (id: number) => http.patch(`/items/${id}/close`),
  mine: () => http.get<never, Item[]>('/items/mine'),
}

export const claimApi = {
  create: (itemId: number, payload: { description: string; proof: string }) => http.post<never, Claim>(`/claims/items/${itemId}`, payload),
  mine: () => http.get<never, Claim[]>('/claims/mine'),
  received: () => http.get<never, Claim[]>('/claims/received'),
  decide: (id: number, result: 'APPROVED' | 'REJECTED') => http.patch<never, Claim>(`/claims/${id}/decision`, { result }),
}

export const notificationApi = {
  list: () => http.get<never, Notification[]>('/notifications'),
  unreadCount: () => http.get<never, { count: number }>('/notifications/unread-count'),
  read: (id: number) => http.patch(`/notifications/${id}/read`),
  readAll: () => http.patch('/notifications/read-all'),
}

export const adminApi = {
  pendingItems: () => http.get<never, Item[]>('/admin/items/pending'),
  review: (id: number, payload: { result: 'APPROVED' | 'REJECTED'; comment: string }) => http.post<never, Item>(`/admin/items/${id}/review`, payload),
  statistics: () => http.get<never, Statistics>('/admin/statistics'),
}
