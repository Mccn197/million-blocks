package eu.reverie.millionblocks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnlockStageTest {
    @Test void stageUsesIncrementalProgress() {
        var s=new UnlockService.Stage(ToolId.MINING_HAMMER,1000,5000,3000,50);
        assertEquals(2000,s.currentStage());
        assertEquals(4000,s.stageSize());
        assertEquals("mining_hammer",s.id());
        assertEquals("Mining Hammer",s.itemText());
    }
    @Test void clampsUnlockAndCompletionValues() {
        var stage=new UnlockService.Stage(ToolId.LASER,175000,325000,1000000,100);
        assertEquals(150000,stage.currentStage());
        var done=new UnlockService.Stage(null,800000,800000,1000000,100);
        assertEquals("complete",done.id());
        assertEquals(1,done.stageSize());
    }
    @Test void hudFramesTrackBothIndependentProgressBars() {
        var s=new HudState(40000,1000000,1,true,true,true,true,true,0,
            "tnt_launcher","TNT Launcher",15000,50000,30);
        assertEquals(0,s.progressFrame()); // 40k / 1m -> under 5% frame
        assertEquals(6,s.unlockFrame()); // next unlock uses 30%, not 4%
        assertEquals("15 000 / 50 000",s.unlockText());
    }
}
