//
//$LastChangedDate$
//$LastChangedRevision$
//$LastChangedBy$
//

import de.anomic.http.server.RequestHeader;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.server.servletProperties;

//dummy class
public class Help {

    public static servletProperties respond(final RequestHeader requestHeader, final serverObjects post, final serverSwitch env) {
        final servletProperties prop = new servletProperties();
        return prop;
    }
}
