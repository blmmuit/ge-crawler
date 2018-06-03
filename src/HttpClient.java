import types.Internet;
import types.Page;

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This pretends to be a class that goes out to the web and retrieves a page.
 * It actually just reads the json internet file and uses those as canned responses.
 */

public class HttpClient {
    private Internet internet;
    private Gson gson;

    public HttpClient(String filename) {
        gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            internet = gson.fromJson(new FileReader(filename), Internet.class);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Assuming the internet is down.  This will be quick.");
            internet = new Internet();
        }

        // For debugging
        // System.out.println(gson.toJson(internet));
    }

    /**
     * A real client wouldn't have this method, of course.  The starting URL
     * would probably be specified in a config file somewhere.  However, we
     * want to use the first page in the json file as the starting URL, so we
     * just use this method to pull it out.
     *
     * @return the address of the first page of the internet.  Returns null if
     * there is no internet (the .json couldn't be loaded).
     */
    public String getStartingAddress() {
        Page firstPage = internet.pages[0];
        if (firstPage != null) {
            return firstPage.address;
        } else {
            return null;
        }
    }

    /**
     * Fetches the given URL from the internet.
     * @param url The address of the page to fetch
     * @return The Page corresponding to the provided address.
     */
    public Page fetchUrl(String url) {
        // Rather than actually doing an HTTP GET, we just look up the Page in the Internet...
        for (Page page : internet.pages) {
            if (url.equals(page.address)) {
                // System.out.println("Found page " + page);
                return page;
            }
        }

        // System.err.println("No page for URL " + url);
        return null;
    }
}
