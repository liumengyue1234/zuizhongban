import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// ======================== 用户相关API ========================
// 后端路径: /api/user/*
export const userApi = {
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data),
  getProfile: () => request.get('/user/profile'),
  getUserInfo: () => request.get('/user/profile'),                  // 别名，兼容
  updateProfile: (data) => request.put('/user/profile', data),
  updateUserInfo: (data) => request.put('/user/profile', data),     // 别名，兼容
  changePassword: (data) => request.put('/user/password', data),
  list: (params) => request.get('/user/list', { params }),
  getUserList: (params) => request.get('/user/list', { params }),   // 别名，兼容
  delete: (id) => request.delete(`/user/${id}`),
  deleteUser: (id) => request.delete(`/user/${id}`),               // 别名，兼容
  update: (data) => request.put(`/user`, data),
  updateUser: (id, data) => request.put(`/user`, { ...data, id }),  // 别名，兼容
  create: (data) => request.post('/user', data),
  updateStatus: (id, status) => request.put(`/user/${id}/status/${status}`),
}

// ======================== CT影像相关API ========================
// 后端路径: /api/images/*
export const imageApi = {
  upload: (formData) => request.post('/images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  uploadImage: (formData) => request.post('/images/upload', formData, {  // 别名，兼容
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  list: (params) => request.get('/images/list', { params }),
  getImageList: (params) => request.get('/images/list', { params }),       // 别名，兼容
  delete: (id) => request.delete(`/images/${id}`),
  deleteImage: (id) => request.delete(`/images/${id}`),                  // 别名，兼容
  detail: (id) => request.get(`/images/view/${id}`),
  getImageDetail: (id) => request.get(`/images/view/${id}`),            // 别名，兼容
  detect: (id, modelName, denoiseMethod) => request.post(`/images/${id}/detect`, null, {
    params: { modelName: modelName || 'Improved U-Net', denoiseMethod: denoiseMethod || 'none' }
  }),
}

// ======================== 检测记录相关API ========================
// 后端路径: /api/detection/*
export const detectionApi = {
  startDetection: (data) => request.post(`/images/${data.imageId}/detect`, null, {
    params: { modelName: data.modelName || 'Improved U-Net', denoiseMethod: data.denoiseMethod || 'none' }
  }),
  getDetectionResult: (id) => request.get(`/detection/${id}`),
  getHistory: (params) => request.get('/detection/history', { params }),
  getDetectionHistory: (params) => request.get('/detection/history', { params }), // 别名，兼容
  getDetail: (id) => request.get(`/detection/${id}`),
  getDetectionDetail: (id) => request.get(`/detection/${id}`),                   // 别名，兼容
  deleteRecord: (id) => request.delete(`/detection/${id}`),
  batchDelete: (ids) => request.delete('/detection/batch', { data: ids }),
}

// ======================== 报告相关API ========================
// 后端路径: /api/report/*
export const reportApi = {
  generate: (data) => request.post('/report/generate', data),
  generateReport: (detectionId) => request.post('/report/generate', { detectionId }), // 别名，兼容
  list: (params) => request.get('/report/list', { params }),
  getReportList: (params) => request.get('/report/list', { params }),   // 别名，兼容
  getDetail: (id) => request.get(`/report/${id}`),
  getReportDetail: (id) => request.get(`/report/${id}`),                // 别名，兼容
  exportPDF: (id) => request.get(`/report/${id}/export`),
  downloadReport: (id) => request.get(`/report/${id}/export`),          // 别名，兼容
  delete: (id) => request.delete(`/report/${id}`),
  getDetectionList: () => request.get('/detection/history', { params: { pageNum: 1, pageSize: 100 } }), // 获取检测记录用于生成报告
}

// ======================== 模型对比API ========================
// 后端路径: /api/model/*
export const modelApi = {
  compare: (data) => request.post('/model/compare', data),
  getModelComparison: (models) => request.post('/model/compare', { models }), // 别名，兼容
  getModelList: () => request.get('/model/list'),
  getModelDetail: (modelName) => request.get('/model/list'),                  // 暂用list代替
}

export default request
