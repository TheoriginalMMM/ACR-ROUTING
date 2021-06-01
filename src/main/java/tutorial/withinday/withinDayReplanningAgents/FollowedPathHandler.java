package tutorial.withinday.withinDayReplanningAgents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class FollowedPathHandler implements LinkEnterEventHandler {

    public HashMap<org.matsim.api.core.v01.Id<Person>,Queue<org.matsim.api.core.v01.Id<Link>>> paths ;
    private final static Logger log	=	Logger.getLogger(FollowedPathHandler.class);
    private Scenario sc;
    public FollowedPathHandler(Scenario sc){
        sc=sc;
        paths= new HashMap<>();
    }

    @Override
    public void reset(int iteration) {
        paths.clear();
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        if(! paths.containsKey(Id.createPersonId(linkEnterEvent.getVehicleId()))){
            Queue<org.matsim.api.core.v01.Id<Link>> q = new ArrayDeque<>();
            q.add(Id.createLinkId(linkEnterEvent.getLinkId()));
            paths.put(Id.createPersonId(linkEnterEvent.getVehicleId()),q);
        }
        else{
            paths.get(Id.createPersonId(linkEnterEvent.getVehicleId())).add(linkEnterEvent.getLinkId());
        }
    }
}
