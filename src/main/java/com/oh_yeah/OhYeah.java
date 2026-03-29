package com.oh_yeah;

import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.block.ModBlocks;
import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.ModSoundEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OhYeah implements ModInitializer {
	public static final String MOD_ID = "oh-yeah";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		OhYeahConfigManager.initialize();
		ModSoundEvents.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModEntityTypes.initialize();
		LOGGER.info("Initialized {}", MOD_ID);
	}
}
