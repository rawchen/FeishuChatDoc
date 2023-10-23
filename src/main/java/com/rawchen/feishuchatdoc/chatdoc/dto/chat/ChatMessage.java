package com.rawchen.feishuchatdoc.chatdoc.dto.chat;

import lombok.Getter;
import lombok.Setter;

/**
 * ChatMessage
 *
 * @author RawChen
 * @version 2023/10/23 16:35
 **/
@Getter
@Setter
public class ChatMessage {

    private String role;

    private String content;
}