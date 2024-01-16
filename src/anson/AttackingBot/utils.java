package AttackingBot;
import battlecode.common.*;

import java.awt.*;
import java.util.Collections;
import java.util.Vector;

public class utils {
    static MapLocation defflag = null;
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

    public static MapLocation closestSpawn(RobotController rc) throws GameActionException {
        MapLocation[] spawns = rc.getAllySpawnLocations();
        MapLocation ans = null;
        int dist = 0;
        for(MapLocation loc: spawns){
            if(ans == null){
                dist = distance(loc,rc.getLocation());
                ans = loc;
            }else if(dist > distance(loc,rc.getLocation())){
                dist = distance(loc,rc.getLocation());
                ans = loc;
            }
        }
        return ans;
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
        FlagInfo[] flags = rc.senseNearbyFlags(-1);
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
                enemies++;
            }
        }
        if(distance(RobotPlayer.target,rc.getLocation())>4){
            if(botNo<=RobotPlayer.noTeams) {
                Pathfind.moveTowards(rc, RobotPlayer.target);
            }else{
                Pathfind.moveTowards(rc, RobotPlayer.target);
            }
        }
        if(importantEnemy != null) {
            MapLocation center = new MapLocation(Math.round((float)xsum/enemies),Math.round((float)ysum/enemies));
            if (importantEnemy.hasFlag()) {
                Pathfind.moveTowards(rc, importantEnemy.location);
                while (rc.canAttack(importantEnemy.location)) {
                    rc.attack(importantEnemy.location);
                }
            }
            if(enemies>=7){
                if(rc.canBuild(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)))){
                    rc.build(TrapType.EXPLOSIVE,rc.getLocation().add(rc.getLocation().directionTo(center)));
                }
            }else if(enemies>=5){
                if(rc.canBuild(TrapType.STUN,rc.getLocation().add(rc.getLocation().directionTo(center)))){
                    rc.build(TrapType.STUN,rc.getLocation().add(rc.getLocation().directionTo(center)));
                }
            }
            MapInfo[] locations = rc.senseNearbyMapInfos(rc.getLocation(), -1);
            for (MapInfo loc : locations) {
                if (loc.getTrapType() != TrapType.NONE) {
                    Direction dir = center.directionTo(loc.getMapLocation());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                }
            }
            while (rc.canAttack(importantEnemy.location)) {
                rc.attack(importantEnemy.location);
            }
        }
        while (importantAlly!=null && rc.canHeal(importantAlly.location))
            rc.heal(importantAlly.location);
    }
    public static void defendSpawn(RobotController rc) throws GameActionException{
        rc.setIndicatorString("defending");
        if(rc.isSpawned()) {
            rc.writeSharedArray(RobotPlayer.botNo,getEnemies(rc).size());
            if(defflag == null && rc.senseNearbyFlags(4).length>0) {
                defflag = rc.senseNearbyFlags(4)[0].getLocation();
                rc.setIndicatorDot(defflag, 1, 1, 1);
            }

            if (rc.getLocation() != defflag) {
                if(rc.canMove(rc.getLocation().directionTo(defflag))){
                    rc.move(rc.getLocation().directionTo(defflag));
                }
            }
            RobotInfo[] neabyRobots = rc.senseNearbyRobots(-1);
            for (RobotInfo currRobot : neabyRobots) {
                // Checks for which team it's on
                if (currRobot.team != rc.getTeam()) {
                    Direction d1 = rc.getLocation().directionTo(currRobot.getLocation());
                    Direction d2 = d1.rotateLeft();
                    Direction d3 = d1.rotateRight();
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(d1))) {
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(d1));
                    }
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(d2))) {
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(d2));
                    }
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(d3))) {
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(d3));
                    }
                }
            }
            for (RobotInfo currRobot : neabyRobots) {
                // Checks for which team it's on
                if (currRobot.team != rc.getTeam()) {
                    if (rc.canAttack(currRobot.location)) {
                        rc.attack(currRobot.location);
                    }
                }
            }

        }else{
            if(rc.canSpawn(rc.getAllySpawnLocations()[(RobotPlayer.botNo)%3*9])) {
                rc.spawn(rc.getAllySpawnLocations()[(RobotPlayer.botNo)%3*9]);
            }
        }
    }

    public static void protect(RobotController rc, RobotInfo bot) throws GameActionException {
        if(bot==null){
            return;
        }
        if(rc.canHeal(bot.getLocation())){
            rc.heal(bot.getLocation());
        }
        RobotInfo[] infos = rc.senseNearbyRobots(bot.getLocation(),-1,rc.getTeam().opponent());
        for(RobotInfo info:infos){
            if(info.getTeam()!=rc.getTeam()){
                Pathfind.moveTowards(rc,info.getLocation());
                if(rc.canAttack(info.getLocation())){
                    rc.attack(info.getLocation());
                }
            }
        }
    }

    public static void whenSensedFlag(RobotController rc,FlagInfo flag) throws GameActionException {
        if(flag.getTeam()!=rc.getTeam()) {
            if(rc.canPickupFlag(flag.getLocation())){
                rc.pickupFlag(flag.getLocation());
                whenHasFlag(rc);
            }
            if (RobotPlayer.rng.nextInt(10) == 0 || distance(rc.getLocation(), flag.getLocation()) > 5) {
                Pathfind.moveTowards(rc, flag.getLocation());
                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(rc.getLocation().directionTo(flag.getLocation())))) {
                    rc.build(TrapType.STUN, rc.getLocation().add(rc.getLocation().directionTo(flag.getLocation())));
                }
            }
        }
    }
    public static void whenHasFlag(RobotController rc) throws GameActionException {
        Vector<RobotInfo> enemies = getEnemies(rc);
        int sumx = 0, sumy = 0;
        for(RobotInfo enemy: enemies){
            sumx += enemy.location.x;
            sumy += enemy.location.y;
        }
        MapLocation center = new MapLocation(sumx/enemies.size(),sumy/enemies.size());
        while(rc.canMove(rc.getLocation().directionTo(center).opposite())){
            rc.move(rc.getLocation().directionTo(center).opposite());
        }
        MapLocation target = closestSpawn(rc);
        Pathfind.moveTowards(rc,target);
    }

    public static Vector<RobotInfo> getEnemies(RobotController rc) throws GameActionException {
        RobotInfo [] bots = rc.senseNearbyRobots(-1);
        Vector<RobotInfo> enemies = new Vector<>();
        for(RobotInfo bot: bots){
            if(bot.getTeam()!=rc.getTeam()){
                enemies.add(bot);
            }
        }
        return enemies;
    }

    public static Vector<RobotInfo> getAllies(RobotController rc) throws GameActionException {
        RobotInfo [] bots = rc.senseNearbyRobots(-1);
        Vector<RobotInfo> allies = new Vector<>();
        for(RobotInfo bot: bots){
            if(bot.getTeam()==rc.getTeam()){
                allies.add(bot);
            }
        }
        return allies;
    }

    public static MapLocation numToMapLoc(int n){
        return new MapLocation((n%10000)/100,n%100);
    }

    public static boolean Valid(RobotController rc,MapLocation pos){
        return pos.x>=0&&pos.x<rc.getMapWidth() && pos.y>=0 &&pos.y<rc.getMapHeight();
    }
}
