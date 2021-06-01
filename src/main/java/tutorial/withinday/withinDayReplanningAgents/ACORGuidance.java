package tutorial.withinday.withinDayReplanningAgents;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Queue;

public class ACORGuidance {
    // Pour choisir d'activer ou pas le pheromone répulsive
    static final Boolean PHEROMONEPUANT =true;
    //Paramatre pour choirisr quelle version de pheromone répuslive
    //TRUE : pheromone en fonction de nombre e vehucule sur le lien
    //False : pheromone en fonction du temps passé sur le lien
    static final Boolean CONGESTION =true;
    static final Boolean FRTAXU = false;
    final double EVAP = 0.001; // Taux d'evaporisation
    final Double ALPHA = 3.0;
    final Double BETA = 3.0;
    final Double RF0 = 1.;
    public HashMap<Id<Link>, AcoLink> RFN = new HashMap<>();
    public Boolean MultiDest;
    public TTSHandler TTS;
    final  Scenario scenario;
    final private FollowedPathHandler FLOWEDPATHES;
    public int [] distinations;
    final private HashMap<Id<Link>, HashMap<Id<Link>, AcoLink>> Networks = new HashMap<>();
    //Pour  Tracer L'EVOLUTION DES VALUERS DE PHEROMONES SUR CERTAINS LIEN
    final private HashMap<Id<Link>, HashMap<Id<Link>, Deque<Double[]>>> MultipleDATA;
    public Queue<Id<Link>> ATRACER = new ArrayDeque<>();
    //ON PREND UNE VALEUR DANS CHAQUE INTERVALLE DE TEMPS
    final Double INTERVAL = 60.*10.;



    public HashMap<Id<Link>, HashMap<Id<Link>, AcoLink>> getNetworks() {
        return Networks;
    }



    public ACORGuidance(FollowedPathHandler FLOWEDPATHESS, TTSHandler tts, Scenario sc, Boolean MultiDistinations, int[] distinations) {
        this.scenario = sc;
        this.FLOWEDPATHES = FLOWEDPATHESS;
        this.TTS = tts;
        this.MultiDest = MultiDistinations;
        this.distinations=distinations;
        if (MultiDistinations) {
            if (PHEROMONEPUANT)
                this.RFN = InitilatisationNetwork(true);
            for (int distination : distinations) {
                Networks.put(Id.createLinkId(String.valueOf(distination)), InitilatisationNetwork(false));
            }
        } else {
            for (int distination : distinations) {
                Networks.put(Id.createLinkId(String.valueOf(distination)), InitilatisationNetwork(MultiDistinations));
            }
        }
        //ICI CHOISIR QUELLE LIEN ON VEUT TRACER SES VALEURS DE PHEROMONES
        ATRACER.add(Id.createLinkId("46"));

        Object[] links = ATRACER.toArray();
        this.MultipleDATA = new HashMap<>();
        for (int distination : this.distinations) {
            for (Object idl : links) {
                System.out.println("ID LINK A TRACER" +  idl);
                Deque<Double[]> donnek = new ArrayDeque<>();
                Double[] d0 = {0.0, 0.2};
                donnek.add(d0);
                HashMap<Id<Link>, Deque<Double[]>> evol = new HashMap<>();
                evol.put((Id<Link>) idl, donnek);

                MultipleDATA.put(Id.createLinkId(Integer.toString(distination)), evol);
            }
        }
    }

    static Id<Link> SimpleRouletWheelV2(Object[] choix, Double[] Probas) {
        double nb = Math.random();
        boolean end = false;
        System.out.println(" [ SimpleRouletWheel] nb " + nb);
        Double PC = Probas[0];
        Id<Link> resultats = (Id<Link>) choix[0];
        int INDEXCHOIX = 0;
        int i = 0;
        while (!end && i < choix.length) {
            if (nb < PC) {
                resultats = (Id<Link>) choix[i];
                INDEXCHOIX = i;
                end = true;
            } else {
                PC += Probas[i + 1];
            }
            i++;
        }
        System.out.println("[Simple roulett wheel V2] CHOOSE THE LINK V2 DISTINATION " + resultats + " WITH PROBABILITE OF " + Probas[INDEXCHOIX]);
        return resultats;
    }

