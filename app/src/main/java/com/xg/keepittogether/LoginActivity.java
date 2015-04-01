package com.xg.keepittogether;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;


public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }



    public void startSignUp(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void login(View view) {
        EditText emailView = (EditText)findViewById(R.id.loginEmailET);
        EditText passwordView = (EditText)findViewById(R.id.loginPasswordET);


        //Validate the input
        boolean validationError = false;
        StringBuilder validationErrorMessage = new StringBuilder(getResources().getString(R.string.error_intro));
        if (isEmpty(emailView)) {
            validationError = true;
            validationErrorMessage.append(getResources().getString(R.string.error_blank_email));
        }
        if (isEmpty(passwordView)) {
            if (validationError) {
                validationErrorMessage.append(getResources().getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getResources().getString(R.string.error_blank_password));
        }
        validationErrorMessage.append(getResources().getString(R.string.error_end));

        // If there is a validation error, display the error
        if (validationError) {
            Toast.makeText(LoginActivity.this, validationErrorMessage.toString(), Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Set up a progress dialog
        final ProgressDialog dlg = new ProgressDialog(LoginActivity.this);
        dlg.setTitle("Please wait.");
        dlg.setMessage("Logging in.  Please wait.");
        dlg.show();
        ParseUser.logInInBackground(emailView.getText().toString(), passwordView.getText().toString(), new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                dlg.dismiss();
                if (e != null) {
                    // Show the error message
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    // Start an intent for the dispatch activity
                    FragmentTransaction transiction = getFragmentManager().beginTransaction();
                    SelectMemberNameFragment selectMemberNameFragment = new SelectMemberNameFragment();
                    transiction.add(R.id.login_layout, selectMemberNameFragment,"selectMemberName");
                    transiction.commit();

                }
            }
        });
        // Call the Parse login method
    }

    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0) {
            return false;
        } else {
            return true;
        }
    }

}
