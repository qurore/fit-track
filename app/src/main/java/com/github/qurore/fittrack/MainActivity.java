package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.github.qurore.fittrack.database.MongoDBClient;
import org.bson.Document;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    private Auth0 auth0;
    private TextView userProfileTextView;
    private Button loginButton;
    private Button logoutButton;
    private MongoDBClient mongoDBClient;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Auth0
        auth0 = new Auth0(
                getString(R.string.com_auth0_client_id),
                getString(R.string.com_auth0_domain)
        );
        
        // 注: 最新のAuth0 SDKでは setOIDCConformant メソッドは不要
        // デフォルトでOIDC準拠モードになっています
        
        // Set up UI elements
        userProfileTextView = findViewById(R.id.userProfileTextView);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        loginButton.setOnClickListener(v -> login());
        logoutButton.setOnClickListener(v -> logout());
        
        // Initially hide logout button
        logoutButton.setVisibility(View.GONE);
        
        // MongoDB Atlasクライアントの初期化
        String connectionString = getString(R.string.mongodb_connection_string);
        String databaseName = getString(R.string.mongodb_database_name);
        mongoDBClient = MongoDBClient.getInstance(connectionString, databaseName);
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
                        
                        // Update UI
                        runOnUiThread(() -> {
                            loginButton.setVisibility(View.GONE);
                            logoutButton.setVisibility(View.VISIBLE);
                            Toast.makeText(MainActivity.this, 
                                    "Login successful!", 
                                    Toast.LENGTH_SHORT).show();
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
        // Auth0 APIからユーザー情報を取得
        AuthenticationAPIClient authClient = new AuthenticationAPIClient(auth0);
        authClient.userInfo(accessToken)
                .start(new Callback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        String name = userProfile.getName();
                        String email = userProfile.getEmail();
                        
                        // デバッグ用ログを追加
                        android.util.Log.d("Auth0Profile", "Name: " + name + ", Email: " + email);
                        
                        runOnUiThread(() -> {
                            // 明示的にnullチェックを追加
                            if (name != null && email != null) {
                                userProfileTextView.setText(String.format("Name: %s\nEmail: %s", name, email));
                            } else {
                                userProfileTextView.setText("Profile received but some data is missing");
                            }
                        });
                        
                        // MongoDBにユーザープロファイルを保存
                        saveUserProfileToMongoDB(userProfile);
                    }

                    @Override
                    public void onFailure(AuthenticationException e) {
                        // エラーログを追加
                        android.util.Log.e("Auth0Error", "Failed to get profile: " + e.getMessage());
                        
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
    
    // ユーザープロファイル情報をMongoDBに保存する例
    private void saveUserProfileToMongoDB(UserProfile userProfile) {
        try {
            Document userDocument = new Document()
                    .append("auth0_id", userProfile.getId())
                    .append("name", userProfile.getName())
                    .append("email", userProfile.getEmail())
                    .append("created_at", new java.util.Date());
            
            Disposable disposable = mongoDBClient.insertDocumentAsync("users", userDocument)
                    .subscribe(success -> {
                        if (success) {
                            Log.d("MongoDB", "User profile saved successfully");
                        } else {
                            Log.e("MongoDB", "Failed to save user profile");
                        }
                    }, error -> {
                        Log.e("MongoDB", "Error saving user profile: " + error.getMessage());
                        // Don't let MongoDB errors crash the app
                    });
            
            disposables.add(disposable);
        } catch (Exception e) {
            // Catch any exceptions to prevent app crashes
            Log.e("MongoDB", "Exception when trying to save profile: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        // RxJavaのDisposableをクリーンアップ
        disposables.clear();
        
        // MongoDBクライアントを閉じる
        if (mongoDBClient != null) {
            mongoDBClient.close();
        }
        
        super.onDestroy();
    }
}