import types.Page;

public class PageWorker implements Runnable {
    /** The URL that we will be inspecting. */
    private String url;
    /** The client to be used used to fetch the page. */
    private HttpClient client;
    /** To track which pages we've been to, etc. */
    private InternetTracker tracker;

    public PageWorker(String url, HttpClient client, InternetTracker tracker) {
        this.url = url;
        this.client = client;
        this.tracker = tracker;
    }

    public void run() {
        if (url == null) {
            // No address, no work
            System.err.println("No URL specified for page");
            return;
        }

        Page page = client.fetchUrl(url);

        if (page != null) {
            tracker.urlVisited(url);
            parsePage(page);
        } else {
            tracker.urlNotFound(url);
        }
    }

    /**
     * Parses the page and extracts and links.
     *
     * @param page The page to parse.
     */
    private void parsePage(Page page) {
        tracker.addLinks(page.links);
    }
}
