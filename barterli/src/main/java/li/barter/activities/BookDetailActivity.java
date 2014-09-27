/*
 * Copyright (C) 2014 barter.li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package li.barter.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import li.barter.R;
import li.barter.data.DatabaseColumns;
import li.barter.data.SQLConstants;
import li.barter.data.SQLiteLoader;
import li.barter.data.TableUserBooks;
import li.barter.fragments.AbstractBarterLiFragment;
import li.barter.fragments.BookDetailFragment;
import li.barter.http.IBlRequestContract;
import li.barter.http.ResponseInfo;
import li.barter.utils.AppConstants;
import li.barter.utils.Utils;

/**
 * Activity to display the book details. Send the book Id in the intent arguments with the key
 * {@linkplain li.barter.utils.AppConstants.Keys#ID} and the details will be loaded
 * <p/>
 * Created by vinay.shenoy on 10/07/14.
 */
@ActivityTransition(createEnterAnimation = R.anim.slide_in_from_right, createExitAnimation = R.anim.zoom_out, destroyEnterAnimation = R.anim.zoom_in, destroyExitAnimation = R.anim.slide_out_to_right)
public class BookDetailActivity extends AbstractDrawerActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "BookDetailActivity";

    private static final String BOOK_BY_ID = DatabaseColumns.ID + SQLConstants.EQUALS_ARG;

    /** ID of the book to be loaded */
    private String mBookId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        initDrawer(R.id.drawer_layout, isMultipane() ? R.id.frame_side_content : R.id.frame_nav_drawer, isMultipane());
        mBookId = getIntent().getStringExtra(AppConstants.Keys.ID);

        if (savedInstanceState == null) {
            if (TextUtils.isEmpty(mBookId)) {
                finish();
            } else {
                loadBookDetailsFragment();
                loadBookDetails();
            }
        }

    }

    /** Loads the book details fragment into the main content */
    private void loadBookDetailsFragment() {

        loadFragment(R.id.frame_content, (AbstractBarterLiFragment) Fragment.instantiate(this,
                                                                                         BookDetailFragment.class
                                                                                                 .getName()
                     ),
                     AppConstants.FragmentTags.BOOK_DETAIL, false, null
        );
    }

    /** Loads the book details */
    private void loadBookDetails() {

        getSupportLoaderManager().restartLoader(AppConstants.Loaders.BOOK_DETAILS, null, this);
    }

    @Override
    protected boolean isDrawerActionBarToggleEnabled() {
        return false;
    }

    @Override
    protected String getAnalyticsScreenName() {
        return null;
    }

    @Override
    protected Object getTaskTag() {
        return hashCode();
    }

    @Override
    public void onSuccess(final int requestId, final IBlRequestContract request, final ResponseInfo response) {

    }

    @Override
    public void onBadRequestError(final int requestId, final IBlRequestContract request, final int errorCode, final String errorMessage, final Bundle errorResponseBundle) {

    }


    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {

        if (id == AppConstants.Loaders.BOOK_DETAILS) {
            return new SQLiteLoader(this, false, TableUserBooks.NAME, null, BOOK_BY_ID,
                                    new String[]{mBookId}, null, null, null, null);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {

        if (loader.getId() == AppConstants.Loaders.BOOK_DETAILS) {

            if (data.moveToFirst()) {
                final Bundle bookDetails = Utils.cursorToBundle(data);
                final BookDetailFragment fragment = (BookDetailFragment) getSupportFragmentManager()
                        .findFragmentByTag(
                                AppConstants.FragmentTags.BOOK_DETAIL);

                if (fragment != null && fragment.isResumed()) {
                    fragment.updateBookDetails(bookDetails);
                }
            }
        }

    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {

    }
}
