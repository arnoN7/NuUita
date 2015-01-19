package example.com.nuuita;

import android.graphics.Typeface;
import android.widget.TextView;

import com.parse.ParseUser;

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


}
