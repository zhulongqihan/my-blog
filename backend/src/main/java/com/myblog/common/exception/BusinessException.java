package com.myblog.common.exception;

import com.myblog.common.constant.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * 设计目标：统一业务异常处理，避免在Controller层写大量if-else
 * 
 * 使用场景：
 * - 参数校验失败
 * - 业务规则不满足
 * - 资源不存在
 * 
 * 示例：
 * if (article == null) {
 *     throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
 * }
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final Integer code;
    
    /**
     * 错误信息
     */
    private final String message;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }
}
