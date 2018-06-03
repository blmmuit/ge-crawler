import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class does tracking of which pages have been viewed and parsed.  In the
 * real world, this would probably be backed by a database of some variety.
 *
 * It acts as part of the producer/consumer model.  The Crawler consumes
 * pending addresses, and the PageWorker produces new addresses.  This acts as
 * a holding area.
 */
public class InternetTracker {
    private Set<String> visited;
    private List<String> pending;
    private Set<String> duplicateAddresses;
    private Set<String> badAddresses;

    public InternetTracker() {
        // Wrap these up to allow thread-safe access.  We don't need anything
        // too fancy, just a way to make sure that when we add new elements to
        // these sets, we do so safely.
        visited = Collections.synchronizedSet(new HashSet());
        duplicateAddresses = Collections.synchronizedSet(new HashSet());
        badAddresses = Collections.synchronizedSet(new HashSet());

        // We'll do manual synchronization on the list of pending addresses,
        // since we're going to allow a read with timeout on the list within
        // getNextUrl
        pending = new ArrayList();
    }

    /**
     * Fetches the next URL to visit.  Since threads may be processing data in
     * the background, there may be no URLs when the method is called.
     * The timeout specifies how long to wait for background threads to make
     * new URLs available.
     *
     * @param timeout The maximum duration to wait for an address, in
     * milliseconds.
     *
     * @return An address to be crawled, or null if none are currently known.
     */
    public synchronized String getNextUrl(long timeout) {
        String url = null;

        // Check for more work.  If none, wait...
        if (pending.isEmpty()) {
            try {
                wait(timeout);
            } catch (InterruptedException ignored) {
                // Do nothing, we don't care
            }

            // Check for work again after we waited
            if (!pending.isEmpty()) {
                url = pending.remove(0);
            }
        } else {
            url = pending.remove(0);
        }

        notifyAll();
        // System.out.println("Next URL = " + url);
        return url;
    }

    /**
     * Are there any URLs still waiting to be visited?
     *
     * @return true if there are
     */
    public synchronized boolean hasWork() {
        // System.out.println("Pending URLs: " + pending);
        return !pending.isEmpty();
    }

    /**
     * Inform the tracker that a given URL was visited for crawling.
     *
     * @param url The address that was visited.
     */
    public void urlVisited(String url) {
        visited.add(url);
    }

    /**
     * Inform the tracker that a given URL wasn't reachable.
     *
     * @param url The address that didn't seem to be valid
     */
    public void urlNotFound(String url) {
        badAddresses.add(url);
    }

    /**
     * Adds the provided array of links to the set of pages to crawl.
     * Synchronized, to provide thread safe access to the list of
     * pending addresses.
     *
     * @param links An array of addresses
     */
    public synchronized void addLinks(String[] links) {
        boolean addedLinks = false;

        // System.out.println("Adding links: " + links);

        for (String link : links) {
            // We don't want to re-add links we already visited to the pending
            // list.  If the link was a duplicated, make sure it's recorded as
            // such.  If the URL is known bad, silently skip it.
            if (visited.contains(link)) {
                // Track duplicates.  A real system might track the number of
                // links pointing to this address to determine popularity.
                duplicateAddresses.add(link);
            } else if (!badAddresses.contains(link)) {
                pending.add(link);
                addedLinks = true;
            }
        }

        if (addedLinks) {
            notifyAll();
        }
    }

    /**
     * Prints crawling stats as required by the exercise.
     */
    public void printStats() {
        System.out.println("Success:");
        System.out.println(visited);

        System.out.println("\nSkipped:");
        System.out.println(duplicateAddresses);

        System.out.println("\nError:");
        System.out.println(badAddresses);
    }
}
