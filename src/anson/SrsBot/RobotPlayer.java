package SrsBot;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    // Variables
    static int botNo;

    // Generate the random seed
    static Random rng;

    //Location it should defend
    static MapLocation target = null;


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
        if (RobotPlayer.rng == null) RobotPlayer.rng = new Random(rc.getID());
        while (true) {
            if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
                rc.buyGlobal(GlobalUpgrade.ACTION);
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
            }
            if(rc.getRoundNum()==1){
                botNo = utils.getBotNum(rc);
                Clock.yield();
            }
            if(botNo<3){
                utils.defendSpawn(rc);
                Clock.yield();
                continue;
            }
            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                utils.askForHelpIfNeed(rc);
                if(target==null){
                    target = utils.getClosestFlag(rc);
                }
                int roundNum = rc.getRoundNum();
                if (!rc.isSpawned()){
                    if(target==null) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        // Pick a random spawn location to attempt spawning in
                        // Make the robot retreat to the location it spawned in
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                        }
                    }else{
                        utils.spawnClosest(rc, target);
                    }
                }
                if(rc.isSpawned() && roundNum < 160){
                    Pathfind.explore(rc);
                }
                if (rc.isSpawned() && roundNum >= 160) {
                    FlagInfo flag = utils.getNearbyEnemyDroppedFlag(rc);
                    FlagInfo pickedUp = utils.getPickedUpEnemyDroppedFlag(rc);
                    if(rc.hasFlag()){
                        utils.whenHasFlag(rc);
                    }else if(flag!=null){
                        utils.whenSensedFlag(rc,flag);
                    }else if(pickedUp!=null){
                        utils.protect(rc,rc.senseRobotAtLocation(pickedUp.getLocation()));
                    }
                    else if(!utils.inGroup(rc)){
                        MapLocation target = utils.getCloestRobotThatNeedHelp(rc);
                        if(target == null) {
                            target = utils.closestSpawn(rc);
                        }
                        Pathfind.moveTowards(rc,target);
                    }else{
                        utils.moveWithGroup(rc,botNo);
                    }
                    utils.attackAndHeal(rc,botNo);
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