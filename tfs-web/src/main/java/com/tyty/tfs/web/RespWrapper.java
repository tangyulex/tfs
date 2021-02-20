package com.tyty.tfs.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespWrapper<T> implements Serializable {

    public static final Integer SUCCESS = 1;
    public static final Integer FAIL = 0;
    public static final Integer UNLOGIN = -1;

    private Integer code;
    private String msg;

    private T data;

    public static <T> RespWrapper<T> success(T data) {
        return success(StringUtils.EMPTY, data);
    }

    public static <T> RespWrapper<T> success(String msg, T data) {
        return new RespWrapper<>(SUCCESS, msg, data);
    }

    public static <T> RespWrapper<T> fail(String msg) {
        return fail(msg, null);
    }

    public static <T> RespWrapper<T> fail(String msg, T data) {
        return new RespWrapper<>(FAIL, msg, data);
    }

}
