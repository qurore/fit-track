package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.auth0.android.authentication.AuthenticationAPIClient;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Auth0 auth0;
    private TextView userProfileTextView;
    private Button loginButton;
    private Button logoutButton;
    private ImageButton settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Auth0
        auth0 = new Auth0(
                getString(R.string.com_auth0_client_id),
                getString(R.string.com_auth0_domain)
        );
        
        // Set up UI elements
        userProfileTextView = findViewById(R.id.userProfileTextView);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        settingsButton = findViewById(R.id.settingsButton);
        
        loginButton.setOnClickListener(v -> login());
        logoutButton.setOnClickListener(v -> logout());
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        });
        
        // Initially hide logout and settings buttons
        logoutButton.setVisibility(View.GONE);
        settingsButton.setVisibility(View.GONE);
        
        // Check if we need to logout (coming from DashboardActivity)
        if (getIntent().getBooleanExtra("LOGOUT", false)) {
            logout();
        }
    }
    
    private void login() {
        WebAuthProvider.login(auth0)
                .withScheme(getString(R.string.com_auth0_scheme))
                .withScope("openid profile email read:current_user")
                .withAudience(String.format("https://%s/api/v2/", getString(R.string.com_auth0_domain)))
                .start(this, new Callback<Credentials, AuthenticationException>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        // Store credentials or tokens as needed
                        String accessToken = credentials.getAccessToken();
                        
                        // Fetch user profile
                        fetchUserProfile(accessToken);
                        
                        // Launch the Dashboard Activity
                        Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(dashboardIntent);
                        
                        // Optionally finish the login activity if you don't want to return to it on back press
                        // finish();
                    }

                    @Override
                    public void onFailure(AuthenticationException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                    "Login failed: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    private void fetchUserProfile(String accessToken) {
        // Fetch user information from Auth0 API
        AuthenticationAPIClient authClient = new AuthenticationAPIClient(auth0);
        authClient.userInfo(accessToken)
                .start(new Callback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        String name = userProfile.getName();
                        String email = userProfile.getEmail();
                        
                        // Add debug log
                        Log.d(TAG, "Auth0 Profile - Name: " + name + ", Email: " + email);
                        
                        runOnUiThread(() -> {
                            // Create intent with user data and launch dashboard
                            // without updating MainActivity UI
                            Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
                            dashboardIntent.putExtra("USER_NAME", name);
                            dashboardIntent.putExtra("USER_EMAIL", email);
                            startActivity(dashboardIntent);
                            
                            // Optionally finish MainActivity to prevent returning to login screen on back press
                            // finish();
                        });
                    }

                    @Override
                    public void onFailure(AuthenticationException e) {
                        // Add error log
                        Log.e(TAG, "Failed to get profile: " + e.getMessage());
                        
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                    "Failed to get user profile: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    public void logout() {
        WebAuthProvider.logout(auth0)
                .withScheme(getString(R.string.com_auth0_scheme))
                .start(this, new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onSuccess(Void unused) {
                        runOnUiThread(() -> {
                            userProfileTextView.setText("");
                            loginButton.setVisibility(View.VISIBLE);
                            logoutButton.setVisibility(View.GONE);
                            settingsButton.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onFailure(AuthenticationException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                    "Logout failed: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
}