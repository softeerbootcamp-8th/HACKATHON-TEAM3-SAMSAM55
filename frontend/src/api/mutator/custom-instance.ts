import axios, { type AxiosRequestConfig } from 'axios'

export const AXIOS_INSTANCE = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: true,
})

export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
  const source = axios.CancelToken.source()

  const promise = AXIOS_INSTANCE({ ...config, cancelToken: source.token }).then(
    ({ data }) => data,
  ) as Promise<T> & { cancel: () => void }

  promise.cancel = () => {
    source.cancel('Query was cancelled')
  }

  return promise
}

export default customInstance
