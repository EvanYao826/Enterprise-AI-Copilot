import adminRequest from './admin'

export const getDashboardStats = () => {
  return adminRequest.get('/dashboard/stat')
}
