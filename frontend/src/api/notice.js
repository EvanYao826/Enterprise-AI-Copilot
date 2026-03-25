import request from './index'
import adminRequest from './admin'

export const getNoticeList = (params) => {
  return adminRequest.get('/notice/list', { params })
}

export const addNotice = (data) => {
  return adminRequest.post('/notice/add', data)
}

export const updateNotice = (data) => {
  return adminRequest.put('/notice/update', data)
}

export const deleteNotice = (id) => {
  return adminRequest.delete(`/notice/delete/${id}`)
}

export const getLatestNotices = () => {
  return request.get('/notice/latest')
}
