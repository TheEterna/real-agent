package com.ai.agent.real.contract.model.protocol;

import java.io.Serializable;

/**
 * 响应结果类
 *
 * @author han
 * @time 2025/9/17 13:17
 */

public class ResponseResult<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 状态码：成功
	 */
	public static final int SUCCESS_CODE = 200;

	/**
	 * 状态码：失败
	 */
	public static final int ERROR_CODE = 500;

	/**
	 * 状态码：参数错误
	 */
	public static final int PARAM_ERROR_CODE = 400;

	/**
	 * 状态码：未授权
	 */
	public static final int UNAUTHORIZED_CODE = 401;

	/**
	 * 状态码：禁止访问
	 */
	public static final int FORBIDDEN_CODE = 403;

	/**
	 * 状态码：资源不存在
	 */
	public static final int NOT_FOUND_CODE = 404;

	/**
	 * 状态码
	 */
	private int code;

	/**
	 * 响应消息
	 */
	private String message;

	/**
	 * 响应数据
	 */
	private T data;

	/**
	 * 时间戳
	 */
	private long timestamp;

	/**
	 * 构造方法
	 */
	public ResponseResult() {
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 构造方法
	 * @param code 状态码
	 * @param message 响应消息
	 * @param data 响应数据
	 */
	public ResponseResult(int code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 创建成功响应（无数据）
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> success() {
		return new ResponseResult<>(SUCCESS_CODE, "操作成功", null);
	}

	/**
	 * 创建成功响应（带数据）
	 * @param data 响应数据
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> success(T data) {
		return new ResponseResult<>(SUCCESS_CODE, "操作成功", data);
	}

	/**
	 * 创建成功响应（带消息和数据）
	 * @param message 响应消息
	 * @param data 响应数据
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> success(String message, T data) {
		return new ResponseResult<>(SUCCESS_CODE, message, data);
	}

	/**
	 * 创建错误响应（默认消息）
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> error() {
		return new ResponseResult<>(ERROR_CODE, "操作失败", null);
	}

	/**
	 * 创建错误响应（带消息）
	 * @param message 错误消息
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> error(String message) {
		return new ResponseResult<>(ERROR_CODE, message, null);
	}

	/**
	 * 创建错误响应（带状态码和消息）
	 * @param code 状态码
	 * @param message 错误消息
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> error(int code, String message) {
		return new ResponseResult<>(code, message, null);
	}

	/**
	 * 创建参数错误响应
	 * @param message 错误消息
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> paramError(String message) {
		return new ResponseResult<>(PARAM_ERROR_CODE, message, null);
	}

	/**
	 * 创建未授权响应
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> unauthorized() {
		return new ResponseResult<>(UNAUTHORIZED_CODE, "未授权访问", null);
	}

	/**
	 * 创建资源不存在响应
	 * @return 响应结果对象
	 */
	public static <T> ResponseResult<T> notFound() {
		return new ResponseResult<>(NOT_FOUND_CODE, "资源不存在", null);
	}

	// getter和setter方法
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "ResponseResult{" + "code=" + code + ", message='" + message + '\'' + ", data=" + data + ", timestamp="
				+ timestamp + '}';
	}

}
