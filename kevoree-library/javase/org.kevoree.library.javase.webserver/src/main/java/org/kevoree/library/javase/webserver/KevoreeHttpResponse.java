package org.kevoree.library.javase.webserver;

import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 14/10/11
 * Time: 08:41
 * To change this template use File | Settings | File Templates.
 */
public class KevoreeHttpResponse {

    private UUID tokenID = UUID.randomUUID();

    private String content = "";

    public UUID getTokenID() {
        return tokenID;
    }

    public void setTokenID(UUID tokenID) {
        this.tokenID = tokenID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}