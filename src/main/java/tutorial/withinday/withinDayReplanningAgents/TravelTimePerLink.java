package tutorial.withinday.withinDayReplanningAgents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class TravelTimePerLink implements BasicEventHandler {
    private final static Logger log = Logger.getLogger(TravelTimePerLink.class);
    final Double MARGE = 0.01;
    private ACORGuidance ROUTER;
    private Scenario scenario;
    private int[] destination;
    //Pour l'analyse
    final Queue<Id<Link>> ATRACER = new ArrayDeque<>();
    final Double INTERVAL = 10.0 * 60.0;
    public Map<Id<Person>, Double> LTTs;
    public Map<Id<Link>, Queue<Double>> TVL;
    private HashMap<Id<Link>, HashMap<Id<Link>, Deque<Double[]>>> MultipleDATA;

    public TravelTimePerLink(Scenario sc, ACORGuidance router, int[] destiantions) {
        this.scenario = sc;
        LTTs = new HashMap<>();
        TVL = new HashMap<>();
        this.ROUTER = router;
        this.destination = destiantions;


        //ATRACER.add(Id.createLinkId("53"));
        ATRACER.add(Id.createLinkId("46"));

        Object[] links = ATRACER.toArray();
        this.MultipleDATA = new HashMap<>();
        for (int k = 0; k < this.destination.length; k++) {
            for (Object idl : links) {
                Deque<Double[]> donnek = new ArrayDeque<Double[]>();
                Double[] d0 = {0.0, 1.0};
                donnek.add(d0);
                HashMap<Id<Link>, Deque<Double[]>> evol = new HashMap<>();
                evol.put((Id<Link>) idl, donnek);

                MultipleDATA.put(Id.createLinkId(Integer.toString(this.destination[k])), evol);
            }
        }

    }

    public ACORGuidance getROUTER() {
        return ROUTER;
    }
    //Pour traver la vitesse relative sur un lien
    public void TracerEvolution(Double time, Id<Link> linkAtracer, Id<Link> destination, double vr) {
        for (Id<Link> id : this.MultipleDATA.keySet()) {
            Double TDerniereDonne = MultipleDATA.get(id).get(linkAtracer).getLast()[0];
            Double dernierPheromone = MultipleDATA.get(id).get(linkAtracer).getLast()[1];
            if (!(time - TDerniereDonne < INTERVAL)) {
                if (time - TDerniereDonne == INTERVAL) {
                    //Double Pheromone = this.ROUTER.getNetworks().get(destination).get(linkAtracer).getPheromone();
                    Double[] d = {time, vr};

                    this.MultipleDATA.get(id).get(linkAtracer).add(d);

                } else if ((1. < (time - TDerniereDonne) / INTERVAL)) {
                    int k = 0;
                    for (int m = 1; m < (int) ((time - TDerniereDonne) / INTERVAL); m++) {
                        //Double Pheromone = this.ROUTER.getNetworks().get(destination).get(linkAtracer).getPheromone();
                        Double[] di = {(TDerniereDonne + m * INTERVAL), vr};
                        this.MultipleDATA.get(id).get(linkAtracer).add(di);
                        k = m;
                    }
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        String SEP = new String("\\+");
        if (event instanceof PersonDepartureEvent) {
            Id<Person> ID = Id.createPersonId(((PersonDepartureEvent) event).getPersonId().toString().concat("+").concat(((PersonDepartureEvent) event).getLinkId().toString()));
            //Id<Person> ID = Id.createPersonId(((PersonDepartureEvent) event).getPersonId().toString().concat(((PersonDepartureEvent) event).getLinkId().toString()));
            LTTs.put(ID, ((PersonDepartureEvent) event).getTime());
        } else if (event instanceof LinkEnterEvent) {
            Id<Person> ID = Id.createPersonId(((LinkEnterEvent) event).getVehicleId().toString().concat("+").concat(((LinkEnterEvent) event).getLinkId().toString()));
            System.out.println(" [EVENT HANDLER ]TTIME PER LINK ENTRE DE " + ID + " AT " + ((LinkEnterEvent) event).getTime());
            LTTs.put(ID, ((LinkEnterEvent) event).getTime());

        } else if (event instanceof VehicleEntersTrafficEvent) {
            Id<Person> ID = Id.createPersonId(((VehicleEntersTrafficEvent) event).getVehicleId().toString().concat("+").concat(((VehicleEntersTrafficEvent) event).getLinkId().toString()));
            System.out.println("ID IN THE ENTRER :" + ID);
            LTTs.put(ID, ((VehicleEntersTrafficEvent) event).getTime());
        } else if (event instanceof LinkLeaveEvent) {

            Id<Person> ID = Id.createPersonId((((LinkLeaveEvent) event)).getVehicleId().toString().concat("+").concat(((((LinkLeaveEvent) event)).getLinkId().toString())));
            String[] infos = ID.toString().split(SEP, 3);
            if (LTTs.containsKey(ID)) {
                Id<Person> idp = Id.createPersonId((((LinkLeaveEvent) event)).getVehicleId().toString());
                Double Te = LTTs.get(ID);
                LTTs.put(ID, ((LinkLeaveEvent) event).getTime() - Te);
                if (this.ROUTER.PHEROMONEPUANT) {
                    //Id<Person> idp = Id.createPersonId((((LinkLeaveEvent) event)).getVehicleId().toString());
                    //Double NEGPHER = (LTTs.get(ID) / getPerfectTime((((LinkLeaveEvent) event)).getLinkId()) * -1.) + 1.;
                    if (!ROUTER.CONGESTION) {
                        //Id<Person> idp = Id.createPersonId((((LinkLeaveEvent) event)).getVehicleId().toString());
                        Double NEGPHER = this.getTauxVitessMax(idp, (((LinkLeaveEvent) event)).getLinkId());
                        String separateur = new String("\\+");
                        //On enleve pas de pheromone sur le lien d'arriver
                        if (!((((LinkLeaveEvent) event)).getVehicleId().toString().split(separateur)[1]).equals((((LinkLeaveEvent) event)).getLinkId().toString())) {
                            ROUTER.MAJPheromoneCongestionV2(ID, NEGPHER, ((LinkLeaveEvent) event).getTime());

                        }
                    }

                }
                if (ATRACER.contains((((LinkLeaveEvent) event)).getLinkId())) {
                    TracerEvolution((((LinkLeaveEvent) event)).getTime(), Id.createLinkId(infos[2]), Id.createLinkId(infos[1]), this.getRelativeVitesse(idp, (((LinkLeaveEvent) event)).getLinkId()));
                }
                //On compte pas le temps d'une fourmi sur son lien d'arrivé  (car on arrive pas à le controlé )
                if (!infos[1].equals(infos[2])) {
                    if (!TVL.containsKey((((LinkLeaveEvent) event)).getLinkId())) {
                        Queue<Double> q = new ArrayDeque<>();
                        q.add(LTTs.get(ID));
                        TVL.put((((LinkLeaveEvent) event)).getLinkId(), q);
                    } else if ((TVL.containsKey((((LinkLeaveEvent) event)).getLinkId()))) {
                        TVL.get((((LinkLeaveEvent) event)).getLinkId()).add(this.LTTs.get(ID));
                    }
                }
            }

        } else if (event instanceof VehicleLeavesTrafficEvent) {
            Id<Person> ID = Id.createPersonId(((VehicleLeavesTrafficEvent) event).getVehicleId().toString().concat("+").concat((((((VehicleLeavesTrafficEvent) event)).getLinkId().toString()))));
            String[] infos = ID.toString().split(SEP, 3);
            if (LTTs.containsKey(ID)) {
                Double Te = LTTs.get(ID);
                System.out.println(" [EVENT HANDLER ]TTIME PER LINK SORTIE  " + ID + " AT " + (((VehicleLeavesTrafficEvent) event).getTime()));
                LTTs.put(ID, (((VehicleLeavesTrafficEvent) event).getTime() - Te));
                Id<Person> idp = Id.createPersonId((((VehicleLeavesTrafficEvent) event)).getVehicleId().toString());
                if (this.ROUTER.PHEROMONEPUANT) {
                    String separateur = new String("\\+");

                    Double NEGPHER = this.getTauxVitessMax(idp, (((VehicleLeavesTrafficEvent) event)).getLinkId());
                    if (!ROUTER.CONGESTION)
                        if (!(idp.toString().split(separateur)[1]).equals((((VehicleLeavesTrafficEvent) event)).getLinkId().toString())) {
                            ROUTER.MAJPheromoneCongestionV2(ID, NEGPHER, ((VehicleLeavesTrafficEvent) event).getTime());
                        }
                }
                if (ATRACER.contains((((VehicleLeavesTrafficEvent) event)).getLinkId()))
                    TracerEvolution(((VehicleLeavesTrafficEvent) event).getTime(), Id.createLinkId(infos[2]), Id.createLinkId(infos[1]), this.getRelativeVitesse(idp, (((VehicleLeavesTrafficEvent) event)).getLinkId()));
            } else {
                throw new RuntimeException("should not happen; a car should always have to enter a link before leaving it");
            }

            //On compte pas le temps d'une fourmi sur son lien d'arrivé  (car on arrive pas à le controlé )
            if (!infos[1].equals(infos[2])) {
                if (!TVL.containsKey((((VehicleLeavesTrafficEvent) event)).getLinkId())) {
                    Queue<Double> q = new ArrayDeque<>();
                    q.add(LTTs.get(ID));
                    TVL.put((((VehicleLeavesTrafficEvent) event)).getLinkId(), q);
                } else if ((TVL.containsKey((((VehicleLeavesTrafficEvent) event)).getLinkId()))) {
                    TVL.get((((VehicleLeavesTrafficEvent) event)).getLinkId()).add(this.LTTs.get(ID));
                }
            }
        } else if (event instanceof PersonArrivalEvent) {

            Id<Person> ID = Id.createPersonId(((PersonArrivalEvent) event).getPersonId().toString().concat("+").concat((((((PersonArrivalEvent) event)).getLinkId().toString()))));
            String[] infos = ID.toString().split(SEP, 3);
            if (LTTs.containsKey(ID)) {
                Double Te = LTTs.get(ID);
                LTTs.put(ID, (((PersonArrivalEvent) event).getTime() - Te));
                if (this.ROUTER.PHEROMONEPUANT) {
                    Double NEGPHER = this.getTauxVitessMax(ID, (((PersonArrivalEvent) event)).getLinkId());
                    if (!ROUTER.CONGESTION) {
                        ROUTER.MAJPheromoneCongestionV2(ID, NEGPHER, ((PersonArrivalEvent) event).getTime());

                    }
                }
                   /* if (ATRACER.contains((((PersonArrivalEvent) event)).getLinkId())) {
                        TracerEvolution((((VehicleLeavesTrafficEvent) event)).getTime(), Id.createLinkId(infos[2]), Id.createLinkId(infos[1]));
                    }*/

                //On compte pas le temps d'une fourmi sur son lien d'arrivé  (car on arrive pas à le controlé )
                if (!infos[1].equals(infos[2])) {
                    if (!TVL.containsKey((((PersonArrivalEvent) event)).getLinkId())) {
                        Queue<Double> q = new ArrayDeque<>();
                        q.add(LTTs.get(ID));
                        TVL.put((((PersonArrivalEvent) event)).getLinkId(), q);
                    } else if ((TVL.containsKey((((PersonArrivalEvent) event)).getLinkId()))) {
                        TVL.get((((PersonArrivalEvent) event)).getLinkId()).add(this.LTTs.get(ID));
                    }
                }
            } else {
                throw new RuntimeException("should not happen; a car should always have to enter a link before leaving it");
            }
        }
    }

    public Double getPerfectTime(Id<Link> idl) {
        Double length = (scenario.getNetwork().getLinks().get(idl).getLength());
        Double sppedMAX = scenario.getNetwork().getLinks().get(idl).getFreespeed();
        return length / sppedMAX;
    }

    public Double getRelativeVitesse(Id<Person> ID, Id<Link> idL) {
        Double length = (scenario.getNetwork().getLinks().get(idL).getLength());
        Double sppedMAX = scenario.getNetwork().getLinks().get(idL).getFreespeed();
        Double TM = length / sppedMAX;
        Id<Person> C = Id.createPersonId((ID.toString()).concat("+").concat((idL.toString())));
        return (this.LTTs.get(C) / TM);
    }

    public Double getTauxVitessMax(Id<Person> ID, Id<Link> idL) {
        Double length = (scenario.getNetwork().getLinks().get(idL).getLength());
        Double sppedMAX = scenario.getNetwork().getLinks().get(idL).getFreespeed();
        Double TM = length / sppedMAX;
        Id<Person> C = Id.createPersonId((ID.toString()).concat("+").concat((idL.toString())));
        System.out.println("[TAUX DE CONGESTION ] ID DU LINK " + idL);
        System.out.println("[TAUX DE CONGESTION ] LE TEMPS PASSÉ sur le link : " + this.LTTs.get(C));
        System.out.println("[TAUX DE CONGESTION ] TAUX TOUT COURT TEMPPASS/TEMPS PARFAIT : " + (this.LTTs.get(C) / TM));
        System.out.println("[TAUX DE CONGESTION ] TAUX AVEC LA MARGE (VALEUR A ENLEVER) :" + ((-((this.LTTs.get(C)) / (TM * (1 + MARGE))) + 1.)));
        return (((-((this.LTTs.get(C)) / (TM * (1 + MARGE)))) + 1.));
    }

    @Override
    public void reset(int i) {
        this.LTTs.clear();
    }

    void CalculeTempsDeVoyageMoyenParLien() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("output/withindayexemple/TempsVoyageMoyenParLien.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashMap<Id<Link>, Double> TTPERLINKMOYEN = new HashMap();
        for (Id<Link> il : TVL.keySet()) {
            Object[] TTLS = TVL.get(il).toArray();
            int NB = TTLS.length;
            Double sum = 0.;
            for (int j = 0; j < TTLS.length; j++) {
                sum += (Double) TTLS[j];
            }
            TTPERLINKMOYEN.put(il, this.scenario.getNetwork().getLinks().get(il).getLength() / (sum / NB));
        }
        for (Id<Link> idl : TTPERLINKMOYEN.keySet()) {
            /*            if (!idl.equals(Id.createLinkId("24")) &&!idl.equals(Id.createLinkId("1")) ) {*/
            writer.println(idl + ";" + TTPERLINKMOYEN.get(idl));
            /*            }*/
        }
        writer.close();
    }

    public void ECRIRETRACE() {
        Object[] links = ATRACER.toArray();
        for (Object idl : links) {
            for (Id<Link> idd : this.MultipleDATA.keySet()) {
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter("output/withindayexemple/TraceVrelative" + idl.toString() + "D" + idd.toString() + ".csv");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Object[] data = this.MultipleDATA.get((Id<Link>) idd).get((Id<Link>) idl).toArray();
                //writer.println("TEMPS;VR");
                for (int j = 0; j < data.length; j++) {
                    Double[] cord = (Double[]) data[j];
                    writer.println(cord[0] + ";" + cord[1]);
                }
                writer.close();
            }
        }
    }

    void CalculeVitesseRelativeMoyenneParLien() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("output/withindayexemple/VitesseRelativeMoyenne.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashMap<Id<Link>, Double> TTPERLINKMOYEN = new HashMap();
        for (Id<Link> il : TVL.keySet()) {
            Object[] TTLS = TVL.get(il).toArray();
            int NB = TTLS.length;
            Double sum = 0.;
            for (int j = 0; j < TTLS.length; j++) {
                sum += (Double) TTLS[j];
            }

            TTPERLINKMOYEN.put(il, ((sum / NB) / getPerfectTime(il)));
        }
        Double FluditeMoyenne = 0.;
        int counter = 0;
        Double FLUDIEMAX = 1.;
        Id<Link> LienPeuFluide = Id.createLinkId("-1");
        for (Id<Link> idl : TTPERLINKMOYEN.keySet()) {
            counter++;
            if (FLUDIEMAX <= TTPERLINKMOYEN.get(idl)) {
                FLUDIEMAX = TTPERLINKMOYEN.get(idl);
                LienPeuFluide = idl;
            }
            FluditeMoyenne += TTPERLINKMOYEN.get(idl);
            writer.println(idl + ";" + TTPERLINKMOYEN.get(idl));
        }

        FluditeMoyenne = FluditeMoyenne / counter;
        System.out.println("----------- Le lien peu fluide c'est " + LienPeuFluide);
        System.out.println("----------- Le Pire Fludite " + FLUDIEMAX);
        writer.println("VITESSE RELATIVE DE TOUS LE RÉSSEAU " + FluditeMoyenne);
        System.out.println("VITESSE RELATIVE DE TOUS LE RÉSSEAU " + FluditeMoyenne);
        log.info("----------- Le lien peu fluide c'est " + LienPeuFluide);
        log.info("----------- Le Pire Fludite " + FLUDIEMAX);
        log.info("VITESSE RELATIVE DE TOUS LE RÉSSEAU " + FluditeMoyenne);
        log.info("VITESSE RELATIVE DE TOUS LE RÉSSEAU " + FluditeMoyenne);
        writer.close();
    }
}
