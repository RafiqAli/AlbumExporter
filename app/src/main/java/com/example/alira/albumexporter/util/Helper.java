package com.example.alira.albumexporter.util;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.facebook.AccessToken;


/*As shown by its name, this class contains static methods
 * that are relevant in the developpement of the app.
 */

public class Helper {


    /* This method takes the id of the photo and the type and returns
     * the full url to access the photo.
     *
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

    // this method test if the mobile phone is connected
    // to internet via WIFI or Cellular Data.
    public static boolean isConnected(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }





}
