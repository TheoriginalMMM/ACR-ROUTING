package tutorial.withinday.withinDayReplanningAgents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

public class TTSHandler implements VehicleEntersTrafficEventHandler {
    public Map<Id<Person>,	Double> TTSs;
    public double TTSMoyes;
    private final static Logger log	=	Logger.getLogger(TTSHandler.class);

    public TTSHandler() {
        TTSs = new HashMap<>();
        TTSMoyes = 0.;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
        System.out.println("LE MOMENT OU "+vehicleEntersTrafficEvent.getPersonId()+"IL RENTRE DANS LE RESSEAU "+vehicleEntersTrafficEvent.getTime());
        this.TTSs.put(vehicleEntersTrafficEvent.getPersonId(),	vehicleEntersTrafficEvent.getTime());
    }

    public void CalculerTTSMoyen(){
        Double sum=0.;
        for(Double tts : TTSs.values()){
            sum+=tts;
        }
        this.TTSMoyes=sum/TTSs.size();
        log.info("[ANALYSE]  ====== TTS MOYENS POUR CETTE EXECUTION ============ "+TTSMoyes);
    }
    @Override
    public void reset(int i) {
        System.out.println("RIENA  FAIRE RESTER");
        Double sum=0.;
        for(Double tts : TTSs.values()){
            sum+=tts;
        }
        this.TTSMoyes=sum/TTSs.size();
    }
    //Pour ne pas prendre en compte le temps traversé sur le lien d'arrivé
    public void MAJTTS(Id<Person> id, Double time){
        System.out.println("[TTS HANDLER] : "+id+" ------> ENTRER : "+this.TTSs.get(id));
        System.out.println("[TTS HANDLER] : "+id+" ------> SORTIE : "+time);
        double	departureTime	=	this.TTSs.get(id);
        double	TTs	=	time	-	departureTime;
        this.TTSs.put(id,	TTs);
        System.out.println("[TTS HANDLER] : "+id+" ------> TTS : "+this.TTSs.get(id));
    }



}

