package org.wordpress.android.ui.notifications.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;

import java.util.Random;

import static org.wordpress.android.push.GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID;

public class NotificationsPendingDraftsReceiver extends BroadcastReceiver {

    public static final int PENDING_DRAFTS_NOTIFICATION_ID = GENERIC_LOCAL_NOTIFICATION_ID + 1;
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";

    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
    public static final long ONE_WEEK = ONE_DAY * 7;
    public static final long ONE_MONTH = ONE_WEEK * 4;

    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 40; // just over a month

    private static final int BASE_REQUEST_CODE = 100;

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        // here build notifications
        mContext = context;

        // get extras from intent in order to build notification
        long postId = intent.getLongExtra(POST_ID_EXTRA, 0);
        if (postId != 0) {
            Post post = WordPress.wpDB.getPostForLocalTablePostId(postId);
            if (post != null) {

                long now = System.currentTimeMillis();
                long daysInDraft = (now - post.getDateLastUpdated()) / ONE_DAY;
                boolean isPage = post.isPage();

                if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                    String formattedString = mContext.getString(R.string.pending_draft_one_generic);

                    long one_day_ago = now - ONE_DAY;
                    long one_week_ago = now - ONE_WEEK;
                    long one_month_ago = now - ONE_MONTH;

                    long dateLastUpdated = post.getDateLastUpdated();
                    if (dateLastUpdated < one_month_ago) {
                        formattedString = mContext.getString(R.string.pending_draft_one_month);
                    }
                    else
                    if (dateLastUpdated < one_week_ago) {
                        // use any of the available 2 string formats, randomly
                        Random randomNum = new Random();
                        int result = randomNum.nextInt(2);
                        if (result == 0)
                            formattedString = mContext.getString(R.string.pending_draft_one_week_1);
                        else
                            formattedString = mContext.getString(R.string.pending_draft_one_week_2);
                    }
                    else
                    if (dateLastUpdated < one_day_ago) {
                        // use any of the available 2 string formats, randomly
                        Random randomNum = new Random();
                        int result = randomNum.nextInt(2);
                        if (result == 0)
                            formattedString = mContext.getString(R.string.pending_draft_one_day_1);
                        else
                            formattedString = mContext.getString(R.string.pending_draft_one_day_2);
                    }

                    buildSinglePendingDraftNotification(post.getTitle(), formattedString, postId, isPage);

                } else {
                    // if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't know (i.e. value for lastUpdated
                    // is zero) then just show a generic message
                    buildSinglePendingDraftNotificationGeneric(post.getTitle(), postId, isPage);
                }
            }
        }

    }

    private String getPostTitle(String postTitle) {
        String title = postTitle;
        if (TextUtils.isEmpty(postTitle)) {
            title = "(" + mContext.getResources().getText(R.string.untitled) + ")";
        }
        return title;
    }

    private void buildSinglePendingDraftNotification(String postTitle, String formattedMessage, long postId, boolean isPage){
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage), String.format(formattedMessage, getPostTitle(postTitle)), postId, isPage);
    }

    private void buildSinglePendingDraftNotificationGeneric(String postTitle, long postId, boolean isPage){
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage), String.format(mContext.getString(R.string.pending_draft_one_generic), getPostTitle(postTitle)), postId, isPage);
    }

    private PendingIntent getResultIntentForOnePost(long postId, boolean isPage) {

        Intent resultIntent = new Intent(mContext, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.putExtra(POST_ID_EXTRA, postId);
        resultIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, BASE_REQUEST_CODE, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }

    private void buildNotificationWithIntent(PendingIntent intent, String message, long postId, boolean isPage) {
        NotificationCompat.Builder builder = NativeNotificationsUtils.getBuilder(mContext);
        builder.setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(intent);

        if (postId != 0) {
            addOpenDraftActionForNotification(mContext, builder, postId, isPage);
            addIgnoreActionForNotification(mContext, builder, postId, isPage);
        }
        addDismissActionForNotification(mContext, builder, postId, isPage);

        NativeNotificationsUtils.showMessageToUserWithBuilder(builder, message, false,
                makePendingDraftNotificationId(postId), mContext);
    }

    public static int makePendingDraftNotificationId(long localPostId) {
        // constructs a notification ID (int) based on a localPostId (long) which should be low numbers
        // by casting explicitely
        // Integer.MAX_VALUE should be enough notifications
        return PENDING_DRAFTS_NOTIFICATION_ID + (int)localPostId;
    }

    private void addOpenDraftActionForNotification(Context context, NotificationCompat.Builder builder, long postId, boolean isPage) {
        // adding open draft action
        Intent openDraftIntent = new Intent(context, WPMainActivity.class);
        openDraftIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        openDraftIntent.putExtra(POST_ID_EXTRA, postId);
        openDraftIntent.putExtra(IS_PAGE_EXTRA, isPage);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, BASE_REQUEST_CODE + 1, openDraftIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_edit_icon, mContext.getText(R.string.edit),
                pendingIntent);
    }

    private void addIgnoreActionForNotification(Context context, NotificationCompat.Builder builder, long postId, boolean isPage) {
        // Call processing service when user taps on IGNORE - we should remember this decision for this post
        Intent ignoreIntent = new Intent(context, NotificationsProcessingService.class);
        ignoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_IGNORE);
        ignoreIntent.putExtra(POST_ID_EXTRA, postId);
        ignoreIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent ignorePendingIntent =  PendingIntent.getService(context,
                BASE_REQUEST_CODE + 2, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_close_white_24dp, mContext.getText(R.string.ignore),
                ignorePendingIntent);
    }

    private void addDismissActionForNotification(Context context, NotificationCompat.Builder builder, long postIdList, boolean isPage) {
        // Call processing service when notification is dismissed
        Intent notificationDeletedIntent = new Intent(context, NotificationsProcessingService.class);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_DISMISS);
        notificationDeletedIntent.putExtra(POST_ID_EXTRA, postIdList);
        notificationDeletedIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent dismissPendingIntent =  PendingIntent.getService(context,
                BASE_REQUEST_CODE + 3, notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
    }

}
