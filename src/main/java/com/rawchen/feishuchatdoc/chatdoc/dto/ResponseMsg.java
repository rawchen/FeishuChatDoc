package com.rawchen.feishuchatdoc.chatdoc.dto;

import lombok.Data;

/**
 * ResponseMsg
 *
 * @author RawChen
 * @version 2023/10/23 16:35
 **/
@Data
public class ResponseMsg {
    private boolean flag;
    private int code;
    private String desc;
    private String sid;

    public boolean success() {
        return code == 0;
    }
}