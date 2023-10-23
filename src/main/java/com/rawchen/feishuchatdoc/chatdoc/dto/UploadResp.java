package com.rawchen.feishuchatdoc.chatdoc.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * UploadResp
 *
 * @author RawChen
 * @version 2023/10/23 16:35
 */
@Getter
@Setter
public class UploadResp extends ResponseMsg {
    private Datas data;

    @Data
    public static class Datas {
        private String originPath;
        private String filePath;
        private String fileId;
    }
}
