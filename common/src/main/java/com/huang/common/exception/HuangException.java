package com.huang.common.exception;

import com.huang.common.result.ResultCodeEnum;
import lombok.Data;

@Data
public class HuangException extends RuntimeException {

    private Integer code;

    public HuangException(Integer code, String message) {
        super(message);
        this.code = code;
    }


    public HuangException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }
}
