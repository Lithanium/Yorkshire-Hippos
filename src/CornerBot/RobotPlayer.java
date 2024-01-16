package CornerBot;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    // Variables
    static boolean done = false;
    static int turnCount = 0;
    static int exploreMap = -1;
    static int retreatLength = 20;
    static int shouldMove;
    static int botNo;

    // Generate the random seed
    public static Random rng;

    //Location it should defend
    static MapLocation toDefend = null;
    static MapLocation Corner = null;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    public static void run(RobotController rc) throws GameActionException {
        // Make the rng seed
        if (rng == null) rng = new Random(rc.getID());

        // This is how long you want to explore the map
        // NOTE: NOT USED YET
        if (exploreMap == -1) exploreMap = rc.getMapHeight() + rc.getMapWidth();
        int mapHeight = rc.getMapHeight();
        int mapWidth = rc.getMapWidth();
        while (true) {

            // Check for global upgrades

            // First get action and then healing
            if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                rc.buyGlobal(GlobalUpgrade.ACTION);
            }
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
            }

            turnCount += 1;
            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                int roundNum = rc.getRoundNum();
                int id = rc.getID();
                if(roundNum == 1){
                    botNo = utils.getBotNum(rc);
                    MapLocation spawn1 = rc.getAllySpawnLocations()[4];
                    MapLocation spawn2 = rc.getAllySpawnLocations()[13];
                    MapLocation spawn3 = rc.getAllySpawnLocations()[22];
                    int avgx = (spawn1.x+spawn2.x+ spawn3.x)/3;
                    int avgy = (spawn1.y+spawn2.y+ spawn3.y)/3;
                    if(avgx >= mapWidth/2){
                        if(avgy>=mapHeight/2) {
                            Corner = new MapLocation(mapWidth-1,mapHeight-1);
                            if(botNo<25){
                                toDefend = new MapLocation(mapWidth - 1 - 6, mapHeight - 1);
                            }else{
                                toDefend = new MapLocation(mapWidth - 1, mapHeight - 1 - 6);
                            }
                        }else{
                            Corner = new MapLocation(mapWidth-1,0);
                            if(botNo<25){
                                toDefend = new MapLocation(mapWidth - 1 - 6, 0);
                            }else{
                                toDefend = new MapLocation(mapWidth - 1, 6);
                            }
                        }
                    }else{
                        if(avgy>=mapHeight/2) {
                            Corner = new MapLocation(0,mapHeight-1);
                            if(botNo<25){
                                toDefend = new MapLocation(6, mapHeight - 1);
                            }else{
                                toDefend = new MapLocation(0, mapHeight - 1 - 6);
                            }
                        }else{
                            Corner = new MapLocation(0,0);
                            if(botNo<25){
                                toDefend = new MapLocation(0, 6);
                            }else{
                                toDefend = new MapLocation(6, 0);
                            }
                        }
                    }
                    Clock.yield();
                }
                // Try spawn in the bot
                if (!rc.isSpawned()){
                    if(roundNum<200) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        // Pick a random spawn location to attempt spawning in
                        // Make the robot retreat to the location it spawned in
                        MapLocation randomLoc = spawnLocs[(botNo % 3) * 9 + rng.nextInt(3)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                            //toDefend = randomLoc;
                        }
                    }else{
                        utils.spawnClosest(rc,Corner);
                    }
                }
                if (rc.isSpawned()) {
                    if(botNo<=2 && !done){
                        if(!rc.hasFlag()) {
                            FlagInfo[] flags = rc.senseNearbyFlags(-1);
                            for (FlagInfo flag : flags) {
                                if (flag.getTeam() == rc.getTeam() && !flag.isPickedUp()) {
                                    if (rc.canPickupFlag(flag.getLocation())) {
                                        rc.pickupFlag(flag.getLocation());
                                    }
                                }
                            }
                        }else{
                            MapLocation target = null;
                            if(botNo == 0) {
                                target = Corner;
                            }else if(botNo == 1){
                                if(Corner.x == 0){
                                    target = new MapLocation(6,Corner.y);
                                }else{
                                    target = new MapLocation(mapWidth-7,Corner.y);
                                }
                            }else{
                                if(Corner.y == 0){
                                    target = new MapLocation(Corner.x,6);
                                }else{
                                    target = new MapLocation(Corner.x,mapHeight-7);
                                }
                            }

                            if(utils.distance(target,rc.getLocation())<3 && rc.senseMapInfo(target).isWall() && rc.canDropFlag(rc.getLocation())){
                                rc.dropFlag(rc.getLocation());
                                done = true;
                            }else if(rc.canDropFlag(target)){
                                rc.dropFlag(target);
                                done = true;
                            }else{
                                Pathfind.moveTowards(rc,target,botNo);
                            }
                            rc.setIndicatorDot(target,1,1,1);
                        }
                    }
                    // Relevant robots to interact with
                    utils.attackAndHeal(rc,botNo);
                    //System.out.println(botNo);
                    MapLocation[] people = new MapLocation[50];
                    for(int i = 0; i<50; i++) {
                        people[i] = new MapLocation((rc.readSharedArray(i) % 10000) / 100, rc.readSharedArray(i) % 100);
                    }
                    // Explore and get crumbs time
                    if (turnCount < 160)
                        Pathfind.explore(rc,botNo);
                        // Retreat to designated location
                    else if (turnCount < 200 || utils.distance(rc.getLocation(),toDefend)>10 || RobotPlayer.rng.nextInt(15) == 0) {
                        Pathfind.moveTowards(rc, toDefend,botNo);
                    } else {
                        // Sense if there is a flag nearby
                        // Dig water for levels
                        if(rc.getRoundNum()>1900) {
                            for (Direction checkValid : directions) {
                                // Dig where possible
                                MapLocation currLoc = rc.getLocation().add(checkValid);
                                if (rc.canDig(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
                                    rc.dig(currLoc);
                            }

                            // Fill water for levels
                            for (Direction checkValid : directions) {
                                // Fill where possible
                                MapLocation currLoc = rc.getLocation().add(checkValid);
                                if (rc.canFill(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
                                    rc.fill(currLoc);
                            }
                        }
                    }
                }
            } catch (GameActionException e) {
                // Bad things happen
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Bad things happen
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // End the turn
                Clock.yield();
            }
        }
    }

}
