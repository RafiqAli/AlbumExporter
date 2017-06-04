package com.example.alira.albumexporter;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.alira.albumexporter.models.Album;
import com.example.alira.albumexporter.models.Photo;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.R.attr.path;

public class WelcomeActivity extends AppCompatActivity {


    private int albumsLength = -1;
    private List<Album> albumsList = new ArrayList<Album>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

       /* final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();*/

        /*
        for(int i=0;i<9;i++)
        {
            Album album = new Album();

            album.setName("Album"+String.valueOf(i));
            album.setCountWrapper(String.valueOf(i)+" photos");

            Bitmap cover = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.yinyang);
            album.setCover_photo(cover);

            albumsList.add(album);
        }*/


        final ProgressDialog progress = new ProgressDialog(getApplicationContext());

        final GridView albumsGrid = (GridView) this.findViewById(R.id.welcome_grid);

        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        Thread requestsThread = new Thread() {

            public void run()
            {
                getAlbums();
            }
        };

        requestsThread.start();

        Thread thread = new Thread(){
            public void run(){

                while (true)
                {
                    if(albumsLength != -1)
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                albumsGrid.setAdapter(new AlbumGridAdapter(getApplicationContext(),albumsList));
                                progress.dismiss();
                            }
                        });

                        try {
                            this.finalize();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }


                }
            }
        };

        thread.start();
    }


    public List<Album> getAlbums()
    {

        String albumsRetrievalUrl = "/"+Profile.getCurrentProfile().getId()+"/albums?fields=name,count,privacy,cover_photo";

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                albumsRetrievalUrl,
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        try {

                            JSONObject object = response.getJSONObject();
                            JSONArray albums = new JSONArray(object.getString("data"));

                             albumsLength = albums.length();

                            Album albuum = new Album();

                            for (int i=0;i<albums.length();i++)
                            {
                                JSONObject jo = (JSONObject) albums.get(i);

                                JSONObject cover_photo = jo.getJSONObject("cover_photo");

                                Album album = new Album(jo.getString("id"));

                                album.setName(jo.getString("name"));
                                album.setCount(jo.getInt("count"));
                                album.setCountWrapper(String.valueOf(album.getCount())+" photos");

                                album.setCover_photo_id(cover_photo.getString("id"));

                                getPhoto(album);


                                Log.i("album-tostring",album.toString());


                                albumsList.add(album);



                            }


                        } catch (JSONException E) {
                            E.printStackTrace();
                        } catch (Exception e)
                        {
                            Log.i("error-message",e.getMessage());
                            //Log.i("albums-retrieval-error",response.getError().getErrorMessage());
                        }
                    }
                }
        ).executeAsync();

        return albumsList;
    }

    public void getPhoto(final Album album)
    {
        String photoRetrievalUrl = "/"+album.getId()+"/picture";

        Bundle params = new Bundle();
        params.putString("type", "thumbnail");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                photoRetrievalUrl,
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        URL url = response.getConnection().getURL();
                        AsyncPhoto async = new AsyncPhoto();
                        try {
                            Bitmap downloadedImage = (Bitmap) async.execute(url.toString()).get();
                            album.setCover_photo(downloadedImage);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                    }
                }
        ).executeAsync();

    }


    private class AlbumGridAdapter extends BaseAdapter
    {

        private Context context;

        private LayoutInflater inflater;

        private  List<Album> albums;

        public AlbumGridAdapter(Context context,List<Album> albums)
        {
            this.context = context;
            this.albums  = albums;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View gridView;

            if(convertView == null)
            {
                gridView = new View(context);

                gridView = inflater.inflate(R.layout.album_item,null);

                ImageView albumPhoto = (ImageView) gridView.findViewById(R.id.album_item_imageview);

                TextView albumName  = (TextView) gridView.findViewById(R.id.album_item_name);
                TextView albumCount = (TextView) gridView.findViewById(R.id.album_item_count);

                albumPhoto.setImageBitmap(this.getItem(position).getCover_photo());
                albumName.setText(this.getItem(position).getName());
                albumCount.setText(this.getItem(position).getCountWrapper());

            }
            else
            {
                gridView = convertView;
            }

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


class AsyncPhoto extends AsyncTask<String,String,Bitmap> {

        @Override
        protected Bitmap doInBackground(String[] params) {
            String stringUrl = params[0];
            Bitmap decodedImage = null;
            try {
                URL url = new URL(stringUrl);
                decodedImage = BitmapFactory.decodeStream(url.openStream());
                Log.i("decoded",String.valueOf(decodedImage.getRowBytes()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return decodedImage;
        }
    };












