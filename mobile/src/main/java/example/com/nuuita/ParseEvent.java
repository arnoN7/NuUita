package example.com.nuuita;

import android.util.Log;

import com.parse.ParseObject;
import com.parse.SaveCallback;

/**
 * Created by administrateur on 24/12/14.
 */
public class ParseEvent {

    ParseTaskType type;
    ParseObject object;

    public ParseEvent(ParseTaskType type, ParseObject object) {
        this.type = type;
        this.object = object;
    }
    public void execute (SaveCallback saveCallBack) {
        if (type == ParseTaskType.SaveInBackground) {
            Log.d("saveInBackground", "Save...");
            object.saveInBackground(saveCallBack);
        }
    }
}
