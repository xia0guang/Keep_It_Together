package com.xg.keepittogether;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SignUpCallback;


public class SignUpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
    }

    public void signUp(View view) {
        EditText emailView = (EditText)findViewById(R.id.signUpEmailET);
        EditText memberNameView = (EditText)findViewById(R.id.signUpMemberNameET);
        EditText passwordView = (EditText)findViewById(R.id.signUpPasswordET);
        EditText matchPasswordView = (EditText)findViewById(R.id.matchPasswordET);

        //Validate the input
        boolean validationError = false;
        StringBuilder validationErrorMessage = new StringBuilder(getResources().getString(R.string.error_intro));
        if (isEmpty(emailView)) {
            validationError = true;
            validationErrorMessage.append(getResources().getString(R.string.error_blank_email));
        }
        if (isEmpty(memberNameView)) {
            if (validationError) {
                validationErrorMessage.append(getResources().getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getResources().getString(R.string.error_blank_member_name));
        }
        if (isEmpty(passwordView)) {
            if (validationError) {
                validationErrorMessage.append(getResources().getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getResources().getString(R.string.error_blank_password));
        }
        if (!isMatching(passwordView, matchPasswordView)) {
            if (validationError) {
                validationErrorMessage.append(getResources().getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getResources().getString(
                    R.string.error_mismatched_passwords));
        }
        validationErrorMessage.append(getResources().getString(R.string.error_end));

        // If there is a validation error, display the error
        if (validationError) {
            Toast.makeText(this, validationErrorMessage.toString(), Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Set up a progress dialog
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Please wait.");
        dlg.setMessage("Signing up.  Please wait.");
        dlg.show();

        // Set up a new Parse user
        ParseUser user = new ParseUser();
        user.setEmail(emailView.getText().toString());
        user.setUsername(emailView.getText().toString());
        user.setPassword(passwordView.getText().toString());

        // Call the Parse signup method
        user.signUpInBackground(new SignUpCallback() {

            @Override
            public void done(ParseException e) {
                dlg.dismiss();
                if (e != null) {
                    // Show the error message
                    Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                } else {

                    //Store member name in Members Table
                    ParseObject privateNote = new ParseObject("Members");
                    EditText memberNameView = (EditText)findViewById(R.id.signUpMemberNameET);
                    privateNote.put("memberName", memberNameView.getText().toString());
                    privateNote.setACL(new ParseACL(ParseUser.getCurrentUser()));
                    privateNote.saveInBackground();
                    //Set member name in preferences file
                    SharedPreferences userPref = getSharedPreferences("User_Preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putString("memberName", memberNameView.getText().toString());
                    editor.commit();

                    // Start an intent for the dispatch activity
                    Intent intent = new Intent(SignUpActivity.this, DispatchActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                }
            }
        });
    }

    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isMatching(EditText etText1, EditText etText2) {
        if (etText1.getText().toString().equals(etText2.getText().toString())) {
            return true;
        } else {
            return false;
        }
    }

}
