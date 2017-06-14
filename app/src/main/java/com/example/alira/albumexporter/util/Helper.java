package com.example.alira.albumexporter.util;

import com.facebook.AccessToken;

/**
 * Created by alira on 6/14/2017.
 */

public class Helper {


    /*
     * @param String photo_id : the facebook id of the picture
     * @Returns String : the facebook url of the given picture
     */
    public static String getUrlByImageId(String photo_id,String photo_type)
    {
        String protocol = "https://";
        String host = "graph.facebook.com/v2.9/";
        String access_token = AccessToken.getCurrentAccessToken().getToken();
        String suffix = "/picture";

        return protocol + host + photo_id + suffix + "?" + "access_token=" + access_token + "&" + "type=" + photo_type;

    }
}
