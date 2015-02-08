package example.com.nuuita;

import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by administrateur on 24/12/14.
 */
public class Utils {

    public static void setTitleItalicIfDraft(Todo todo, TextView todoTitle) {
        if (todo.isDraft()) {
            todoTitle.setTypeface(null, Typeface.ITALIC);
        } else {
            todoTitle.setTypeface(null, Typeface.NORMAL);
        }
    }
    public static String getRoleName(String todoListName, ParseUser owner) {
        return owner.getObjectId() + todoListName;
    }

    public static void sendPushToUser(ParseUser user, String pushText) {
        // Create our Installation query
        ParseQuery pushQuery = ParseInstallation.getQuery();
        //ParseQuery userQuery = ParseUser.getQuery();
        //userQuery.whereEqualTo("appName", "NuUita");
        //pushQuery.whereMatchesQuery(TodoListActivity.USER_KEY, userQuery);
        //pushQuery.whereEqualTo("appName", "NuUita");

        // Send push notification to query
        ParsePush push = new ParsePush();
        push.setQuery(pushQuery); // Set our Installation query
        push.setMessage(pushText);
        push.sendInBackground();
    }


}
