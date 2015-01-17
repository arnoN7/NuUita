package example.com.nuuita;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by administrateur on 24/12/14.
 */
public class ParseEventTaskQueue {

    List<ParseEvent> eventList;

    public ParseEventTaskQueue() {
        eventList = new ArrayList<ParseEvent>();
    }

    public void add(ParseObject todo) {
        ParseEvent event = new ParseEvent(ParseTaskType.SaveInBackground, todo);
        eventList.add(event);
        Log.d("Add Parse Event", "Size heap: " + eventList.size());

        //If the list only contain the element we just add
        if(eventList.size() == 1) {
            executeNext();
        }
    }

    public void executeNext() {
        if (eventList.size() > 0) {
            Log.d("Execute Parse Event", "Size heap: " + eventList.size());
            eventList.get(0).execute(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Log.d("Exec Event Done", "Size heap: " + eventList.size());
                    eventList.remove(0);
                    executeNext();
                }
            });

        }
    }
}
