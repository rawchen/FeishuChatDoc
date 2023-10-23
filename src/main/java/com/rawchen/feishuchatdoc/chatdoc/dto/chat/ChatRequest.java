package com.rawchen.feishuchatdoc.chatdoc.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ChatRequest
 *
 * @author RawChen
 * @version 2023/10/23 16:35
 **/
@Getter
@Setter
@Builder
public class ChatRequest {

    private List<String> fileIds;

    private List<ChatMessage> messages;

    private Integer topN;
}