package me.xjanua.aitranswatch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RestResponse<T> {
    private final boolean success;
    private final T data;

    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(true, data);
    }
}
