package tutorial.withinday.withinDayReplanningAgents;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;


public class ControllerListner implements StartupListener, IterationEndsListener, EventHandler {
    private final static Logger log = Logger.getLogger(ControllerListner.class);
    private TTSHandler TTSH;
    private TravelTimePerLink TTL;
    private FollowedPathHandler followedPaths;
    private ACORGuidance Guidance;
    private CongestionObserver Congistion;


    ControllerListner(TTSHandler tts, TravelTimePerLink ttl, FollowedPathHandler flpaths, ACORGuidance guidance, CongestionObserver cong) {
        this.TTSH = tts;
        this.TTL = ttl;
        this.followedPaths = flpaths;
        this.Guidance = guidance;
        this.Congistion = cong;
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        System.out.println("#CONTROLLER LISTNER DEBUT NOTIFY STARTUP");
        startupEvent.getServices().getEvents().addHandler(this.TTSH);
        startupEvent.getServices().getEvents().addHandler(this.followedPaths);
        startupEvent.getServices().getEvents().addHandler(this.Congistion);
        startupEvent.getServices().getEvents().addHandler(this.TTL);

    }

    @Override
    public void reset(int iteration) {
        this.followedPaths.reset(iteration);
        this.TTSH.TTSs.clear();
        this.TTSH.TTSMoyes = 0;
        this.Congistion.reset(iteration);
        System.out.println("DE LA PART DE RESTE :  TTS MOYEN A L'ITERATION " + iteration + " VAL " + this.TTSH.TTSMoyes);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) { //ANALYSE DES RÉSULTATS
        this.Congistion.MaxAgentsPerLink();
        this.Guidance.AfficherPheromone();
        this.TTL.CalculeTempsDeVoyageMoyenParLien();
        this.TTL.CalculeVitesseRelativeMoyenneParLien();
        this.TTL.ECRIRETRACE();
        this.TTL.getROUTER().ECRIRETRACE();
        this.TTSH.CalculerTTSMoyen();

        this.reset(iterationEndsEvent.getIteration());

    }
}


