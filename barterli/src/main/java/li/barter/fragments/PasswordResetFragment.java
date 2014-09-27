/*
 * Copyright (C) 2014 barter.li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
  * limitations under the License.
 */

package li.barter.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request.Method;
import com.google.android.gms.analytics.HitBuilders.EventBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import li.barter.BarterLiApplication;
import li.barter.R;
import li.barter.activities.AbstractBarterLiActivity.AlertStyle;
import li.barter.analytics.AnalyticsConstants.Actions;
import li.barter.analytics.AnalyticsConstants.Categories;
import li.barter.analytics.AnalyticsConstants.ParamKeys;
import li.barter.analytics.AnalyticsConstants.ParamValues;
import li.barter.analytics.AnalyticsConstants.Screens;
import li.barter.analytics.GoogleAnalyticsManager;
import li.barter.http.BlRequest;
import li.barter.http.HttpConstants;
import li.barter.http.HttpConstants.ApiEndpoints;
import li.barter.http.HttpConstants.RequestId;
import li.barter.http.IBlRequestContract;
import li.barter.http.ResponseInfo;
import li.barter.utils.AppConstants.Keys;
import li.barter.utils.Logger;
import li.barter.utils.Utils;

/**
 * @author Anshul Kamboj Fragment to reset the password after receiving the token via email
 */

@FragmentTransition(enterAnimation = R.anim.slide_in_from_right, exitAnimation = R.anim.zoom_out,
                    popEnterAnimation = R.anim.zoom_in,
                    popExitAnimation = R.anim.slide_out_to_right)
public class PasswordResetFragment extends AbstractBarterLiFragment implements
        OnClickListener {

    private static final String TAG = "PasswordResetFragment";

    /**
     * Minimum length of the entered password
     */
    private final int mMinPasswordLength = 8;
    private Button   mResetButton;
    private EditText mTokenEditText;
    private EditText mNewPasswordEditText;
    private EditText mConfirmNewPasswordEditText;
    private String   mEmailId;

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        init(container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_reset_password, null);

        final Bundle extras = getArguments();

        if (extras != null) {
            mEmailId = extras.getString(Keys.EMAIL);
        }

        mResetButton = (Button) view
                .findViewById(R.id.button_reset_password);
        mNewPasswordEditText = (EditText) view.findViewById(R.id.edit_text_newpassword);
        mConfirmNewPasswordEditText = (EditText) view
                .findViewById(R.id.edit_text_confirmpassword);
        mTokenEditText = (EditText) view.findViewById(R.id.edit_text_token);


        mResetButton.setOnClickListener(this);
        return view;
    }


    @Override
    protected Object getTaskTag() {
        return hashCode();
    }


    @Override
    public void onClick(final View v) {

        switch (v.getId()) {

            case R.id.button_reset_password: {
                if (isInputValid()) {
                    GoogleAnalyticsManager
                            .getInstance()
                            .sendEvent(
                                    new EventBuilder(Categories.CONVERSION, Actions.SIGN_IN_ATTEMPT)
                                            .set(ParamKeys.TYPE, ParamValues.RESET)
                            );
                    callPasswordReset(mTokenEditText.getText().toString(),
                                      mNewPasswordEditText.getText().toString(), mEmailId);
                }
                break;
            }


        }
    }


    /**
     * Call the password_reset Api
     *
     * @param email The entered email
     */

    private void callPasswordReset(String token, String password, String email) {


        final JSONObject requestObject = new JSONObject();

        try {
            requestObject.put(HttpConstants.EMAIL, email);
            requestObject.put(HttpConstants.PASSWORD, password);
            requestObject.put(HttpConstants.TOKEN, token);
            final BlRequest request = new BlRequest(Method.POST, HttpConstants.getApiBaseUrl()
                    + ApiEndpoints.PASSWORD_RESET, requestObject.toString(), mVolleyCallbacks);

            request.setRequestId(RequestId.RESET_USER_PASSWORD);
            addRequestToQueue(request, true, 0, true);

        } catch (final JSONException e) {
            // Should never happen
            Logger.e(TAG, e, "Error building create user json");
        }


    }


    /**
     * Validates the text fields for resetting a password. Automatically sets the error messages for
     * the text fields
     *
     * @return <code>true</code> If the input is valid, <code>false</code> otherwise
     */
    private boolean isInputValid() {

        final String password = mNewPasswordEditText.getText().toString();
        final String confirmPassword = mConfirmNewPasswordEditText.getText().toString();
        final String token = mTokenEditText.getText().toString();
        boolean isValid = !TextUtils.isEmpty(password);
        isValid = !TextUtils.isEmpty(confirmPassword);

        if (!isValid) {
            mNewPasswordEditText.setError(getString(R.string.error_enter_email));
        }

        if (isValid) {
            isValid &= !TextUtils.isEmpty(token);
            if (!isValid) {
                mTokenEditText.setError(getString(R.string.error_enter_token));
            }
        }
        if (isValid) {
            isValid &= (password.length() >= mMinPasswordLength);
            if (!isValid) {
                mNewPasswordEditText
                        .setError(getString(R.string.error_password_minimum_length,
                                            mMinPasswordLength));
            }
        }
        if (isValid) {
            if (password.equals(confirmPassword)) {
                isValid = true;
            } else {
                isValid = false;
            }
        }

        return isValid;
    }

    @Override
    public void onSuccess(final int requestId,
                          final IBlRequestContract request,
                          final ResponseInfo response) {
        if (requestId == RequestId.RESET_USER_PASSWORD) {

            final Bundle userInfo = response.responseBundle;

            Utils.updateUserInfoFromBundle(userInfo, true);
            BarterLiApplication.startChatService();

            final Intent returnIntent = new Intent();
            final Bundle arguments = getArguments();
            if (arguments != null && arguments.containsKey(Keys.ONWARD_INTENT)) {
                returnIntent
                        .putExtra(Keys.ONWARD_INTENT, arguments.getParcelable(Keys.ONWARD_INTENT));
            }

            getActivity().setResult(ActionBarActivity.RESULT_OK, returnIntent);
            getActivity().finish();

        }


    }

    @Override
    public void onBadRequestError(final int requestId,
                                  final IBlRequestContract request, final int errorCode,
                                  final String errorMessage, final Bundle errorResponseBundle) {

        if (requestId == RequestId.RESET_USER_PASSWORD) {
            showCrouton(errorMessage, AlertStyle.ERROR);
        }
    }


    @Override
    protected String getAnalyticsScreenName() {
        return Screens.PASSWORD_RESET;
    }

}
