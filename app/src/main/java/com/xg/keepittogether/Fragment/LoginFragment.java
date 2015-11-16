package com.xg.keepittogether.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xg.keepittogether.R;

public class LoginFragment extends Fragment implements View.OnClickListener{

    private OnButtonClickedListener mListener;

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    public LoginFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        TextView signInButton = (TextView)view.findViewById(R.id.signInBTInFragment);
        signInButton.setOnClickListener(this);
        TextView signUpButton = (TextView)view.findViewById(R.id.signUpBTInFragment);
        signUpButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnButtonClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnButtonClickedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.signInBTInFragment) {
            EditText emailView = (EditText) getActivity().findViewById(R.id.loginEmailET);
            EditText passwordView = (EditText)getActivity().findViewById(R.id.loginPasswordET);
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
                Toast.makeText(getActivity(), validationErrorMessage.toString(), Toast.LENGTH_LONG)
                        .show();
                return;
            }
            mListener.signIn(emailView.getText().toString(), passwordView.getText().toString());
        }
        if(id == R.id.signUpBTInFragment) {
            mListener.startSignUp();
        }
    }

    public interface OnButtonClickedListener {
        // TODO: Update argument type and name
        public void signIn(String email, String password);
        public void startSignUp();
    }

    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0) {
            return false;
        }
        return true;
    }
}
