

import net.yacy.kelondro.io.ByteCountInputStream;
import net.yacy.kelondro.io.ByteCountOutputStream;
import net.yacy.kelondro.util.MemoryControl;
import net.yacy.kelondro.workflow.WorkflowProcessor;
import de.anomic.http.server.RequestHeader;
import de.anomic.search.Segment;
import de.anomic.search.Segments;
import de.anomic.search.Switchboard;
import de.anomic.search.SwitchboardConstants;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class status_p {
    
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        Segment segment = null;
        if (post == null || !post.containsKey("html")) {
            prop.setLocalized(false);
            if (post.containsKey("segment") && sb.verifyAuthentication(header, false)) {
                segment = sb.indexSegments.segment(post.get("segment"));
            }
        }
        if (segment == null) segment = sb.indexSegments.segment(Segments.Process.PUBLIC);
        
        prop.put("rejected", "0");
        sb.updateMySeed();
        final int cacheMaxSize = (int) sb.getConfigLong(SwitchboardConstants.WORDCACHE_MAX_COUNT, 10000);
        prop.putNum("ppm", sb.currentPPM());
        prop.putNum("qpm", sb.peers.mySeed().getQPM());
        prop.put("wordCacheSize", Integer.toString(segment.termIndex().getBufferSize()));
        prop.put("wordCacheMaxSize", Integer.toString(cacheMaxSize));
		//
		// memory usage and system attributes
        prop.putNum("freeMemory", MemoryControl.free());
        prop.putNum("totalMemory", MemoryControl.total());
        prop.putNum("maxMemory", MemoryControl.maxMemory);
        prop.putNum("processors", WorkflowProcessor.availableCPU);

		// proxy traffic
		prop.put("trafficIn", ByteCountInputStream.getGlobalCount());
		prop.put("trafficProxy", ByteCountOutputStream.getAccountCount("PROXY"));
		prop.put("trafficCrawler", ByteCountInputStream.getAccountCount("CRAWLER"));

        // return rewrite properties
        return prop;
    }
    
}