    Double CalculerDistance(Id<Link> Destination, Id<Link> curent) {
        //Noued à la fin du link a choisir
        Coord B = this.scenario.getNetwork().getLinks().get(curent).getToNode().getCoord();
        //Noeud à acctuelle
        Coord b = this.scenario.getNetwork().getLinks().get(curent).getFromNode().getCoord();
        //Noeud destination
        Coord A = this.scenario.getNetwork().getLinks().get(Destination).getFromNode().getCoord();
        if (curent.equals(Destination))
            return 0.1;
        Double DistanceCENTRE = (Math.pow(Math.pow(((B.getX() + b.getX()) / 2.0) - A.getX(), 2) + Math.pow(((B.getY() + b.getY()) / 2.0) - A.getY(), 2), 0.5));
        Double distance = (Math.pow(Math.pow(B.getX() - A.getX(), 2) + Math.pow(B.getY() - A.getY(), 2), 0.5));
        //return (distance==0?0.1:distance);
        return (distance == 0 ? DistanceCENTRE : distance);
        //return Math.pow(Math.pow(((B.getX() + b.getX()) / 2) - A.getX(), 2) + Math.pow(((B.getY() + b.getY()) / 2) - A.getY(), 2), 0.5);
    }

    HashMap<Id<Link>, AcoLink> InitilatisationNetwork(Boolean RF) {
        HashMap<Id<Link>, Double> Shemin = new HashMap<>();
        HashMap<Id<Link>, AcoLink> Network = new HashMap<>();
        int SIZEExpected = this.scenario.getNetwork().getLinks().values().size();
        for (Link l : this.scenario.getNetwork().getLinks().values()) {
            Object[] outLinks = l.getToNode().getOutLinks().keySet().toArray();
            double NB = outLinks.length;
            for (double i = 0.0; i < NB; i++) {
                Id<Link> id = (Id<Link>) outLinks[(int) i];
                //AcoLink acoL = new AcoLink(NB);
                if (!Network.keySet().contains(id)) {
                    //Link sl = this.scenario.getNetwork().getLinks().get(id);
                    AcoLink nl;
                    if (RF) {
                        nl = new AcoLink(RF0);
                        //this.Network.put(id, nl);
                    } else {
                        nl = new AcoLink();
                    }

                    Network.put(id, nl);
                }
            }
        }
        assert (Network.keySet().size() == SIZEExpected);
        return Network;
    }
    //MAJ des valeurs de phéromones à la sortie de chaque lien
    void MAJPheromoneCongestionV2(Id<Person> ID, Double diff,Double time ) {
        String suparetor = "\\+";
        String[] Key = ID.toString().split(suparetor);
        Id<Link> Idl = Id.createLinkId(Key[2]);
        Id<Link> Dest = Id.createLinkId(Key[1]);

        Node fromNoued = this.scenario.getNetwork().getLinks().get(Idl).getFromNode();
        System.out.println("[MAJPheromoneCongestion] PHEROMONE NEGATIVE QU'ON VA ENLEVER SUR LE LINKD " + Idl + " :" + Math.pow(diff, 1));
        if (diff <= 0) {
            if (!MultiDest) {
                // Sans evap
                this.Networks.get(Dest).get(Idl).AddDeltaF(diff, 0.);
                //Si on veut tester d'integré l'evaporisation pour la phéromone répulsif
                //this.Networks.get(Dest).get(Idl).AddDeltaF(diff, EVAP);
                if(ATRACER.contains(Idl))
                    TracerEvolution(time,Idl,Dest);
                System.out.println("[MAJPheromoneCongestion] PHEROMONE  SUR LE LINKD  Apres " + Idl + " :" + this.Networks.get(Dest).get(Idl).getPheromone());
            } else {
                    for (Id<Link> id : Networks.keySet()) {
                        this.Networks.get(id).get(Idl).AddDeltaF(diff, 0.);
                        if(ATRACER.contains(Idl))
                            TracerEvolution(time,Idl,id);

                }
                System.out.println("[MAJPheromoneCongestion] PHEROMONE REPULSIF SUR LE LINKD  Apres " + Idl + " :" + this.RFN.get(Idl).getPheromone());
            }
        }
    }

