package com.rawchen.feishuchatdoc.chatdoc.dto.chat;

import lombok.Data;

/**
 * @author shuangquan.chen
 * @date 2023-10-26 13:39
 */
@Data
public class ChatExtend {

    /**
     * 用户问题未匹配到文档内容时，是否使用大模型兜底回答问题
     */
    boolean sparkWhenWithoutEmbedding;

    /**
     * 大模型问答时的温度，取值 0-1，temperature 越大，大模型回答随机度越高
     */
    Float temperature;

}
