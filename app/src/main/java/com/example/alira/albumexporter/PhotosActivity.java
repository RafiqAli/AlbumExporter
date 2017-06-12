package com.example.alira.albumexporter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.alira.albumexporter.models.Album;
import com.example.alira.albumexporter.models.Photo;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PhotosActivity extends AppCompatActivity {

    List<Photo> photosList = new ArrayList<>();
    ProgressDialog progress;
    GridView photosGrid;
    String after;
    PhotosGridAdapter adapter = new PhotosGridAdapter(this,photosList);
    String album_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        photosGrid = (GridView) findViewById(R.id.photos_grid);

        photosGrid.setAdapter(adapter);

        Intent intent = getIntent();
        album_id   = intent.getExtras().getString("album_id");
        String album_name = intent.getExtras().getString("album_name");

        Log.i("album-id-null",album_id);

        setTitle(album_name);

        new PhotosIdsFetcher().execute(album_id);

    }



    public String getImageById(String photo_id)
    {
        Bitmap decodedImage = null;
        String id = photo_id; // picture id
        String protocol = "https://";
        String host = "graph.facebook.com/v2.9/";
        String access_token = AccessToken.getCurrentAccessToken().getToken();
        String suffix = "/picture";
        String type = "normal";
        String Url = protocol + host + id + suffix + "?" + "access_token=" + access_token + "&" + "type=" + type;


        return Url;
    }

    public void loadMoreContent()
    {

        Bundle params = new Bundle();

        params.putInt("limit",25);
        params.putString("after",after);

        String path = album_id+"/photos";

        Log.i("path-loadmore",path);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                path,
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {

                        JSONObject jsonObject = response.getJSONObject();
                        Log.i("response",response.getJSONObject().toString());

                        try {

                            after = jsonObject.getJSONObject("paging").getJSONObject("cursors").getString("after");

                            ;

                            JSONArray fetchedPhotosIds = jsonObject.getJSONArray("data");

                            for (int i = 0; i < fetchedPhotosIds.length(); i++) {
                                JSONObject item = fetchedPhotosIds.getJSONObject(i);
                                Photo photo = new Photo(item.getString("id"));
                                photosList.add(photo);

                                Log.i("after-photo",photo.toString());

                            }

                            adapter.notifyDataSetChanged();

                            } catch (JSONException e) {
                                e.printStackTrace();

                            if(response.getError() != null) Log.i("error-graph",response.getError().toString());
                        }

                    }

                }
        ).executeAsync();

    }

    private class PhotosGridAdapter extends BaseAdapter {
        private Context context;
        private final int LOAD_LIMIT = 25;
        private  int identifer = 0;

        private List<Photo> photos;
        PhotosGridAdapter(Context context, List<Photo> photos) {
            this.context = context;
            this.photos = photos;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(position == LOAD_LIMIT*identifer) {

                loadMoreContent();
                identifer++;
            }


            Log.i("item-position",String.valueOf(position));

            ImageView imageView;
            if (convertView == null) {

                 imageView = new ImageView(context);
                 imageView.setLayoutParams(new GridView.LayoutParams(500,500));
                 imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                 imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            Glide.with(getApplicationContext())
                    .load(getImageById(getItem(position).getId()))
                    .into(imageView);



            return imageView;
        }
        @Override
        public int getCount() {
            return photos.size();
        }
        @Override
        public Photo getItem(int position) {
            return photos.get(position);
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
    }



    private class PhotosIdsFetcher extends AsyncTask<String, Object, List<Photo>> {
        @Override
        protected List<Photo> doInBackground(String... params) {
            String id = params[0]; // album-id
            String protocol = "https://";
            String host = "graph.facebook.com/v2.9/";
            String access_token = AccessToken.getCurrentAccessToken().getToken();
            String suffix = "/photos";
            String url = protocol + host + id + suffix + "?" + "access_token=" + access_token;
            String JsonResponse;
            try {
                URL Url = new URL(url);
                URLConnection con = Url.openConnection();
                InputStream in = con.getInputStream();
                String encoding = con.getContentEncoding();
                encoding = encoding == null ? "UTF-8" : encoding;
                JsonResponse = IOUtils.toString(in, encoding);
                JSONObject jsonObject = new JSONObject(JsonResponse);
                JSONArray fetchedPhotosIds = jsonObject.getJSONArray("data");

                JSONObject paging = jsonObject.getJSONObject("paging");

                after = jsonObject.getJSONObject("paging").getJSONObject("cursors").getString("after");

                Log.i("next",after);

                ;

                for (int i = 0; i < fetchedPhotosIds.length(); i++) {
                    JSONObject item = fetchedPhotosIds.getJSONObject(i);
                    Photo photo = new Photo(item.getString("id"));

                    Log.i("photo",photo.toString());
                    photosList.add(photo);
                }



            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return photosList;
        }


        @Override
        protected void onPostExecute(List<Photo> list) {
            super.onPostExecute(list);
            adapter.notifyDataSetChanged();
            progress.dismiss();
        }

    }
}
