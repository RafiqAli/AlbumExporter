package com.example.alira.albumexporter;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.alira.albumexporter.util.Helper;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static com.example.alira.albumexporter.util.Helper.hasActiveInternetConnection;

public class MainActivity extends AppCompatActivity {

    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if the user is logged in, we launch the albums activity directly
        if (Profile.getCurrentProfile() != null) {
            Intent intentt = new Intent(getApplicationContext(),AlbumsActivity.class);
            startActivity(intentt);
        }


        // test if the mobile phone is connected to the internet
        if(Helper.isConnected(getApplicationContext()))
        {
            setContentView(R.layout.activity_main);

            LoginButton loginButton;
            callbackManager = CallbackManager.Factory.create();

            loginButton = (LoginButton) findViewById(R.id.login_button);
            // permissions
            loginButton.setReadPermissions("public_profile", "email","user_photos");

            // Callback registration
            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                @Override
                public void onSuccess(LoginResult loginResult) {
                    // on success, we launch the albums activity
                    Intent intent = new Intent(getApplicationContext(),AlbumsActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onCancel() {
                    Intent intent = new Intent(getApplicationContext(),ErrorActivity.class);
                    intent.putExtra("error","We understand your decision, but the application cannot retrieve your photos without your permission. Thank you for using our app.");
                    startActivity(intent);
                }

                @Override
                public void onError(FacebookException exception) {

                    Intent intent = new Intent(getApplicationContext(),ErrorActivity.class);
                    intent.putExtra("error","An error has occurred while trying to retrieve your data from Facebook server.");
                    startActivity(intent);
                }
            });

        }
        else
        {

            Intent intent = new Intent(getApplicationContext(),ErrorActivity.class);
            intent.putExtra("error","error connecting to internet");
            startActivity(intent);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //callbackManager.onActivityResult(requestCode, resultCode, data);

        if (callbackManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

    }
}
