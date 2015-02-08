package example.com.nuuita;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by Arnaud Rover on 17/01/15.
 */
public class NuUitaApplication extends Application {
    public static final String TODO_GROUP_NAME = "ALL_TODOS";

    @Override
    public void onCreate() {
        super.onCreate();

        // add todo's subclass
        ParseObject.registerSubclass(Todo.class);

        // enable the Local Datastore
        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(this, "GeDgr7ykroUMA85QY8wyU8LR2KClQqbXlHtoz6Mx", "n9PZAknvxDGQbb24YW1I8XUgddQXnykwLAdJuNdz");
        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        ParseACL.setDefaultACL(defaultACL, true);


        /*ParsePush.("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });*/
    }
}
