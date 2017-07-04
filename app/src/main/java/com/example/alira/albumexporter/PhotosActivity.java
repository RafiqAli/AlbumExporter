package com.example.alira.albumexporter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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


/* This class is responsible for the display of the pictures included
 * in each album. when the user taps an album_item within the gridview on the
 * activity_albums. the AlbumsActivity send data to photos activity in intents
 * this class then takes the album id and perform a graph request in order to get
 * the first (25) load of photos and prints them on gridview.
 * when the user scrolls to the end the closetoend() method get called and then if the
 * the graph request (wrapped in loadContent method) is free (is not locked due to a previous call @see util/Locker )
 * the closetoend method performs a graphrequest which gets the next load of picture through the next
 * pagination link.
 */

public class PhotosActivity extends AppCompatActivity {
    List<Photo> photosList = new ArrayList<>();
    ProgressDialog progress;
    GridView photosGrid;
    PhotosGridAdapter adapter = new PhotosGridAdapter(this,photosList);
    Locker locker = new Locker();
    GraphResponse lastGraphResponse = null;

    // the callback that get executed after the request retreives
    // a response.
    GraphRequest.Callback callback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            lastGraphResponse = response;
            JSONObject jsonObject = response.getJSONObject();
            try {
                // the result is a jsonObject that contains the data field which
                // contains a json array with photo nodes limited by default limit.
                JSONArray fetchedPhotosIds = jsonObject.getJSONArray("data");

                for (int i = 0; i < fetchedPhotosIds.length(); i++) {
                    JSONObject item = fetchedPhotosIds.getJSONObject(i);
                    Photo photo = new Photo(item.getString("id"));
                    photosList.add(photo);
                }
                progress.dismiss();
                // we notify the adapter to refresh the gridview
                adapter.notifyDataSetChanged();
                // and we unlock the ressource
                locker.unlock();

            } catch (JSONException e)
            {
                e.printStackTrace();
                // if there's a problem in json response, the app will show a dialog with an error message shown below,
                // and it will return back to previous activity (Albums Activity)
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PhotosActivity.this);
                // setting the error message
                alertDialogBuilder.setMessage("Sorry an error has occured we couldn't fetch your photos, please try again later");
                        alertDialogBuilder.setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // terminate the current app, which leads us the previous
                                        finish();
                                    }
                                });
                // displaying the error dialog
                alertDialogBuilder.show();
            }
        }
    };

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

         /* we retreive the values transmitted through activities by intents
          * they are repectively album id and album name, and we set the title of
          * the activity by the album name
          */
        Intent intent = getIntent();
        String album_id   = intent.getExtras().getString("album_id");
        String album_name = intent.getExtras().getString("album_name");
        setTitle(album_name);

        //we launch a progress dialog that lasts until the images metadata loads up
        progress = new ProgressDialog(this);
        progress.setTitle("Loading"); progress.setMessage("Wait while loading..."); progress.setCancelable(false);
        //progress.show();

        //binding the adapter to the gridview
        photosGrid = (GridView) findViewById(R.id.photos_grid);
        photosGrid.setAdapter(adapter);

        // we set some the type and the path parameters
        Bundle params = new Bundle();
        params.putString("type","normal");String path = album_id+"/photos";

            //we execute the photos metadata fetch asynchronously
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

        // method that returns a boolean indicating
        // the user has scrolled to end or not
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
            // if the users scrolls to the end, and the request ressource
            // is not locked (see locker and load more content for more information)
            // we call the function loadContent() in order to load more content using
            // the next link in pagination field.
            if(closeToEnd(position) && !locker.isLocked()) loadContent();

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View gridView;
            gridView = inflater.inflate(R.layout.photo_item,parent,false);
            // retreiving the view elements
            ImageView photoView = (ImageView) gridView.findViewById(R.id.photo_item_imageview);
            final ProgressBar progressBar = (ProgressBar) gridView.findViewById(R.id.photo_item_progress_bar);

            //listening to the checkbox actions
            final CheckBox checkBox = (CheckBox) gridView.findViewById(R.id.photo_item_checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    //TODO: add items to list and then display it when user clicks on the desired actionbar
                }
            });

            //performing a toggle mark/unmark when user clicks on view item
            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            });
            checkBox.setVisibility(View.VISIBLE);

            //downloading using Glide library and binding the image to the appropriate
            //imageview inside the view.
            Glide.with(getApplicationContext())
                    // downloading the image, getting it from cache if already downloaded
                    .load(Helper.getUrlByImageId(getItem(position).getId(),"normal"))
                    //listening to the request beahavior
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
                    // binding the resulted image to the imageview
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
