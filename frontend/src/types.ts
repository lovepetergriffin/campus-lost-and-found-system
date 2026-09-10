export interface User {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
  contact?: string
}

export interface Category {
  id: number
  name: string
  sortOrder: number
}

export type ItemType = 'LOST' | 'FOUND'
export type ItemStatus = 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'CLAIMED' | 'CLOSED'

export interface Item {
  id: number
  name: string
  type: ItemType
  categoryId: number
  categoryName: string
  location: string
  eventTime: string
  description: string
  imageUrl?: string
  contact?: string
  status: ItemStatus
  publisherId: number
  publisherName: string
  createdAt: string
  updatedAt: string
}

export interface ItemPayload {
  name: string
  type: ItemType
  categoryId: number | undefined
  location: string
  eventTime: string
  description: string
  imageUrl?: string
  contact?: string
}

export interface Claim {
  id: number
  itemId: number
  itemName: string
  applicantId: number
  applicantName: string
  description: string
  proof: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  processedAt?: string
}

export interface Notification {
  id: number
  receiverId: number
  type: string
  content: string
  isRead: boolean
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface Statistics {
  userCount: number
  itemCount: number
  publishedCount: number
  pendingCount: number
  claimedCount: number
  claimCount: number
  claimRate: number
  lostCount: number
  foundCount: number
}
