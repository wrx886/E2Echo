import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { AUTH_FAILED_MESSAGE, RESULT_CODE_FAIL, RESULT_CODE_OK } from './types'
import type { Result } from './types'

/**
 * 接口调用失败时抛出的异常。
 *
 * <p>把后端统一响应 {@code Result} 的失败状态、认证失败以及 axios 的传输异常统一成同一种异常，
 * 调用方只需要捕获 {@link ApiError}。</p>
 */
export class ApiError extends Error {

  /** 后端返回的状态码，传输异常时为 {@link RESULT_CODE_FAIL}。 */
  readonly code: string

  /** 是否由会话失效导致（需要重新从客户端打开网页）。 */
  readonly authFailed: boolean

  constructor(message: string, code: string, authFailed = false) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.authFailed = authFailed
  }
}

/**
 * 判断异常是否为会话失效。
 *
 * @param error 待判断的异常
 * @returns 会话失效时返回 true
 */
export function isAuthFailed(error: unknown): boolean {
  return error instanceof ApiError && error.authFailed
}

/**
 * 认证失效的通知回调。
 *
 * <p>由应用启动时注册（弹出提示、引导用户回到客户端重新打开网页），模块内保证在会话恢复之前
 * 只触发一次，避免并发请求弹出多条相同提示。</p>
 */
type AuthFailedHandler = () => void

let authFailedHandler: AuthFailedHandler | null = null

/** 上一次请求之后是否已经通知过认证失效。 */
let authFailedNotified = false

/**
 * 注册认证失效回调。
 *
 * @param handler 回调，为空时取消注册
 */
export function setAuthFailedHandler(handler: AuthFailedHandler | null): void {
  authFailedHandler = handler
}

/**
 * 判断响应体是否为统一响应结构。
 *
 * @param value 响应体
 * @returns 是统一响应结构时返回 true
 */
function isResult(value: unknown): value is Result<unknown> {
  return typeof value === 'object' && value !== null
    && typeof (value as Result<unknown>).code === 'string'
}

/**
 * 通知认证失效。
 *
 * <p>同一次会话失效只通知一次：回调可能引导用户重新打开网页，重复弹出没有意义；请求重新成功
 * 后会重置标记，让下一次失效仍能通知。</p>
 */
function notifyAuthFailed(): void {
  if (authFailedNotified) {
    return
  }
  authFailedNotified = true
  authFailedHandler?.()
}

/**
 * 把后端返回的失败结果转换成异常。
 *
 * @param result 统一响应
 * @returns 对应的接口异常
 */
function toApiError(result: Result<unknown>): ApiError {
  const authFailed = result.message === AUTH_FAILED_MESSAGE
  if (authFailed) {
    notifyAuthFailed()
  }
  return new ApiError(result.message || '请求失败', authFailed ? RESULT_CODE_FAIL : result.code, authFailed)
}

/**
 * 把 axios 的传输异常转换成接口异常。
 *
 * @param error 原始异常
 * @returns 接口异常
 */
function toTransportError(error: AxiosError): ApiError {
  // 服务端返回了响应：优先按统一响应解析，业务失败与认证失败都由这里兜底
  if (error.response) {
    if (isResult(error.response.data)) {
      return toApiError(error.response.data)
    }
    return new ApiError('服务器状态异常！', String(error.response.status))
  }
  // 请求没有发出去：客户端进程已退出、网页被单独打开等情况
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return new ApiError('请求超时！', '-1')
  }
  return new ApiError('无法连接客户端，请确认客户端仍在运行！', '-1')
}

/**
 * 接口调用使用的 axios 实例。
 *
 * <p>baseURL 为 {@code '/'}：网页由 client 自己的容器提供，接口与页面同源，不需要也不允许跨域。</p>
 */
export const http = axios.create({
  baseURL: '/',
  timeout: 30 * 1000,
})

// 响应拦截：剥掉统一响应的外壳，成功直接把 data 交给调用方，失败一律抛 ApiError
http.interceptors.response.use(
  (response) => {
    if (!isResult(response.data)) {
      throw new ApiError('服务器状态异常！', String(response.status))
    }
    if (response.data.code !== RESULT_CODE_OK) {
      throw toApiError(response.data)
    }
    // 会话恢复，允许下一次失效时再次通知
    authFailedNotified = false
    return response.data.data as never
  },
  (error: AxiosError) => Promise.reject(toTransportError(error)),
)

/**
 * 发起请求并返回统一响应中的业务数据。
 *
 * <p>泛型只描述业务数据的类型：响应拦截器已经把统一响应的外壳剥掉，异常也统一成了
 * {@link ApiError}。</p>
 *
 * @param config 请求配置
 * @returns 业务数据
 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  // 响应拦截器已经把 Promise 的兑现值从 AxiosResponse 换成了业务数据，但 axios 的类型声明无法
  // 表达这件事（它仍然按 AxiosResponse 推断），所以这里用断言把类型对齐
  return http.request(config) as unknown as Promise<T>
}

/**
 * 发起 GET 请求。
 *
 * @param url    接口地址
 * @param params 查询参数
 * @returns 业务数据
 */
export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ method: 'GET', url, params })
}

/**
 * 发起 POST 请求。
 *
 * @param url  接口地址
 * @param data 请求体
 * @returns 业务数据
 */
export function post<T>(url: string, data?: unknown): Promise<T> {
  return request<T>({ method: 'POST', url, data })
}