    void MAJPheromoneOnArriver(Id<Person> idp, Id<Link> Dest, Double time) {
        Object[] PATH = FLOWEDPATHES.paths.get(idp).toArray();
        System.out.println("PATH " + PATH);
        System.out.println("[MAJPheromoneOnArriver] TTS DE LA PERSONNE " + this.TTS.TTSs.get(idp));
        System.out.println("[MAJPheromoneOnArriver] ------INVERSSE DU TTS" + 1 / this.TTS.TTSs.get(idp));
        System.out.println("[MAJPheromoneOnArriver] ------Valeur ajoué quand on arrive sans le ALPHA  :" + Math.pow(10 * 1.0 / this.TTS.TTSs.get(idp), 1));
        System.out.println("[MAJPheromoneOnArriver] ------Valeur ajoué quand on arrive :" + Math.pow(10 * 1.0 / this.TTS.TTSs.get(idp), 1));

        if (EVAP == 0.) {
            for (int i = 0; i < PATH.length; i++) {
                this.Networks.get(Dest).get((Id<Link>) PATH[i]).AddDeltaF(1 / this.TTS.TTSs.get(idp), EVAP);
                //this.Networks.get(Dest).get((Id<Link>) PATH[i]).setPheromoneAMD(1 / this.TTS.TTSs.get(idp));
                System.out.println(" PATH : " + (Id<Link>) PATH[i] + "NEW PHEROMONE " + this.Networks.get(Dest).get((Id<Link>) PATH[i]).getPheromone());
                if(ATRACER.contains((Id<Link>)PATH[i]))
                    TracerEvolution(time,(Id<Link>)PATH[i],Dest);
            }
        } else {
            for (Id<Link> idl : this.Networks.get(Dest).keySet()) {
                if (FLOWEDPATHES.paths.get(idp).contains(idl)) {
                    this.Networks.get(Dest).get(idl).AddDeltaF(1 / this.TTS.TTSs.get(idp), EVAP);
                    if(ATRACER.contains((Id<Link>)idl))
                        TracerEvolution(time,idl,Dest);
                } else {
                    this.Networks.get(Dest).get(idl).AddDeltaF(0, EVAP);
                    if(ATRACER.contains((Id<Link>)idl))
                        TracerEvolution(time,idl,Dest);
                }
            }
        }
    }

    //Pour stoker les valeurs de phéromone
    public void TracerEvolution (Double time, Id < Link > linkAtracer, Id < Link > destination){
        Double TDerniereDonne = MultipleDATA.get(destination).get(linkAtracer).getLast()[0];
        Double dernierPheromone = MultipleDATA.get(destination).get(linkAtracer).getLast()[1];
        if (!(time - TDerniereDonne < INTERVAL)) {
            if (time - TDerniereDonne == INTERVAL) {
                Double Pheromone = this.getNetworks().get(destination).get(linkAtracer).getPheromone();
                Double[] d = {time, Pheromone};
                this.MultipleDATA.get(destination).get(linkAtracer).add(d);
            } else if ((1. < (time - TDerniereDonne) / INTERVAL)) {
                int k = 0;
                for (int m = 1; m < (int) ((time - TDerniereDonne) / INTERVAL); m++) {
                    Double Pheromone = this.getNetworks().get(destination).get(linkAtracer).getPheromone();
                    Double[] di = {TDerniereDonne + m * INTERVAL, Pheromone};
                    this.MultipleDATA.get(destination).get(linkAtracer).add(di);
                    k = m;
                }
                /*Double Pheromone = this.getNetworks().get(destination).get(linkAtracer).getPheromone();
                Double[] d = {TDerniereDonne + k * INTERVAL, Pheromone};
                this.MultipleDATA.get(destination).get(linkAtracer).add(d);*/
            }
        }
           /* Double Pheromone = this.getNetworks().get(destination).get(linkAtracer).getPheromone();
            Double[] d = {time , Pheromone};
            this.MultipleDATA.get(destination).get(linkAtracer).add(d);*/
    }
    public void ECRIRETRACE () {
        Object[] links = ATRACER.toArray();
        for (Object idl : links) {
            for (Id<Link> idd : this.MultipleDATA.keySet()) {
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter("output/withindayexemple/TracePheromone" + idl.toString() + "D" + idd.toString() + ".csv");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Object[] data = this.MultipleDATA.get((Id<Link>)idd).get((Id<Link>) idl).toArray();
                //writer.println("TEMPS;PHEROMONE");
                for (int j = 0; j < data.length; j++) {
                    Double[] cord = (Double[]) data[j];
                    writer.println(cord[0] + ";" + cord[1]);
                }
                writer.close();
            }
        }
    }
    public Id<Link> choosBestNextLinkIdV2(Id<Link> courantID, Id<Link> Dest, Id<Node> interdit) {
        Link curantLink = this.scenario.getNetwork().getLinks().get(courantID);
        Object[] outLinks = curantLink.getToNode().getOutLinks().keySet().toArray();
        Object[] outLinksPermis = new Object[outLinks.length - 1];
        int index = 0;
        for (int i = 0; i < outLinks.length; i++) {
            if (!scenario.getNetwork().getLinks().get((Id<Link>) outLinks[i]).getToNode().getId().toString().equals(interdit.toString())) {
                outLinksPermis[index] = (Id<Link>) outLinks[i];
                index++;
            }
        }
        Double[] Probas = GetProbaWithOnlyPossibleLinks(curantLink, Dest, outLinksPermis);
        Id<Link> route = SimpleRouletWheelV2(outLinksPermis, Probas);
        return route;
    }

