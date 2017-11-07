package uk.ac.warwick.my.app.bridge;


import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles making JS calls into the HTML page. Relies on calling
 * reset() as a page is first loaded, and ready() when it is known
 * to be ready for calling methods. In between reset() and ready()
 * it will enqueue requests and call them in sequence when ready().
 */
public final class JavascriptInvoker {
    private boolean pageReady;
    private final List<String> pendingInvocations = new ArrayList<>();
    private final Handler handler = new Handler();
    private final WebView webView;

    private Timer stuckChecker = newStuckCheck();

    public JavascriptInvoker(WebView webView) {
        this.webView = webView;
    }

    public void reset() {
        stuckChecker = newStuckCheck();
        pageReady = false;

        // used to clear the pending invocations here, but we queue stuff up
        // on activity creation and it's hard to avoid destroying those.
        // The only thing that can happen is to invoke some commands intended
        // for the previous page.
    }

    // Clear pending invocations. We should only do this when destroying the Invoker.
    public void clear() {
        pendingInvocations.clear();
    }

    public void ready() {
        pageReady = true;
        Log.d("MyWarwick", "HTML page reports it is ready");
        // handler to run on the right thread
        handler.post(new Runnable() {
            public void run() {
                for (String js : pendingInvocations) {
                    doInvoke(js);
                }
                pendingInvocations.clear();
            }
        });
    }

    public void invokeMyWarwickMethodIfAvailable(String methodName) {
        invoke(String.format("('%s' in MyWarwick) && MyWarwick.%s()", methodName, methodName));
    }

    public void invokeMyWarwickMethod(String js) {
        invoke("MyWarwick." + js);
    }

    public void invoke(String js) {
        if (pageReady) {
            doInvoke(js);
        } else {
            Log.d("MyWarwick", "Delaying invocation of " + js);
            pendingInvocations.add(js);
        }
    }

    private void doInvoke(String js) {
        Log.d("MyWarwick", "Invoking " + js);
        webView.loadUrl("javascript:" + js);
    }

    /**
     * Check whether we've asked for some JS to get run but never
     * called ready(). This will happen either if there's a bug that
     * means we miss a call to ready(), or the page is loading at a
     * truly glacial pace. Either way is not good.
     */
    private Timer newStuckCheck() {
        if (stuckChecker != null) stuckChecker.cancel();
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!(pageReady || pendingInvocations.isEmpty())) {
                    Log.e("MyWarwick", pendingInvocations.size() + " JS calls are waiting to be invoked but the page has not said it's ready()!");
                    Log.d("MyWarwick", "Invoking now");
                    ready();
                }
            }
        }, 5000, 2000);
        return t;
    }
}
