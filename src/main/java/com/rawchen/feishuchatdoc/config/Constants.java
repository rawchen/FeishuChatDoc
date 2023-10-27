package com.rawchen.feishuchatdoc.config;

import lombok.Data;

/**
 * @author RawChen
 * @date 2023-10-23 23:44
 */
@Data
public class Constants {

    // 文档上传
    public static final String FILE_UPLOAD_URL = "https://chatdoc.xfyun.cn/openapi/fileUpload";

    // 文档问答
    public static final String CHAT_URL = "wss://chatdoc.xfyun.cn/openapi/chat";

    // 发起文档总结
    public static final String START_SUMMARY_URL = "https://chatdoc.xfyun.cn/openapi/startSummary";

    // 获取文档总结/概要内容
    public static final String FILE_SUMMARY_URL = "https://chatdoc.xfyun.cn/openapi/fileSummary";

}
