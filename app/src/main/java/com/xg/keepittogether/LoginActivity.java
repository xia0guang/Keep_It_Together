package com.xg.keepittogether;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.xg.keepittogether.Fragment.LoginFragment;


public class LoginActivity extends Activity implements LoginFragment.OnButtonClickedListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        LoginFragment loginFragment = LoginFragment.newInstance();
        transaction.add(R.id.sign_in_fragment, loginFragment, "loginFragment");
        transaction.commit();

    }


    @Override
    public void startSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    @Override
    public void signIn(String email, String password) {
        // Set up a progress dialog
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Please wait.");
        dlg.setMessage("Logging in.  Please wait.");
        dlg.show();
        ParseUser.logInInBackground(email, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                dlg.dismiss();
                if (e != null) {
                    // Show the error message
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    // Start an intent for the dispatch activity
                    FragmentTransaction transaction2 = getFragmentManager().beginTransaction();
                    SelectMemberNameFragment selectMemberNameFragment = new SelectMemberNameFragment();
                    transaction2.replace(R.id.sign_in_fragment, selectMemberNameFragment, "selectMemberName");
                    transaction2.commit();
                }
            }
        });
        // Call the Parse login method
    }

}
