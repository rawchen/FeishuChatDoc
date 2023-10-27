package com.rawchen.feishuchatdoc.util;

public class MessageContent {

    public static String ofText(String text) {
        return String.format("{\"text\":\"%s\"}", text);
    }
}
