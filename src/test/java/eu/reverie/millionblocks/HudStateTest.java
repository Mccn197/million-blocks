package eu.reverie.millionblocks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudStateTest {
    @Test void formatsLiveCounterAndOptionalFields() {
        var s=new HudState(1852080,1000000,2,true,true,true,true,true,0);
        assertEquals("1 852 080",s.countText());assertEquals("/ 1 000 000",s.goalText());
        assertEquals("100%",s.percentText());assertEquals("x2",s.multiplierText());
        var hidden=new HudState(10,100,1,true,false,false,false,false,0);
        assertEquals("",hidden.goalText());assertEquals("",hidden.percentText());assertEquals("",hidden.multiplierText());
    }
    @Test void crackRespondsToMiningAndReturnsToIdle() {
        long now=1_000_000_000L;
        var s=new HudState(1,100,1,true,true,true,true,true,now);
        assertEquals(1,s.crack(now));assertEquals(2,s.crack(now+150_000_000));
        assertEquals(3,s.crack(now+300_000_000));assertEquals(4,s.crack(now+450_000_000));
        assertEquals(0,s.crack(now+600_000_000));assertEquals(0,HudState.EMPTY.crack(now));
        assertEquals(0,s.crack(now-1));
    }
    @Test void continuousMiningDoesNotFreezeTheFirstCrackFrame() {
        long start=1_000_000_000L;
        assertEquals(start,HudState.animationStart(start,start+100_000_000));
        assertEquals(start,HudState.animationStart(start,start+450_000_000));
        assertEquals(start+600_000_000,HudState.animationStart(start,start+600_000_000));
    }
    @Test void multiplierChangesScoreAndNeverOverflows() {
        assertEquals(12,HudState.addSaturated(10,1,2));assertEquals(11,HudState.addSaturated(10,1,1));
        assertEquals(Long.MAX_VALUE,HudState.addSaturated(Long.MAX_VALUE-1,2,2));
        assertEquals(Long.MAX_VALUE,HudState.addSaturated(10,Long.MAX_VALUE,2));
        assertEquals(10,HudState.addSaturated(10,-1,2));
    }
    @Test void sanitizesInvalidConfigurationAndCounts() {
        var s=new HudState(-1,0,9,true,true,true,true,true,0);
        assertEquals(0,s.count());assertEquals(1,s.target());assertEquals(1,s.multiplier());assertEquals(0,s.percent());
        assertEquals(32,new HudState(325000,1000000,1,true,true,true,true,true,0).percent());
    }
}
