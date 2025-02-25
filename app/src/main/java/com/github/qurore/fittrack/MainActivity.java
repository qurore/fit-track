package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.management.ManagementException;

public class MainActivity extends AppCompatActivity {
    private Auth0 auth0;
    private TextView userProfileTextView;
    private Button loginButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Set up edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize Auth0
        auth0 = new Auth0(
                getString(R.string.com_auth0_client_id),
                getString(R.string.com_auth0_domain)
        );
        
        // Set up UI elements
        userProfileTextView = findViewById(R.id.userProfileTextView);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        loginButton.setOnClickListener(v -> login());
        logoutButton.setOnClickListener(v -> logout());
        
        // Initially hide logout button
        logoutButton.setVisibility(View.GONE);
    }
    
    private void login() {
        WebAuthProvider.login(auth0)
                .withScheme(getString(R.string.com_auth0_scheme))
                .withScope("openid profile email")
                .withAudience(String.format("https://%s/userinfo", getString(R.string.com_auth0_domain)))
                .start(this, new Callback<Credentials, AuthenticationException>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        // Store credentials or tokens as needed
                        String accessToken = credentials.getAccessToken();
                        
                        // Fetch user profile
                        fetchUserProfile(accessToken);
                        
                        // Update UI
                        runOnUiThread(() -> {
                            loginButton.setVisibility(View.GONE);
                            logoutButton.setVisibility(View.VISIBLE);
                        });
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
        UsersAPIClient usersClient = new UsersAPIClient(auth0, accessToken);
        usersClient.getProfile(accessToken)
                .start(new Callback<UserProfile, ManagementException>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        String name = userProfile.getName();
                        String email = userProfile.getEmail();
                        
                        runOnUiThread(() -> {
                            userProfileTextView.setText(String.format("Name: %s\nEmail: %s", name, email));
                        });
                    }

                    @Override
                    public void onFailure(ManagementException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                    "Failed to get user profile: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    private void logout() {
        WebAuthProvider.logout(auth0)
                .withScheme(getString(R.string.com_auth0_scheme))
                .start(this, new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onSuccess(Void unused) {
                        runOnUiThread(() -> {
                            userProfileTextView.setText("");
                            loginButton.setVisibility(View.VISIBLE);
                            logoutButton.setVisibility(View.GONE);
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