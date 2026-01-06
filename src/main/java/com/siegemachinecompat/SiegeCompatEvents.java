package com.siegemachinecompat;

import com.siegemachinecompat.ai.RecruitOperateAnyRangedMachineGoal;
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
    }
}
