package dev.qynl.myfirstmod.portal;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static dev.qynl.myfirstmod.portal.AncientCityGate.*;
class AncientCityGateTest {
    private Map<Point,Cell> fixture(Frame f){
        Map<Point,Cell> cells=new HashMap<>();
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++)if(border(x,y))cells.put(f.at(x,y),Cell.FRAME);
        return cells;
    }
    @Test void acceptsEveryFrameBlockAndBothAxes(){
        for(boolean axis:new boolean[]{true,false}){
            var f=new Frame(new Point(-31,-42,67),axis);var cells=fixture(f);
            assertEquals(56,cells.size());
            for(var point:cells.keySet())assertEquals(f,find(point,p->cells.getOrDefault(p,Cell.EMPTY)).orElseThrow());
        }
    }
    @Test void blocksEveryObstructionWithoutChangingInput(){
        var f=new Frame(new Point(1,2,3),true);
        for(int y=1;y<HEIGHT-1;y++)for(int x=1;x<WIDTH-1;x++){
            var cells=fixture(f);cells.put(f.at(x,y),Cell.BLOCKED);var copy=new HashMap<>(cells);
            assertTrue(find(f.at(0,0),p->cells.getOrDefault(p,Cell.EMPTY)).isEmpty());assertEquals(copy,cells);
        }
    }
    @Test void rejectsEveryMissingFrameCell(){
        var f=new Frame(new Point(0,0,0),false);
        for(var p:fixture(f).keySet()){
            var cells=fixture(f);cells.remove(p);
            assertFalse(valid(f,q->cells.getOrDefault(q,Cell.EMPTY)));
        }
    }
    @Test void preservesAlreadyOpenOrPartialGates(){
        var f=new Frame(new Point(0,0,0),false);var cells=fixture(f);
        for(int y=1;y<HEIGHT-1;y++)for(int x=1;x<WIDTH-1;x++){
            cells.put(f.at(x,y),Cell.PORTAL);assertTrue(valid(f,p->cells.getOrDefault(p,Cell.EMPTY)));
        }
        assertEquals(120,cells.values().stream().filter(c->c==Cell.PORTAL).count());
    }
    @Test void rejectsLegacySmallFramesAndNonFrameClicks(){
        Map<Point,Cell> cells=new HashMap<>();
        for(int y=0;y<5;y++)for(int x=0;x<4;x++)if(x==0||x==3||y==0||y==4)cells.put(new Point(x,y,0),Cell.FRAME);
        assertTrue(find(new Point(0,0,0),p->cells.getOrDefault(p,Cell.EMPTY)).isEmpty());
        assertTrue(find(new Point(1,1,0),p->cells.getOrDefault(p,Cell.EMPTY)).isEmpty());
    }
}
