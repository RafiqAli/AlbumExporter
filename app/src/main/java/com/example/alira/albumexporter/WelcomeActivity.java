package com.example.alira.albumexporter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.alira.albumexporter.models.Album;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;



public class WelcomeActivity extends AppCompatActivity {
    private List<Album> albumsList = new ArrayList<>();
    GridView albumsGrid = null;
    ProgressDialog progress = null;
    AlbumGridAdapter adapter = new AlbumGridAdapter(this,albumsList);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);



        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching albums...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();


        albumsGrid = (GridView) this.findViewById(R.id.welcome_grid);

        //new AlbumsFetcher().execute();

        albumsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i("clicklistener","ok");

                Album album = albumsList.get(position);
                Intent intent = new Intent(getApplicationContext(), PhotosActivity.class);
                intent.putExtra("album_id", album.getId());
                intent.putExtra("album_name", album.getName());
                intent.putExtra("album_count",album.getCount());
                startActivity(intent);
            }
        });


        String path = Profile.getCurrentProfile().getId()+"/albums";
        Bundle params = new Bundle();

        params.putString("fields","name,count,cover_photo");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                path,
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {

                        JSONObject jsonObject = response.getJSONObject();
                        try {
                            JSONArray fetchedAlbums = jsonObject.getJSONArray("data");


                            for (int i = 0; i < fetchedAlbums.length(); i++) {
                                JSONObject jo = (JSONObject) fetchedAlbums.get(i);
                                JSONObject cover_photo = jo.getJSONObject("cover_photo");
                                Album album = new Album(jo.getString("id"));
                                album.setName(jo.getString("name"));
                                album.setCount(jo.getInt("count"));
                                album.setCountWrapper(String.valueOf(album.getCount()));
                                album.setCover_photo_id(cover_photo.getString("id"));

                                albumsList.add(album);
                            }

                            Collections.sort(albumsList);

                            albumsGrid.setAdapter(new AlbumGridAdapter(getApplicationContext(),albumsList));

                            progress.dismiss();

                        } catch (JSONException e) {
                            e.printStackTrace();

                            if(response.getError() != null) Log.i("error-graph",response.getError().toString());
                        }

                    }

                }
        ).executeAsync();



    }



    public String getImageById(String photo_id)
    {
        Bitmap decodedImage = null;
        String id = photo_id; // picture id
        String protocol = "https://";
        String host = "graph.facebook.com/v2.9/";
        String access_token = AccessToken.getCurrentAccessToken().getToken();
        String suffix = "/picture";
        String type = "album";
        String Url = protocol + host + id + suffix + "?" + "access_token=" + access_token + "&" + "type=" + type;

        return Url;
    }


































    private class AlbumsFetcher extends AsyncTask<String, Object, List<Album>> {
        @Override
        protected List<Album> doInBackground(String... params) {
            String id = Profile.getCurrentProfile().getId();
            String protocol = "https://";
            String host = "graph.facebook.com/v2.9/";
            String access_token = AccessToken.getCurrentAccessToken().getToken();
            String suffix = "/albums";
            String fields = "name,count,privacy,cover_photo";
            String url = protocol + host + id + suffix + "?" + "fields=" + fields + "&" + "access_token=" + access_token;
            String JsonResponse;
            try {
                URL Url = new URL(url);
                URLConnection con = Url.openConnection();
                InputStream in = con.getInputStream();
                String encoding = con.getContentEncoding();
                encoding = encoding == null ? "UTF-8" : encoding;
                JsonResponse = IOUtils.toString(in, encoding);
                JSONObject jsonObject = new JSONObject(JsonResponse);
                JSONArray fetchedAlbums = jsonObject.getJSONArray("data");

                class BitmapDecodingRunnable implements Runnable{
                    private Album album;
                    private BitmapDecodingRunnable(Album album) { this.album = album; }
                    @Override
                    public void run() {
                        try {
                            Bitmap decodedImage =  new PhotoFetcher().execute(album.getCover_photo_id()).get();
                            album.setCover_photo(decodedImage);
                        } catch (InterruptedException | ExecutionException e) { e.printStackTrace();}
                    }
                }

                for (int i = 0; i < fetchedAlbums.length(); i++) {
                    JSONObject jo = (JSONObject) fetchedAlbums.get(i);
                    JSONObject cover_photo = jo.getJSONObject("cover_photo");
                    Album album = new Album(jo.getString("id"));
                    album.setName(jo.getString("name"));
                    album.setCount(jo.getInt("count"));
                    album.setPrivacy(jo.getString("privacy"));
                    album.setCountWrapper(String.valueOf(album.getCount()));
                    album.setCover_photo_id(cover_photo.getString("id"));
                    Log.i("album-tostring", album.toString());

                    runOnUiThread(new BitmapDecodingRunnable(album));

                    albumsList.add(album);
                }
            } catch (JSONException E) {
                E.printStackTrace();
            } catch (Exception e) {
                Log.i("error-message", e.getMessage());
            }

            return albumsList;
        }

        @Override
        protected void onPostExecute(List<Album> list) {
            super.onPostExecute(list);
            albumsGrid.setAdapter(new AlbumGridAdapter(getApplicationContext(),albumsList));
            progress.dismiss();
        }
    }

    private class AlbumGridAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        private List<Album> albums;
        AlbumGridAdapter(Context context, List<Album> albums) {
            this.context = context;
            this.albums = albums;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View gridView;

                gridView = inflater.inflate(R.layout.album_item,parent,false);
                ImageView albumPhoto = (ImageView) gridView.findViewById(R.id.album_item_imageview);
                TextView albumName = (TextView) gridView.findViewById(R.id.album_item_name);
                TextView albumCount = (TextView) gridView.findViewById(R.id.album_item_count);
                final ProgressBar progressBar = (ProgressBar) gridView.findViewById(R.id.album_item_progress_bar);

                Glide.with(getApplicationContext())
                        .load(getImageById(getItem(position).getId()))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(albumPhoto);


                albumPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                albumName.setText(this.getItem(position).getName());
                albumCount.setText(this.getItem(position).getCountWrapper());

            return gridView;
        }
        @Override
        public int getCount() {
            return albums.size();
        }
        @Override
        public Album getItem(int position) {
            return albums.get(position);
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
    }
}

class PhotoFetcher extends AsyncTask<String, String, Bitmap> {
    @Override
    protected Bitmap doInBackground(String[] params) {
        Bitmap decodedImage = null;
        String id = params[0]; // picture id
        String protocol = "https://";
        String host = "graph.facebook.com/v2.9/";
        String access_token = AccessToken.getCurrentAccessToken().getToken();
        String suffix = "/picture";
        String type = "album";
        String Url = protocol + host + id + suffix + "?" + "access_token=" + access_token + "&" + "type=" + type;
        try {
            URL url = new URL(Url);
            decodedImage = BitmapFactory.decodeStream(url.openStream());

            Log.i("decoded", String.valueOf(decodedImage.getRowBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decodedImage;
    }
}
