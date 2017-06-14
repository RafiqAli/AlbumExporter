package com.example.alira.albumexporter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.alira.albumexporter.models.Photo;
import com.example.alira.albumexporter.util.Helper;
import com.example.alira.albumexporter.util.Locker;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PhotosActivity extends AppCompatActivity {
    List<Photo> photosList = new ArrayList<>();
    ProgressDialog progress;
    GridView photosGrid;
    PhotosGridAdapter adapter = new PhotosGridAdapter(this,photosList);
    Locker locker = new Locker();
    GraphResponse lastGraphResponse = null;

    GraphRequest.Callback callback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            lastGraphResponse = response;
            JSONObject jsonObject = response.getJSONObject();
            try {
                JSONArray fetchedPhotosIds = jsonObject.getJSONArray("data");
                for (int i = 0; i < fetchedPhotosIds.length(); i++) {
                    JSONObject item = fetchedPhotosIds.getJSONObject(i);
                    Photo photo = new Photo(item.getString("id"));
                    photosList.add(photo);
                }
                progress.dismiss();
                adapter.notifyDataSetChanged();
                locker.unlock();
            } catch (JSONException e) {e.printStackTrace();}
        }
    };

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Intent intent = getIntent();
        String album_id   = intent.getExtras().getString("album_id");
        String album_name = intent.getExtras().getString("album_name");
        setTitle(album_name);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading"); progress.setMessage("Wait while loading..."); progress.setCancelable(false);
        progress.show();

        photosGrid = (GridView) findViewById(R.id.photos_grid);
        photosGrid.setAdapter(adapter);

        Bundle params = new Bundle();
        params.putString("type","normal");String path = album_id+"/photos";
        new GraphRequest(
                AccessToken.getCurrentAccessToken(), path, params,
                HttpMethod.GET, callback
        ).executeAsync();
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
    private class PhotosGridAdapter extends BaseAdapter {
        private Context context;
        private final int CLOSE_TO_END_COEFFICIENT = 20;
        private  int identifer = 1;
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
            if(closeToEnd(position) && !locker.isLocked()) loadContent();

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
                    .load(Helper.getUrlByImageId(getItem(position).getId(),"normal"))
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
}
