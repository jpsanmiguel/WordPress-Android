package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Map;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

/**
 * The configuration screen for the StatsWidgetProvider widget.
 */
public class StatsWidgetConfigureActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Spinner mBlogSpinner;
    private int mAccountIDs[];
    private TextView mBlogSpinnerTitle;
    private int mSelectedBlogID;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel out of the widget
        // placement if they press the back button.
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));

        // Intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // If not signed into WordPress inform the user
        if (!AccountHelper.isSignedIn()) {
            ToastUtils.showToast(getBaseContext(), R.string.stats_widget_error_no_account, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        String[] blogNames = getBlogNames();
        if (blogNames == null || blogNames.length == 0) {
            ToastUtils.showToast(getBaseContext(), R.string.stats_widget_error_no_visible_blog, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        // Preselect the first blog in the list
        mSelectedBlogID = mAccountIDs[0];

        // only one blog, skip config
        if (blogNames.length == 1) {
            addWidgetToScreenAndFinish();
            return;
        }

        // Set the view layout resource to use.
        setContentView(R.layout.stats_widget_config);

        mBlogSpinnerTitle = (TextView) findViewById(R.id.blog_spinner_title);
        mBlogSpinner = (Spinner) findViewById(R.id.blog_spinner);
        mBlogSpinner.setOnItemSelectedListener(this);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_menu_dropdown_item, blogNames);
        mBlogSpinner.setAdapter(adapter);
    }

    private void addWidgetToScreenAndFinish() {
        final Context context = StatsWidgetConfigureActivity.this;
        StatsWidgetProvider.setupNewWidget(context, mAppWidgetId, mSelectedBlogID);
        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.blog_spinner) {
            mSelectedBlogID = mAccountIDs[position];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // noop
    }

    private String[] getBlogNames() {
        String[] extraFields = {"homeURL"};
        List<Map<String, Object>> accounts = WordPress.wpDB.getBlogsBy("isHidden = 0", extraFields);
        if (accounts.size() > 0) {
            final String blogNames[] = new String[accounts.size()];
            mAccountIDs = new int[accounts.size()];
            Blog blog;
            for (int i = 0; i < accounts.size(); i++) {
                Map<String, Object> account = accounts.get(i);
                blogNames[i] = BlogUtils.getBlogNameOrHomeURLFromAccountMap(account);
                mAccountIDs[i] = (Integer) account.get("id");
                blog = WordPress.wpDB.instantiateBlogByLocalId(mAccountIDs[i]);
                if (blog == null) {
                    ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
                    return null;
                }
            }
            return blogNames;
        }
        return null;
    }

    public void onOkClicked(View view) {
        addWidgetToScreenAndFinish();
    }
}
