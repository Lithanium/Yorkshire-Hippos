package SrsBot;
import battlecode.common.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

public class utils {
    static MapLocation defflag = null;
    public static boolean inGroup(RobotController rc) throws GameActionException {
        return utils.getAllies(rc).size()>=5;
    }
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
    public static void askForHelpIfNeed(RobotController rc) throws GameActionException{
        int botNo = RobotPlayer.botNo;
        if(rc.isSpawned() && getEnemies(rc).size()>getAllies(rc).size()){
            rc.writeSharedArray(botNo,rc.readSharedArray(botNo)%10000+10000);
            rc.setIndicatorString("asking for help");
        }else{
            rc.writeSharedArray(botNo,rc.readSharedArray(botNo)%10000);
        }
    }

    public static MapLocation getCloestRobotThatNeedHelp(RobotController rc) throws GameActionException{
        int dist = 9999;
        MapLocation ans = null;
        for(int i =0; i<50; i++){
            if(i!=RobotPlayer.botNo) {
                int temp = rc.readSharedArray(i);
                if (temp / 10000 == 1) {
                    MapLocation cur = numToMapLoc(temp);
                    if(distance(cur,rc.getLocation())<dist){
                        ans = cur;
                    }
                }
            }
        }
        return ans;
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
    public static void moveWithGroup(RobotController rc,int botNo) throws GameActionException {
        Vector<RobotInfo> allies = getAllies(rc);
        Integer []distances = {100,100,100,100,100};
        for(RobotInfo ally: allies){
            int idx = 0;
            for(int i = 0; i<5; i++){
                if(distances[idx]<distances[i]){
                    idx = i;
                }
            }
            if(distance(ally.getLocation(),rc.getLocation())<distances[idx]){
                distances[idx] = distance(ally.getLocation(),rc.getLocation());
            }
        }
        if(Collections.max(Arrays.asList(distances))>=3) {
            int sumx = 0, sumy = 0;
            for (RobotInfo ally : allies) {
                sumx += ally.location.x;
                sumy += ally.location.y;
            }
            MapLocation center = new MapLocation(sumx / allies.size(), sumy / allies.size());
            while (rc.canMove(rc.getLocation().directionTo(center))) {
                rc.move(rc.getLocation().directionTo(center));
                rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
            }
        }else{
            MapLocation flag = getClosestFlag(rc);
            if(flag == null||distance(flag,rc.getLocation())<3){
                Pathfind.explore(rc);
            }else{
                Pathfind.moveTowards(rc,getClosestFlag(rc));
            }
        }
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
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                }
                enemies++;
            }
        }
        if(importantEnemy != null) {
            int trapdis = 999;
            MapLocation center = new MapLocation(Math.round((float)xsum/enemies),Math.round((float)ysum/enemies));
            MapInfo[] locations = rc.senseNearbyMapInfos(rc.getLocation(), -1);
            for (MapInfo loc : locations) {
                if (loc.getTrapType() != TrapType.NONE) {
                    trapdis = Math.min(trapdis,distance(loc.getMapLocation(),rc.getLocation()));
                    Direction dir = center.directionTo(loc.getMapLocation());
                    if (rc.canMove(dir) && distance(loc.getMapLocation(),rc.getLocation()) < 3) {
                        rc.move(dir);
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                }
            }
            if (importantEnemy.hasFlag()  && rc.getRoundNum() > 200) {
                Pathfind.moveTowards(rc, importantEnemy.location);
            } else if (rc.getHealth() < 300) {
                Pathfind.moveTowards(rc, rc.getLocation().add(rc.getLocation().directionTo(center).opposite()));
            }
            if(enemies>=5 && trapdis>4){
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
    public static void defendSpawn(RobotController rc) throws GameActionException{
        int botNo = RobotPlayer.botNo;
        rc.setIndicatorString("defending");
        if(rc.isSpawned()) {
            askForHelpIfNeed(rc);
            rc.writeSharedArray(RobotPlayer.botNo,getEnemies(rc).size());
            if(defflag == null && rc.senseNearbyFlags(4).length>0) {
                defflag = rc.senseNearbyFlags(4)[0].getLocation();
                rc.setIndicatorDot(defflag, 1, 1, 1);
            }
            if(defflag != null) {
                if (rc.getLocation() != defflag) {
                    if (rc.canMove(rc.getLocation().directionTo(defflag))) {
                        rc.move(rc.getLocation().directionTo(defflag));
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                }
            }
            RobotInfo[] neabyRobots = rc.senseNearbyRobots(-1);
            for (RobotInfo currRobot : neabyRobots) {
                // Checks for which team it's on
                if (currRobot.team != rc.getTeam()) {
                    Direction d1 = rc.getLocation().directionTo(currRobot.getLocation());
                    Direction d2 = d1.rotateLeft();
                    Direction d3 = d1.rotateRight();
                    if (rc.canBuild(TrapType.STUN, rc.getLocation().add(d1))) {
                        rc.build(TrapType.STUN, rc.getLocation().add(d1));
                    }
                    if (rc.canBuild(TrapType.STUN, rc.getLocation().add(d2))) {
                        rc.build(TrapType.STUN, rc.getLocation().add(d2));
                    }
                    if (rc.canBuild(TrapType.STUN, rc.getLocation().add(d3))) {
                        rc.build(TrapType.STUN, rc.getLocation().add(d3));
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
        if(bot.getLocation().directionTo(closestSpawn(rc))==rc.getLocation().directionTo(closestSpawn(rc))||distance(bot.getLocation(),rc.getLocation())<=2){
            Pathfind.explore(rc);
        }
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
        int botNo = RobotPlayer.botNo;
        if(flag.getTeam()!=rc.getTeam()) {
            if(rc.canPickupFlag(flag.getLocation())){
                rc.pickupFlag(flag.getLocation());
                whenHasFlag(rc);
            }
            if ((double)getAllies(rc).size()/(double)getEnemies(rc).size()>1.3) {
                Pathfind.moveTowards(rc, flag.getLocation());
                if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                    rc.build(TrapType.STUN, rc.getLocation());
                }
            }else{
                rc.writeSharedArray(botNo,rc.readSharedArray(botNo)%10000+10000);
            }
            attackAndHeal(rc,RobotPlayer.botNo);
        }
    }
    public static void whenHasFlag(RobotController rc) throws GameActionException {
        /*int botNo = RobotPlayer.botNo;
        Vector<RobotInfo> enemies = getEnemies(rc);
        if(!enemies.isEmpty()) {
            int sumx = 0, sumy = 0;
            for (RobotInfo enemy : enemies) {
                sumx += enemy.location.x;
                sumy += enemy.location.y;
            }
            MapLocation center = new MapLocation(sumx / enemies.size(), sumy / enemies.size());
            while (rc.canMove(rc.getLocation().directionTo(center).opposite())) {
                rc.move(rc.getLocation().directionTo(center).opposite());
                rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
            }
        }*/
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

    public static FlagInfo getNearbyEnemyDroppedFlag(RobotController rc) throws GameActionException {
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
        for(FlagInfo flag: nearbyFlags){
            if(flag.getTeam()!=rc.getTeam() && !flag.isPickedUp()){
                return flag;
            }
        }
        return null;
    }
    public static FlagInfo getPickedUpEnemyDroppedFlag(RobotController rc) throws GameActionException {
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
        for(FlagInfo flag: nearbyFlags){
            if(flag.getTeam()!=rc.getTeam() && flag.isPickedUp()){
                return flag;
            }
        }
        return null;
    }

    public static MapLocation getClosestFlag(RobotController rc) throws GameActionException{
        if(rc.isSpawned()) {
            MapLocation[] flags = rc.senseBroadcastFlagLocations();
            MapLocation ans = null;
            int dist = 0;
            for (MapLocation flag : flags) {
                if (ans == null || dist > distance(flag, rc.getLocation())) {
                    dist = distance(flag, rc.getLocation());
                    ans = flag;
                }
            }
            return ans;
        }else{
            MapLocation ans = null;
            int dist = 0;
            MapLocation[] flags = rc.senseBroadcastFlagLocations();
            for(MapLocation loc:rc.getAllySpawnLocations()) {
                for (MapLocation flag : flags) {
                    if (ans == null || dist > distance(flag, loc)) {
                        dist = distance(flag, loc);
                        ans = flag;
                    }
                }
            }
            return ans;
        }
    }

    public static boolean Valid(RobotController rc,MapLocation pos){
        return pos.x>=0&&pos.x<rc.getMapWidth() && pos.y>=0 &&pos.y<rc.getMapHeight();
    }
}
