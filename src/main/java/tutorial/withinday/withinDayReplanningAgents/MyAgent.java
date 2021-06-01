package tutorial.withinday.withinDayReplanningAgents;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayDeque;
import java.util.Queue;


public class MyAgent implements MobsimDriverAgent {
    final static Logger log = Logger.getLogger("MyAgent");

    final private ACORGuidance guidance;
    private MobsimVehicle vehicle;
    private final Scenario sc;
    private Id<Link> currentLinkId;
    private final Id<Person> myId;
    private State state = State.ACTIVITY;
    private final  Netsim netsim;
    private final Id<Link> destinationLinkId;
    private final Id<Vehicle> plannedVehicleId;
    private final  Double startTime;

    private Queue<Id<Node>> LienParcouriAvant;

    private double activityEndTime = 1.;

    public MyAgent(ACORGuidance guidance, Scenario sc, EventsManager ev, Netsim netsim, Id<Link> startLinkId, Id<Link> End, MobsimVehicle veh, Id<Person> Id, Double time) {
        System.out.println("DEBUT CONSTRUCTEUR AGENT ");
        log.info("calling MyAgent");
        log.info("CONSTRUCTEUR DE MY AGENT ");
        this.guidance = guidance;
        this.sc = sc;
        this.myId = Id;
        this.netsim = netsim;
        this.currentLinkId = startLinkId;
        this.plannedVehicleId = veh.getId();
        this.destinationLinkId = End;
        this.startTime = time;
        this.LienParcouriAvant = new ArrayDeque<>();
        //LienParcouriAvant.add((org.matsim.api.core.v01.Id<Node>) sc.getNetwork().getLinks().get(currentLinkId).getFromNode().getId());
    }

    @Override
    public void setStateToAbort(double now) {
        this.state = State.ABORT;
        log.info("calling abort; setting state to: " + this.state);
    }

    //À modifier
    @Override
    public void endActivityAndComputeNextState(double now) {
        this.state = State.LEG; // want to move
        log.info("calling endActivityAndComputeNextState; setting state to: " + this.state);
    }

    @Override
    public void endLegAndComputeNextState(double now) {
        this.state = State.ABORT;
        this.activityEndTime = Double.POSITIVE_INFINITY;
        log.info("calling endLegAndComputeNextState; setting state to: " + this.state);
    }

    @Override
    public double getActivityEndTime() {
        log.info("calling getActivityEndTime; answer: " + this.activityEndTime);
        return this.startTime;
        //return this.activityEndTime;
    }

    @Override
    public Double getExpectedTravelTime() {
        return 0.;  // what does this matter for?
    }

    @Override
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
    public String getMode() {
        return TransportMode.car; // either car or nothing
    }

    @Override
    public State getState() {
        log.info("calling getState; answer: " + this.state);
        return this.state;
    }

    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
        //this.LienParcouriAvant=this.currentLinkId;
        this.currentLinkId = linkId;
    }

    @Override
    public Id<Link> getCurrentLinkId() {
        return this.currentLinkId;
    }

    @Override
    public Id<Link> getDestinationLinkId() {
        return this.destinationLinkId;
    }

    @Override
    public Id<Person> getId() {
        return this.myId;
    }

    @Override
    public Id<Link> chooseNextLinkId() {
        Link currentLink = sc.getNetwork().getLinks().get(this.currentLinkId);
        if (!currentLinkId.equals(this.destinationLinkId)) {
            if (this.netsim.getSimTimer().getTimeOfDay() < 14. * 3600) {
                System.out.println("BEFOR CHOOS NEXT LINK ID My id " + this.myId + "CURENT " + this.currentLinkId);
                Id<Link> NexDestination = this.guidance.choosNextLinkIdV2(this.currentLinkId, this.destinationLinkId);
                if (!NexDestination.equals(destinationLinkId)) {
                    int essaieAutreLink = 0;

                    while (LienParcouriAvant.contains((org.matsim.api.core.v01.Id<Node>) sc.getNetwork().getLinks().get(NexDestination).getToNode().getId()) &&
                            essaieAutreLink <= 5) {
                        System.out.println("FAIRE UN RETOUR C'EST INTERDIT J'ESSAIE UNE AUTRE FOIS ");
                        NexDestination = this.guidance.choosNextLinkIdV2(this.currentLinkId, this.destinationLinkId);
                        essaieAutreLink++;
                    }
                    //Si eche on doit faire un retour
                    if (5 <= essaieAutreLink) {
                        //On lui permet de retourner la ou il était avant une seul fois
                        if (LienParcouriAvant.size() == 1) {
                            System.out.println("JE FAIS MON PREMIER  RETOUR PAR AGENT");
                            return NexDestination;
                        } else if (2 <= LienParcouriAvant.size()) { //ICI L'AGENT A DEJA FAIT UN RETOUR ET IL PEUT PAS FAIRE UN DEUXIEMME RETOUR JUSTE APRÉS C'EST PAS RÉALISTE
                            //On  diminue les choix possibles
                            Id<Node> interdit = (Id<Node>) LienParcouriAvant.toArray()[LienParcouriAvant.size() - 1];
                            NexDestination = this.guidance.choosBestNextLinkIdV2(this.currentLinkId, this.destinationLinkId, interdit);
                            return NexDestination;
                        }
                    }
                }
                return NexDestination;
            }
            return null;
        } else {
            this.endLegAndComputeNextState(this.netsim.getSimTimer().getTimeOfDay());
            log.info(" MMM PERSONNE de la part de l'agent  " + this.myId + " ARRIVE A SA DISTINATION " + this.destinationLinkId);
            return null;
        }
    }

    @Override
    public Id<Vehicle> getPlannedVehicleId() {
        return this.plannedVehicleId;
    }

    @Override
    public MobsimVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public void setVehicle(MobsimVehicle veh) {
        this.vehicle = veh;
    }

    @Override
    public void notifyMoveOverNode(Id<Link> newLinkId) {
        //Un agent retourne sur un noeud ou il était a l'etape d'avant
        if (newLinkId.toString().equals(destinationLinkId.toString())) {
            this.guidance.TTS.MAJTTS(this.myId, netsim.getSimTimer().getTimeOfDay());
            this.guidance.MAJPheromoneOnArriver(this.myId, this.destinationLinkId,netsim.getSimTimer().getTimeOfDay());
        } else {
            if (LienParcouriAvant.contains(sc.getNetwork().getLinks().get(newLinkId).getToNode().getId())) {
                this.LienParcouriAvant.add(sc.getNetwork().getLinks().get(newLinkId).getFromNode().getId());
            } else {
/*            if (LienParcouriAvant.size()==2) {
                LienParcouriAvant.poll();
            }*/
                this.LienParcouriAvant.poll();
                this.LienParcouriAvant.add(sc.getNetwork().getLinks().get(newLinkId).getFromNode().getId());
            }
            this.currentLinkId = newLinkId;
        }
    }
        @Override
        public boolean isWantingToArriveOnCurrentLink () {
            return false;
        }

        @Override
        public Facility<? extends Facility<?>> getCurrentFacility () {
            // TODO Auto-generated method stub
            throw new RuntimeException("not implemented");
        }

        @Override
        public Facility<? extends Facility<?>> getDestinationFacility () {
            // TODO Auto-generated method stub
            throw new RuntimeException("not implemented");
        }

    }

