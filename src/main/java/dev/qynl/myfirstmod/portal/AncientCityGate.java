package dev.qynl.myfirstmod.portal;

import java.util.Optional;
import java.util.function.Function;

/** Vanilla 1.21.1 city-center templates: 22x8 outside, 20x6 aperture. */
public final class AncientCityGate {
    public static final int WIDTH=22,HEIGHT=8;
    public enum Cell { FRAME, EMPTY, PORTAL, BLOCKED }
    public record Point(int x,int y,int z) {public Point add(int dx,int dy,int dz){return new Point(x+dx,y+dy,z+dz);}}
    public record Frame(Point origin,boolean alongX) {
        public Point at(int across,int up){return origin.add(alongX?across:0,up,alongX?0:across);}
    }
    private AncientCityGate() {}
    public static boolean border(int x,int y){return x==0||x==WIDTH-1||y==0||y==HEIGHT-1;}
    public static Optional<Frame> find(Point clicked,Function<Point,Cell> read) {
        if(read.apply(clicked)!=Cell.FRAME)return Optional.empty();
        for(boolean axis:new boolean[]{true,false}) for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            if(!border(x,y))continue;
            Frame frame=new Frame(clicked.add(axis?-x:0,-y,axis?0:-x),axis);
            if(valid(frame,read))return Optional.of(frame);
        }
        return Optional.empty();
    }
    public static boolean valid(Frame f,Function<Point,Cell> read) {
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++)if(border(x,y)&&read.apply(f.at(x,y))!=Cell.FRAME)return false;
        for(int y=1;y<HEIGHT-1;y++)for(int x=1;x<WIDTH-1;x++) {
            Cell c=read.apply(f.at(x,y));if(c!=Cell.EMPTY&&c!=Cell.PORTAL)return false;
        }
        return true;
    }
}
