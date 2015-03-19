package Network;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import org.apache.http.NameValuePair;

import java.util.List;

/**
 * Created by linux on 19/3/15.
 */
public class RESTService extends AsyncTask<List<NameValuePair>,Void,Integer> implements DialogInterface.OnCancelListener {
    public static int SUCESS_CODE = 1;
    public static int FAILURE_CODE = 0;

    private Context context;
    private String URL;
    private Dialog progressDialog;
    private boolean showProgressDialog;
    private boolean isCancellable;
    private String progressMessage = "";
    private String callbackResult = "{\"status\":\"False\",\"message\":\"Network unavailable\"}";
    private AsyncTaskListener callback;
    private int requestCode;

    private boolean isTaskCancelled = false;

    private boolean isUnknownHostException = false;
    private boolean isInternetNotConnected = false;

    private static final String MESSAGE_NETWORK_UNAVAILABLE = "Please check your internet connectivity.";

    public RESTService(Context context, String URL,
                       boolean showProgressDialog, String progressMessage,
                       boolean isCancellable, AsyncTaskListener callback, int requestCode) {
        this.context = context;
        this.URL = URL;
        this.progressMessage = progressMessage;
        this.callback = callback;
        this.requestCode = requestCode;
        this.isCancellable = isCancellable;
        this.showProgressDialog = showProgressDialog;
    }


    @Override
    protected Integer doInBackground(List<NameValuePair>... params) {
        return null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {

    }
}
