package Network;

import android.os.Handler;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by linux on 14/3/15.
 */
public abstract class AsyncTask<Params, Progress, Result> {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAXIMUM_POOL_SIZE = 128;
    private static final int KEEP_ALIVE = 1;

    private static final BlockingQueue<Runnable> sWorkQueue = new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "AsyncTask #" + mCount.getAndIncrement());
        }
    };
    private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            sWorkQueue, sThreadFactory);

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;
    private static final int MESSAGE_POST_CANCEL = 0x3;

    private static final InternalHandler sHandler = new InternalHandler();

    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;

    private volatile Status mStatus = Status.PENDING;

    public enum Status {
        PENDING, RUNNING, FINISHED;
    }

    public AsyncTask() {
        mWorker = new WorkerRunnable<Params, Result> {
            @Override
            public Result call ()throws Exception {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                return doInBackground(mParams);
            }
        } ;

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                Message message;
                Result result = null;
                try {
                    result = get();
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(
                            "An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    message = sHandler.obtainMessage(MESSAGE_POST_CANCEL,
                            new AsyncTaskResult<Result>(AsyncTask.this,
                                    (Result[]) null));
                    message.sendToTarget();
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException(
                            "An error occured while executing "
                                    + "doInBackground()", t);
                }
                message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                        new AsyncTaskResult<Result>(AsyncTask.this, result));
                message.sendToTarget();
            }

        };
    }

    public final Status getStatus(){
        return mStatus;
    }
    protected abstract Result doInBackground(Params... params);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }
    protected void onProgressUpdate(Progress... values) {
    }
    public final boolean isCancelled() {
        return mFuture.isCancelled();
    }
    protected void onCancelled(){
    }
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }
    public final Result get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }


    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        sExecutor.execute(mFuture);

        return this;
    }








    private static abstract class WorkerRunnable<Params, Result> implements
            Callable<Result> {
        Params[] mParams;
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {
        final AsyncTask mTask;
        final Data[] mData;

        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }

    private static class InternalHandler extends Handler {
        @SuppressWarnings({ "unchecked", "RawUseOfParameterizedType" })
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
                case MESSAGE_POST_CANCEL:
                    result.mTask.onCancelled();
                    break;
            }
        }
    }


}
