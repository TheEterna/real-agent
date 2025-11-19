import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 1500000
})

instance.interceptors.request.use(cfg => {
  return cfg
})

instance.interceptors.response.use(resp => resp.data, err => {
  return Promise.reject(err)
})

export default instance
