package example.com.nuuita;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;


public class PebbleCommunication {
    private class QueueItem {
        private QueueItem(Context context, PebbleDictionary dict) { this.context = context; this.dict = dict; }
        private Context context;
        private PebbleDictionary dict;
    }

    private PebbleDataReceiver mReceiver;
    private Context appContext;
    private TextView mButtonView;
    private TextView connectionOff;
    private boolean connected;
    private boolean isAppStarted;
    private Queue<QueueItem> queue;
    private boolean pebbleReady;
    private List<TodoListFragment> fragmentList;






    public static final UUID NuUita = UUID.fromString("4f353b5e-f30a-4d57-9edc-7a742a25216f");

    public PebbleCommunication (Context appContext) {
        queue = new ArrayDeque<>();
        pebbleReady = true;
        this.appContext = appContext;

        connected = false;
        //ACTIVITY = this;

    }

    public void sendData(Context context, PebbleDictionary dict) {
        if (pebbleReady) {
            PebbleKit.sendDataToPebble(context, NuUita, dict);
            pebbleReady = false;
            queue.offer(new QueueItem(context, dict));
        }
        else
            queue.offer(new QueueItem(context, dict));
    }

    protected void onResume() {

        connected = PebbleKit.isWatchConnected(appContext);

        Log.i("Pebble", "Pebble is " + (connected ? "connected" : "not connected"));

        if (connected) {

            if (!isAppStarted) {
                Log.i("Pebble", "entree dans startAppOnPebble");
                // Launch the pebble app and start AppMessage (a ne pas mettre dans le on resume sous peine de bug... A reverifier)
                PebbleKit.startAppOnPebble(appContext, NuUita);
                isAppStarted = true;
            }

            PebbleKit.registerReceivedAckHandler(appContext, new PebbleKit.PebbleAckReceiver(NuUita) {
                @Override
                public void receiveAck(Context context, int i) {
                    queue.poll();
                    Log.i("Pebble", "ACK received");
                    if (queue.peek() != null) {
                        PebbleKit.sendDataToPebble(queue.peek().context, NuUita, queue.peek().dict);
                    }
                    else
                        pebbleReady = true;
                }
            });
            PebbleKit.registerReceivedNackHandler(appContext, new PebbleKit.PebbleNackReceiver(NuUita) {
                @Override
                public void receiveNack(Context context, int i) {
                    Log.i("Pebble", "NACK received");
                    pebbleReady = false;
                }
            });

        }
    }

    protected void onPause() {

       if (mReceiver != null) {
            //unregisterReceiver(mReceiver);
            mReceiver = null;
       }

        PebbleKit.closeAppOnPebble(appContext, NuUita);
    }

    public List<TodoListFragment> getFragmentList() {
        return fragmentList;
    }

    public void setFragmentList(List<TodoListFragment> fragmentList) {
        this.fragmentList = fragmentList;
    }
}
