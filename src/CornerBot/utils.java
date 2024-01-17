package CornerBot;
import battlecode.common.*;

import java.util.Collections;
import java.util.Vector;

public class utils {
    public static int distance(MapLocation a, MapLocation b){
        return Math.max(Math.abs(a.x-b.x),Math.abs(a.y-b.y));
    }
    public static void spawnClosest(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation[] spawns = rc.getAllySpawnLocations();
        MapLocation spawn1 = spawns[4];
        MapLocation spawn2 = spawns[13];
        MapLocation spawn3 = spawns[22];
        if(distance(spawn1,target) <= distance(spawn2,target) && distance(spawn1,target) <= distance(spawn3,target)){
            for(int i = 0; i<9; i++) {
                if(rc.canSpawn(spawns[i])) {
                    rc.spawn(spawns[i]);
                }
            }
        }else if(distance(spawn2,target) <= distance(spawn1,target) && distance(spawn2,target) <= distance(spawn3,target)){
            for(int i = 0; i<9; i++) {
                if(rc.canSpawn(spawns[9+i])) {
                    rc.spawn(spawns[9+i]);
                }
            }
        }else{
            for(int i = 0; i<9; i++) {
                if(rc.canSpawn(spawns[18+i])) {
                    rc.spawn(spawns[18+i]);
                }
            }
        }
    }

    public static int getBotNum(RobotController rc) throws GameActionException {
        int botNo = rc.readSharedArray(0);
        rc.writeSharedArray(0,botNo+1);
        return botNo;
    }

    public static void attackAndHeal(RobotController rc,int botNo) throws GameActionException{
        RobotInfo[] neabyRobots = rc.senseNearbyRobots(-1);
        RobotInfo importantEnemy = null;
        RobotInfo importantAlly = null;
        int allies = 0, enemies = 0;
        int xsum = 0, ysum = 0;
        for (RobotInfo currRobot : neabyRobots) {
            // Checks for which team it's on
            if (currRobot.team == rc.getTeam()) {
                if(importantAlly == null || currRobot.getHealth() < importantAlly.getHealth()){
                    importantAlly = currRobot;
                }
                allies++;
            } else {
                if(importantEnemy == null || currRobot.getHealth() < importantEnemy.getHealth()) {
                    xsum += currRobot.getLocation().x;
                    ysum += currRobot.getLocation().y;
                    importantEnemy = currRobot;
                }
                if(currRobot.hasFlag()){
                    if(rc.canAttack(currRobot.getLocation())){
                        rc.attack(currRobot.getLocation());
                    }
                    if(rc.canMove(rc.getLocation().directionTo(currRobot.getLocation()))){
                        rc.move(rc.getLocation().directionTo(currRobot.getLocation()));
                    }
                }
                enemies++;
            }
        }
        if(importantEnemy != null) {
            MapLocation center = new MapLocation(Math.round((float)xsum/enemies),Math.round((float)ysum/enemies));
            if (importantEnemy.hasFlag()  && rc.getRoundNum() > 200) {
                Pathfind.moveTowards(rc, importantEnemy.location, botNo);
            } else if (rc.getHealth() < 300) {
                Pathfind.moveTowards(rc, rc.getLocation().add(rc.getLocation().directionTo(center).opposite()), botNo);
            }
            if(enemies>=5){
                if(rc.canBuild(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)))){
                    rc.build(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)));
                }
            }else if(enemies>=3){
                if(rc.canBuild(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)))){
                    rc.build(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)));
                }
            }
            while (rc.canAttack(importantEnemy.location)) {
                rc.attack(importantEnemy.location);
            }
        }
        while (importantAlly!=null && rc.canHeal(importantAlly.location))
            rc.heal(importantAlly.location);
    }
}