    public Id<Link> choosNextLinkIdV2(Id<Link> courantID, Id<Link> Dest) {
        System.out.println("[GUIDANCE] :  DEBUT DE CODE DE GUIDANCE ");
        System.out.println("[GUIDANCE]  : Courant link ID : " + courantID);
        System.out.println("[GUIDANCE]  : Destination ID : " + Dest);
        Link curantLink = this.scenario.getNetwork().getLinks().get(courantID);
        Object[] outLinks = curantLink.getToNode().getOutLinks().keySet().toArray();
        Double[] Probas = GetProbaWithDistination(curantLink, Dest);
        Id<Link> route = SimpleRouletWheelV2(outLinks, Probas);
        return route;
    }

    public Double[] GetProbaWithOnlyPossibleLinks(Link curentLink, Id<Link> Destination, Object[] outLinks) {
        Double[] Probas = new Double[outLinks.length];
        Double sum = 0.;
        for (int i = 0; i < outLinks.length; i++) {
            if (!MultiDest) {
                Probas[i] = Math.pow(this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone(), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
            } else {
                if (PHEROMONEPUANT) {
                    System.out.println("Repulsiif value " + this.RFN.get((Id<Link>) outLinks[i]).getPheromone());
                    Double SUMPHEROMONES = this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone() / this.RFN.get((Id<Link>) outLinks[i]).getPheromone();
                    System.out.println("TAUX ATTRACTION OF LINK : " + (Id<Link>) outLinks[i] + "EST :" + SUMPHEROMONES);
                    Probas[i] = Math.pow(SUMPHEROMONES, ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
                } else {
                    Probas[i] = Math.pow(this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone(), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
                }
            }
            sum += Probas[i];
        }
        System.out.println("{@@@@Some links were deleted form choices} possible ONLY lien en sortie de " + curentLink.getId());
        for (int j = 0; j < Probas.length; j++) {
            Probas[j] = Probas[j] / sum;
            if (!MultiDest) {
                System.out.println("[][] [Possible  LINK] " + (Id<Link>) outLinks[j] + " PHEROMONE : " + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() + " PROBA :" + Probas[j]);
            } else {
                if (PHEROMONEPUANT) {
                    System.out.println("[][]Possible  LINK  LINK " + (Id<Link>) outLinks[j] + " PHEROMONE ATTRACTIF: "
                            + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() + " Feromone repulssif " + this.RFN.get((Id<Link>) outLinks[j]).getPheromone() +
                            " PROBA :" + Probas[j]);

                } else {
                    System.out.println("Possible  LINK  LINK  LINK " + (Id<Link>) outLinks[j] + " PHEROMONE ATTRACTIF :  AR "
                            + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() +
                            " PROBA :" + Probas[j]);
                }
            }
        }
        return Probas;
    }

    public Double[] GetProbaWithOnlyDistination(Link curentLink, Id<Link> Destination) {
        Object[] outLinks = curentLink.getToNode().getOutLinks().keySet().toArray();
        Double[] Probas = new Double[outLinks.length];
        Double sum = 0.;
        for (int i = 0; i < outLinks.length; i++) {
            Probas[i] = Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
            sum += Probas[i];
        }
        for (int j = 0; j < Probas.length; j++) {
            Probas[j] = Probas[j] / sum;
        }
        return Probas;
    }


    public Double[] GetProbaWithDistination(Link curentLink, Id<Link> Destination) {
        Object[] outLinks = curentLink.getToNode().getOutLinks().keySet().toArray();
        Double[] Probas = new Double[outLinks.length];
        Double sum = 0.;
        for (int i = 0; i < outLinks.length; i++) {
            if (!MultiDest) {
                Probas[i] = Math.pow(this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone(), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
            } else {
                if (PHEROMONEPUANT) {
                    if (FRTAXU) { //Proba on integrant le taux de congestion
                        System.out.println("REPUF" + this.RFN.get((Id<Link>) outLinks[i]).getPheromone());
                        Double SUMPHEROMONES = this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone() / this.RFN.get((Id<Link>) outLinks[i]).getPheromone();
                        System.out.println("TAUX attraction : " + (Id<Link>) outLinks[i] + "EST :" + SUMPHEROMONES);
                        Probas[i] = Math.pow((SUMPHEROMONES <= 0 ? 0.00001 : SUMPHEROMONES), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
                    } else { //Proba avec juste le même principe pour unique destination
                        Probas[i] = Math.pow(this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone(), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
                    }
                } else {
                    Probas[i] = Math.pow(this.Networks.get(Destination).get((Id<Link>) outLinks[i]).getPheromone(), ALPHA) * Math.pow((1 / CalculerDistance(Destination, (Id<Link>) outLinks[i])), BETA);
                }
            }

            sum += Probas[i];

        }
        System.out.println("[GetProbaWithDistination] possible lien en sortie de " + curentLink.getId());
        for (int j = 0; j < Probas.length; j++) {
            Probas[j] = Probas[j] / sum;
            if (!MultiDest) {
                System.out.println("[][][GetProbaWithDistination]  LINK " + (Id<Link>) outLinks[j] + " PHEROMONE : " + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() + " PROBA :" + Probas[j]);
            } else {
                if (PHEROMONEPUANT) {
                    System.out.println("[][][GetProbaWithDistination]  LINK " + (Id<Link>) outLinks[j] + " PHEROMONE :  AR "
                            + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() + " - RFEROMONES " + this.RFN.get((Id<Link>) outLinks[j]).getPheromone() +
                            " PROBA :" + Probas[j]);

                } else {
                    System.out.println("[][][GetProbaWithDistination]  LINK " + (Id<Link>) outLinks[j] + " PHEROMONE :  AR "
                            + this.Networks.get(Destination).get((Id<Link>) outLinks[j]).getPheromone() +
                            " PROBA :" + Probas[j]);
                }
            }
        }
        return Probas;
    }

    public void AfficherPheromone() {

        for (Id<Link> Dest : this.Networks.keySet()) {
            PrintWriter writer2 = null;
            try {
                writer2 = new PrintWriter("output/withindayexemple/PHEROMONESValues" + Dest.toString() + ".csv");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (!MultiDest) {
                writer2.println("IdLink;Pheromone");
                for (Id<Link> id : this.Networks.get(Dest).keySet()) {
                    writer2.println(id + ";" + this.Networks.get(Dest).get(id).getPheromone());
                }
                writer2.close();
            } else {
                if (PHEROMONEPUANT) {
                    if (FRTAXU)
                        writer2.println("IdLink;Pheromone;PheromoneRG;SUM");
                    else {
                        writer2.println("IdLink;Pheromone");
                    }
                } else {
                    writer2.println("IdLink;Pheromone");
                }
                for (Id<Link> id : this.Networks.get(Dest).keySet()) {
                    if (PHEROMONEPUANT) {
                        if (FRTAXU) {
                            Double SUMPHEROM = this.Networks.get(Dest).get(id).getPheromone() / this.RFN.get(id).getPheromone();
                            //System.out.println("LINK " + id + " Phéromone " + SUMPHEROM);
                            writer2.println(id + ";" + this.Networks.get(Dest).get(id).getPheromone() + ";" + this.RFN.get(id).getPheromone() + ";" + SUMPHEROM);
                        } else {
                            writer2.println(id + ";" + this.Networks.get(Dest).get(id).getPheromone());
                        }
                    } else {
                        writer2.println(id + ";" + this.Networks.get(Dest).get(id).getPheromone());
                    }
                }
                writer2.close();
            }
        }
    }
}

