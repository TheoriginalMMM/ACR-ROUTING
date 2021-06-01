/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package tutorial.withinday.withinDayReplanningAgents;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import javax.inject.Inject;
import java.io.IOException;

public class RunWithinDayReplanningAgentExample {

    public static void main(String[] args) throws IOException {
        final int NBAGENTS = 500;
        final Boolean MULTIDISTINATION = true;
        //final int[]  distinations = {68,59};
        final int[]  distinations = {59,69};
        //final int[]  distinations = {59};

        final Double IntervaleDeTemps=0.;
        final int BatchSize =250;

        Config config;
        //Config config = ConfigUtils.createConfig();
        if (args == null || args.length == 0 || args[0] == null) {
            config = ConfigUtils.loadConfig("config.xml");
        } else {
            System.out.println(args[0]);
            config = ConfigUtils.loadConfig(args[0]);
        }
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);
        config.controler().setWriteEventsInterval(1);
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setStartTime(0. * 3600);
        config.qsim().setEndTime(14. * 3600);


        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        Controler ctrl = new Controler(config);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        FollowedPathHandler FPaths = new FollowedPathHandler(scenario);
        TTSHandler TTS = new TTSHandler();
        final ACORGuidance router = new ACORGuidance(FPaths, TTS, scenario,MULTIDISTINATION,distinations);
        final CongestionObserver Cong = new CongestionObserver(scenario, router,distinations);
        final TravelTimePerLink TTL = new TravelTimePerLink(scenario, router,distinations);
        ControllerListner ControllerLi = new ControllerListner(TTS, TTL, FPaths, router, Cong);
        ctrl.addControlerListener(ControllerLi);

        ctrl.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                bindMobsim().toProvider(new Provider<Mobsim>() {

                    @Inject
                    Scenario sc;
                    @Inject
                    EventsManager ev;

                    @Override
                    public Mobsim get() {
                        final QSim qsim = QSimUtils.createDefaultQSim(sc, ev);
                        qsim.addAgentSource(new AgentSource() {

                            final VehicleType basicVehicleType = new VehicleTypeImpl(Id.create("basicVehicleType", VehicleType.class));

                            @Override
                            public void insertAgentsIntoMobsim() {
                                int j=0;
                                int counter = 0;
                                //Id<Link> start = (Id<Link>) (scenario.getNetwork().getLinks().keySet().toArray())[5];
                                Id<Link> start = (Id<Link>) (scenario.getNetwork().getLinks().keySet().toArray())[8];
                                Id<Link> End = Id.createLinkId(String.valueOf(distinations[j]));
                                Double startTime = 0.;
                                //getLegs(p.getSelectedPlan()).get(0).getRoute().getEndLinkId();
                                for (int i = 0; i < NBAGENTS; i++) {
                                    System.out.println("INSERT AGENTS LOOP ");
                                    String Key=String.valueOf(i).concat("+".concat(End.toString()));
                                    MobsimVehicle veh = new QVehicle(new VehicleImpl(Id.create(Key, Vehicle.class), basicVehicleType));
                                    qsim.addParkedVehicle(veh, start);
                                    Id<Person> id = Id.createPersonId(Key);
                                    MyAgent ag = new MyAgent(router, scenario, ev, qsim, start, End, veh, id, startTime);
                                    qsim.insertAgentIntoMobsim(ag);
                                    counter++;
                                    if(counter%BatchSize==0){
                                        startTime+=IntervaleDeTemps;
                                        j+=1;
                                        End=Id.createLinkId(String.valueOf(distinations[j%distinations.length]));
                                    }
                                }
                            }
                        });
                        return qsim;
                    }

                });
            }
        });
        ctrl.run();
    }
}
