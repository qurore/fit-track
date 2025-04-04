package com.github.qurore.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;
    
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView userProfileTextView;
    private Button loginButton;
    private Button logoutButton;
    private ImageButton settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Set up UI elements
        userProfileTextView = findViewById(R.id.userProfileTextView);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        settingsButton = findViewById(R.id.settingsButton);
        
        loginButton.setOnClickListener(v -> signIn());
        logoutButton.setOnClickListener(v -> signOut());
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        });
        
        // Initially hide logout and settings buttons
        logoutButton.setVisibility(View.GONE);
        settingsButton.setVisibility(View.GONE);
        
        // Check if we need to logout (coming from DashboardActivity)
        if (getIntent().getBooleanExtra("LOGOUT", false)) {
            signOut();
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        
        // If user is already logged in, navigate to Dashboard
        if (currentUser != null && !getIntent().getBooleanExtra("LOGOUT", false)) {
            // Launch Dashboard
            Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
            dashboardIntent.putExtra("USER_NAME", currentUser.getDisplayName());
            dashboardIntent.putExtra("USER_EMAIL", currentUser.getEmail());
            startActivity(dashboardIntent);
        }
    }
    
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Authentication Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                updateUI(null);
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            
                            // Launch Dashboard
                            Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
                            if (user != null) {
                                dashboardIntent.putExtra("USER_NAME", user.getDisplayName());
                                dashboardIntent.putExtra("USER_EMAIL", user.getEmail());
                            }
                            startActivity(dashboardIntent);
                        } else {
                            // Sign in failed
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication Failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }
    
    private void signOut() {
        // Firebase sign out
        mAuth.signOut();
        
        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }
    
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            userProfileTextView.setText(getString(R.string.welcome_user, user.getDisplayName()));
            loginButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            settingsButton.setVisibility(View.VISIBLE);
        } else {
            userProfileTextView.setText("");
            loginButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            settingsButton.setVisibility(View.GONE);
        }
    }
}