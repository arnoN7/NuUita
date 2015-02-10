package example.com.nuuita;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.List;

/**
 * Created by Arnaud Rover on 10/02/15.
 */
public class MyPebbleReceiver extends PebbleKit.PebbleDataReceiver {
    public static final int
            MENU_KEY_ASK = 2,
            MENU_LIST_ID = 3,
            ITEM_TEXT = 4,
            MENU_ITEM_TEXT = 5;
    public PebbleCommunication communicationContext;

    public MyPebbleReceiver(PebbleCommunication communicationContext) {
        super(PebbleCommunication.NuUita);
        this.communicationContext = communicationContext;
    }

    @Override
    public void receiveData(Context context, int transactionId, PebbleDictionary data) {
        //ACK the message
        PebbleKit.sendAckToPebble(context, transactionId);

        int res = data.getUnsignedIntegerAsLong(0).intValue();
        Log.i("Pebble", String.format("%d", res));
        // check the key exists
        if (data.getUnsignedIntegerAsLong(0).intValue() != 0) {
            int button = data.getUnsignedIntegerAsLong(0).intValue();
            switch (button) {
                case MENU_KEY_ASK:
                    sendMenus(context);
                    break;
                case MENU_LIST_ID:
                    int cell;
                    cell = data.getUnsignedIntegerAsLong(1).intValue();
                    sendListItems(context, cell);
                    break;
            }
        }
    }

    protected void sendMenus(Context context) {
        int i = 0;
        String tmp;
        List<String> menu = Utils.getTodoListsNames(communicationContext.getFragmentList());

        while (i < menu.size() && i < 20) {
            PebbleDictionary dict = new PebbleDictionary();
            dict.addInt32(0, MENU_ITEM_TEXT);
            dict.addInt32(1, i); // Add to PebbleDictionnary the list_id

            // Check the string's length and reduce it to 16 if necessary.
            if (menu.get(i).length() > 16)
            {
                tmp = menu.get(i).substring(0, 16);
                dict.addString(2, tmp);
            }
            else
                dict.addString(2, menu.get(i));

            //check if it is the end of the menu of list and write the right message into the PebbleDictionnary
            if ((i + 1) == menu.size())
                dict.addInt32(3, 1);
            else
                dict.addInt32(3, 0);

            //Send the Dictionnary
            Log.i("Pebble", dict.toJsonString());
            communicationContext.sendData(context, dict);
            //PebbleKit.sendDataToPebble(context, NuUita, dict);
            i++;
        }
    }

    protected void sendListItems(Context context, int listID) {
        int i = 0;
        String tmp;
        List<Todo> todos = communicationContext.getFragmentList().get(listID).getTodos(false);
        while (i < todos.size() && i < 20) {
            PebbleDictionary dict = new PebbleDictionary();
            dict.addInt32(0, ITEM_TEXT);
            dict.addInt32(1, i);

            // Check the string's length and reduce it to 16 if necessary.
            if (todos.get(i).getTitle().length() > 16)
            {
                tmp = todos.get(i).getTitle().substring(0, 16);
                dict.addString(2, tmp);
            }
            else
                dict.addString(2, todos.get(i).getTitle());

            //check if it is the end of the list and write the right message into the PebbleDictionnary
            if ((i + 1) == todos.size())
                dict.addInt32(3, 1);
            else
                dict.addInt32(3, 0);

            //Send the Dictionnary
            Log.i("Pebble", dict.toJsonString());
            communicationContext.sendData(context, dict);
            //PebbleKit.sendDataToPebble(context, NuUita, dict);
            i++;

        }
    }
}
