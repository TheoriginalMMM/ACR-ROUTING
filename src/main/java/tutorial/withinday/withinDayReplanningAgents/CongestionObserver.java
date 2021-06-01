package tutorial.withinday.withinDayReplanningAgents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CongestionObserver implements BasicEventHandler {
    Map<Id<Link>, Double[]> nVehs = new HashMap<>();
    private Scenario scenario;
    private ACORGuidance ROUTER;
    private int [] destination;
    final Double Occupation=0.01;

    public CongestionObserver(Scenario sc, ACORGuidance router, int [] destination) {
        this.scenario = sc;
        this.ROUTER=router;
        this.destination=destination;
    }
    public void MaxAgentsPerLink(){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("output/withindayexemple/MaxAgentParLien.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("MAX AGENT PER LINK IN THE SAME TIME ");
        System.out.println("LINK ID   NOMBRE D'AGENT AU MEME TEMPS SUR CE LIEN");
        for(Id<Link> idl : this.nVehs.keySet()){
            System.out.println(idl+" "+this.nVehs.get(idl)[1]+";"+this.nVehs.get(idl)[0]);
            writer.println(idl+";"+this.nVehs.get(idl)[1]+";"+this.nVehs.get(idl)[0]);
        }
        writer.close();
    }

    Double congestionLevel(Id<Link> linkId) {

        if (!nVehs.containsKey(linkId)) {
            return 0.;
        }
        final Double[] nn = nVehs.get(linkId);
        final Link link = this.scenario.getNetwork().getLinks().get(linkId);
        Double nLanes = link.getNumberOfLanes();
        Double length = link.getLength();
        Double estimStorCap = nLanes * length/ 7.50; // estimated storage capacity
        System.out.println("NOMBRE DE VHES SUR LE LIEN "+linkId + "a ce moment est "+nn[0]);
        System.out.println("STOR CAP OF LINK : "+linkId+" EST DE : "+estimStorCap);
        System.out.println("(Occupation*estimStorCap) Seuil a dépasser pour enlever de la phéromone :"+(Occupation*estimStorCap));
        return (nn[0] / (Occupation*estimStorCap));
    }

    @Override
    public void reset(int iteration) {
        nVehs.clear();
    }

  /* public void leaveDistination(Id<Link> dest){
        System.out.println("JE FORCE LA SORTIE SUR LES EVENTS HANDLER");
        Double [] vals =this.nVehs.get(dest);
        vals[0]=vals[0]-1;
        this.nVehs.put(dest,vals);
        if(this.ROUTER.PHEROMONEPUANT) {
           System.out.println(" [CongestionObserver] CONGISTION LEVEL  : "+congestionLevel(dest));
           Double NEGPHER = ((congestionLevel(dest) *-1.)+1.);
           System.out.println("[CongestionObserver] CONGISTION LEVEL SUR LE LINK  :"+dest +" EST DE "+NEGPHER);
           if(ROUTER.CONGESTION) {
               ROUTER.MAJPheromoneCongestionV2(dest, NEGPHER);
           }
       }
   }*/

    @Override
    public void handleEvent(Event event) {
        Id<Link> linkId = null;

        if (event instanceof LinkEnterEvent) {
            linkId = ((LinkEnterEvent) event).getLinkId();
            if (nVehs.get(linkId) != null) {
                Double[] val = nVehs.get(linkId);
                val[0] = val[0] + 1.;
                if (val[0] > val[1]) {
                    val[1] = val[0];
                }
                nVehs.put(linkId, val);
            } else {
                Double[] Vals = new Double[2];
                Vals[0] = 1.0;
                Vals[1] = 1.0;
                nVehs.put(linkId, Vals);
            }
        }
        if (event instanceof VehicleEntersTrafficEvent) {
            linkId = ((VehicleEntersTrafficEvent) event).getLinkId();
            if (nVehs.get(linkId) != null) {
                Double[] val = nVehs.get(linkId);
                //System.out.println("@@@@@ VAL 0 : " + val[0] + " @@@@@ ET VAL 1 " + val[1]);
                val[0] = val[0] + 1.;
                if (val[0] > val[1]) {
                    System.out.println("JE RENTRE AU MOINS DANS CETTE PARTIE DU CODE ET VAL IS :" + val[0]);
                    val[1] = val[0];
                }
                nVehs.put(linkId, val);
            } else {
                Double[] Vals = new Double[2];
                Vals[0] = 1.0;
                Vals[1] = 1.0;
                nVehs.put(linkId, Vals);
            }
        }
        if (event instanceof LinkLeaveEvent) {
            //MAJ PHEROMONE EN FONCTION VITESSE
            //T=D/V
            //Temps passé
            //scenario.getNetwork().getLinks().get(LinkLeaveEvent) event).
            //
            linkId = ((LinkLeaveEvent) event).getLinkId();

            if (nVehs.get(linkId) != null) {
                Double[] val = nVehs.get(linkId);
                val[0] = val[0] - 1;
                nVehs.put(linkId, val);
                if(this.ROUTER.PHEROMONEPUANT) {
                    String separateur= new String("\\+");
                    String idd=( ((LinkLeaveEvent)event).getVehicleId().toString()).split(separateur)[1];
                    Id<Person> ID= Id.createPersonId(((LinkLeaveEvent)event).getVehicleId().toString().concat("+".concat(linkId.toString())));
                    System.out.println(" [CongestionObserver] CONGISTION LEVEL  : "+congestionLevel((((LinkLeaveEvent) event)).getLinkId()));
                    Double NEGPHER = ((congestionLevel((((LinkLeaveEvent) event)).getLinkId()) *-1) +1.);
                    System.out.println("[CongestionObserver] CONGISTION LEVEL SUR LE LINK  :"+(((LinkLeaveEvent) event)).getLinkId() +" EST DE "+NEGPHER);
                    if(ROUTER.CONGESTION) {
                        if(!linkId.toString().equals(idd))
                            ROUTER.MAJPheromoneCongestionV2(ID, NEGPHER,((LinkLeaveEvent)event).getTime());

                    }
                }
            } else {
                throw new RuntimeException("should not happen; a car should always have to enter a link before leaving it");
            }
        }
        if (event instanceof VehicleLeavesTrafficEvent) {
            linkId = ((VehicleLeavesTrafficEvent) event).getLinkId();
            if(linkId.toString().equals("30")){
                System.out.println("On SORT DU LIEN DE DESTINATION PEU PROBABLE");
            }
            if (nVehs.get(linkId) != null) {
                Double[] val = nVehs.get(linkId);
                val[0] = val[0] - 1;
                nVehs.put(linkId, val);
                if(this.ROUTER.PHEROMONEPUANT) {
                    String separateur= new String("\\+");
                    String idd= ((VehicleLeavesTrafficEvent)event).getVehicleId().toString().split(separateur)[1];
                    Id<Person> ID= Id.createPersonId(((VehicleLeavesTrafficEvent)event).getVehicleId().toString().concat("+".concat(linkId.toString())));
                    System.out.println(" [CongestionObserver] CONGISTION LEVEL  : "+congestionLevel((((VehicleLeavesTrafficEvent) event)).getLinkId()));
                    Double NEGPHER = (congestionLevel((((VehicleLeavesTrafficEvent) event)).getLinkId()) *-1.)+1.;
                    System.out.println("[CongestionObserver] CONGISTION LEVEL SUR LE LINK  :"+(((VehicleLeavesTrafficEvent) event)).getLinkId() +"EST DE "+NEGPHER);
                    if(ROUTER.CONGESTION) {
                        if(!linkId.toString().equals(idd))
                            ROUTER.MAJPheromoneCongestionV2(ID, NEGPHER,((VehicleLeavesTrafficEvent)event).getTime());
                    }

                }
            } else {
                throw new RuntimeException("should not happen; a car should always have to enter a link before leaving it");
            }
        }
    }
}
