package com.example.alira.albumexporter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import com.example.alira.albumexporter.util.Helper;
import com.example.alira.albumexporter.util.Locker;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AlbumsActivity extends AppCompatActivity {

    private List<Album> albumsList = new ArrayList<>();
    GridView albumsGrid = null;
    ProgressDialog progress = null;
    AlbumGridAdapter adapter = new AlbumGridAdapter(this,albumsList);
    GraphResponse lastGraphResponse = null;

    Locker locker = new Locker();


    GraphRequest.Callback callback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {

            /* updating the class-scope variable with
             * the next response for paging purposes.
             */
            lastGraphResponse = response;
            JSONObject jsonObject = response.getJSONObject();
            try {
                // simple json pojo mapping
                JSONArray fetchedAlbums = jsonObject.getJSONArray("data");
                for (int i = 0; i < fetchedAlbums.length(); i++) {
                    JSONObject jo = (JSONObject) fetchedAlbums.get(i);
                    Album album = new Album(jo.getString("id"));
                    album.setName(jo.getString("name"));
                    album.setCount(jo.getString("count"));
                    album.setCover_photo_id(jo.getJSONObject("cover_photo").getString("id"));
                    albumsList.add(album);
                }
                // sorting the list by name
                Collections.sort(albumsList);
                adapter.notifyDataSetChanged();

                progress.dismiss();
                // unlocking the variable so the girdadapter
                // can be free to send async requests
                locker.unlock();
            } catch (JSONException e) {e.printStackTrace();}
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching albums...");
        progress.setCancelable(false);
        progress.show();

        // (view,adapter,clickListener)-binding the 'albumsGrid' gridView
        albumsGrid = (GridView) this.findViewById(R.id.welcome_grid);
        albumsGrid.setAdapter(adapter);
        albumsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Album album = albumsList.get(position);
                Intent intent = new Intent(getApplicationContext(), PhotosActivity.class);
                intent.putExtra("album_id", album.getId());
                intent.putExtra("album_name", album.getName());
                startActivity(intent);
            }
        });

        String path = Profile.getCurrentProfile().getId()+"/albums";
        Bundle params = new Bundle();
        params.putString("fields","name,count,cover_photo");params.putInt("limit",2);
        new GraphRequest(AccessToken.getCurrentAccessToken(), path, params, HttpMethod.GET,callback).executeAsync();
    }

    /* This function gets the member GraphResponse variable 'lastGraphResponse'
     * and extracts a GraphResquest from it that points on the next
     * bulk of data depending on the limit.
     */
    private void loadContent()
    {
        // locking the function to prevent the adapter
        // from calling multiples async requests with
        // the same definitions which produces duplicate
        // data.
        locker.lock();

        // this function contruct a GraphRequest to the next paging-cursor from previous GraphResponse.
        GraphRequest nextGraphRequest = lastGraphResponse.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT);

        // if we reached the end of our pages, the above method 'getRequestForPagedResults'
        // return 'null' object
        if(nextGraphRequest != null)
        {
            // for clarificating and formatting purposes
            // I declared the callback besides the members
            // variables.
            nextGraphRequest.setCallback(callback);
            nextGraphRequest.executeAsync();
        }
    }

    private class AlbumGridAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        private List<Album> albums;
        private final int CLOSE_TO_END_COEFFICIENT = 1;
        private  int identifer = 1;

        AlbumGridAdapter(Context context, List<Album> albums) {
            this.context = context;
            this.albums = albums;
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
        public View getView(int position, View convertView, ViewGroup parent) {

            // loading content whenever the user get closer to end.
            if(closeToEnd(position) && !locker.isLocked() ) loadContent();

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View gridView;

                gridView = inflater.inflate(R.layout.album_item,parent,false);
                // binding views to objects and filling them with data
                ImageView albumPhoto = (ImageView) gridView.findViewById(R.id.album_item_imageview);
                TextView albumName = (TextView) gridView.findViewById(R.id.album_item_name);
                TextView albumCount = (TextView) gridView.findViewById(R.id.album_item_count);
                albumName.setText(this.getItem(position).getName());
                albumCount.setText(this.getItem(position).getCount());
                albumPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                final ProgressBar progressBar = (ProgressBar) gridView.findViewById(R.id.album_item_progress_bar);

                Glide.with(getApplicationContext())

                        // calls the getUrlByImageId in order to retrieve the Url of
                        // the desired picture
                        .load(Helper.getUrlByImageId(getItem(position).getId(),"album"))
                        // I added this listener in order to desactivate
                        // the progressbar whenever the Glide load function
                        // gets the picture
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
