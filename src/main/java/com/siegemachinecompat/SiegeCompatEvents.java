package com.siegemachinecompat;

import com.siegemachinecompat.ai.RecruitClimbSiegeLadderGoal;
import com.siegemachinecompat.ai.RecruitOperateAnyRangedMachineGoal;
import com.siegemachinecompat.ai.RecruitOperateAnyUseableMachineGoal;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "siege_machine_compat")
public class SiegeCompatEvents {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {

        if (event.getEntity() instanceof BowmanEntity bowman) {
            bowman.goalSelector.addGoal(1, new RecruitOperateAnyRangedMachineGoal(bowman));
        }

        if (event.getEntity() instanceof AbstractRecruitEntity recruit) {
            recruit.goalSelector.addGoal(1, new RecruitClimbSiegeLadderGoal(recruit));
            recruit.goalSelector.addGoal(1, new RecruitOperateAnyUseableMachineGoal(recruit));
        }
    }
}
