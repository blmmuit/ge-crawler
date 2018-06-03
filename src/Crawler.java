import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Crawler {
    private ThreadPoolExecutor pool;
    private BlockingQueue workQueue;
    private InternetTracker tracker;
    private HttpClient client;

    /**
     * Starts the application.
     * @param args Specify the filename of the "internet" .json file on the
     * command line.  This will will be read and parsed/crawled.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: Crawler <internet .json filename>");
            return;
        }

        Crawler crawler = new Crawler(args[0]);
        crawler.crawl();
    }

    /**
     * Creates a new Crawler, specifying which internet to use.
     *
     * @param internetName the name of the json file that contains the fake
     * internet to be used by the Crawler.
     */
    public Crawler(String internetName) {
        // Set up a pool with exactly 5 worker threads, and a work queue that
        // can hold up to 10 items.  Real app would read these settings from
        // config
        workQueue = new ArrayBlockingQueue(10);
        pool = new ThreadPoolExecutor(5, 5, 10L, TimeUnit.SECONDS, workQueue);
        pool.prestartAllCoreThreads();

        // These will be used by all the PageWorkers, so we create them here so
        // we can inject them later.
        tracker = new InternetTracker();
        client = new HttpClient(internetName);
    }

    /**
     * Begin crawling the internet.
     */
    public void crawl() {
        // System.out.println("Crawling...");
        String startingPageUrl = client.getStartingAddress();
        pool.submit(new PageWorker(startingPageUrl, client, tracker));

        // For some reason, the ThreadPoolExecutor is horrible about telling us
        // when it's done with all of its work, so we have to track manually.
        // We'll increment this counter when we submit work, and rely on the
        // pool's guess about how many tasks completed to determine when all
        // the work is done.
        int submittedWorkers = 1;

        // We also need to consider the tracker.  If the starting page (above)
        // completes before we get to the loop, the loop won't even start to
        // execute if we don't check to see whether there are also URLs still
        // to visit.
        while ((pool.getCompletedTaskCount() < submittedWorkers) || tracker.hasWork()) {
            // System.out.println("In main work loop");

            // This delay (1s) corresponds to the time the loop will wait once
            // all submitted tasks are complete, AND the tracker doesn't have
            // any more URLs to process.  Too long and the application will
            // take a while to shut down once all the work is compete.  Too
            // short, and we spin on the CPU.
            String nextUrl = tracker.getNextUrl(1000L);
            if (nextUrl != null) {
                pool.submit(new PageWorker(nextUrl, client, tracker));
                submittedWorkers++;
            }
        }
        // System.out.println("All work is complete");

        // All done.  Terminate thread pool
        stop();

        // Print results for the test
        tracker.printStats();
    }

    /**
     * Stop crawling.
     */
    public void stop() {
        pool.shutdown();

        try {
            // wait 5s for the pool to end.  It shouldn't have any active work
            // so there's no reasonable excuse for this to fail.
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                // But it did fail.  Force shutdown.
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
        }
    }
}

