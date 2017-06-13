package com.example.alira.albumexporter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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
    PhotosGridAdapter adapter = new PhotosGridAdapter(this,photosList);
    PagingHandler pagingHandler = null;


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
        String album_id   = intent.getExtras().getString("album_id");
        String album_name = intent.getExtras().getString("album_name");
        int album_count = intent.getExtras().getInt("album_count");

        pagingHandler = new PagingHandler(album_id,album_count);

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

    private class PagingHandler
    {
        private int limit = 25; // default
        private int count;
        private int remaining_packages = 0;
        private String after;
        private String album_id;


        public  PagingHandler(String album_id,int count)
        {
            this.album_id = album_id;
            this.count = count;
            remaining_packages = count/limit;
        }

        public PagingHandler(String album_id,int count,int limit)
        {
            this.album_id = album_id;
            this.count = count;
            this.limit = limit;
        }


        public String deliverNextLoadCursor()
        {
            //remaining_packages--;

            return after;
        }

        public void setNextLoadCursor(String nextLoadCursor)
        {
            this.after = nextLoadCursor;
        }

        public boolean hasMore()
        {
            if(remaining_packages > 0) return true;

            else return false;
        };

        public int getLimit() {
            return limit;
        }

        public int getCount() {
            return count;
        }

        public String getAlbum_id() {
            return album_id;
        }
    }

    public void loadMoreContent()
    {
        if(pagingHandler.hasMore())
        {
            Bundle params = new Bundle();
            params.putInt("limit",pagingHandler.getLimit());
            params.putString("after",pagingHandler.deliverNextLoadCursor());
            String path = pagingHandler.getAlbum_id()+"/photos";
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
                                pagingHandler.setNextLoadCursor(jsonObject.getJSONObject("paging").getJSONObject("cursors").getString("after"));

                                Log.i("paginHandler",pagingHandler.deliverNextLoadCursor());

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

    }

    private class PhotosGridAdapter extends BaseAdapter {
        private Context context;
        private final int CLOSE_TO_END_COEFFICIENT = 20;
        private  int identifer = 0;
        private LayoutInflater inflater;

        private List<Photo> photos;
        PhotosGridAdapter(Context context, List<Photo> photos) {
            this.context = context;
            this.photos = photos;
        }


        private boolean closeToEnd(int position)
        {
            if(position == CLOSE_TO_END_COEFFICIENT*identifer) {
                identifer++;
                return  true;
            }
            else return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if(closeToEnd(position)) loadMoreContent();

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View gridView;

            gridView = inflater.inflate(R.layout.photo_item,parent,false);

            ImageView photoView = (ImageView) gridView.findViewById(R.id.photo_item_imageview);
            final ProgressBar progressBar = (ProgressBar) gridView.findViewById(R.id.photo_item_progress_bar);
            final CheckBox checkBox = (CheckBox) gridView.findViewById(R.id.photo_item_checkbox);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    //TODO: add items to list and then display it when user clicks on the desired actionbar
                }
            });

            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            });

            checkBox.setVisibility(View.VISIBLE);

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
                    .into(photoView);


            return gridView;
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

                pagingHandler.setNextLoadCursor(jsonObject.getJSONObject("paging").getJSONObject("cursors").getString("after"));

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
