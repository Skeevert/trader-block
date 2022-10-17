package net.fabricmc.tradingblockmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingBlockMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("modid");
	public static final TradingBlock TRADING_BLOCK = new TradingBlock(FabricBlockSettings.of(Material.WOOD).strength(2.0f));
	public static final BlockEntityType<TradingBlockEntity> TRADING_BLOCK_ENTITY = Registry.register(
	        Registry.BLOCK_ENTITY_TYPE,
	        new Identifier("trading_block_mod", "trading_block_entity"),
	        FabricBlockEntityTypeBuilder.create(TradingBlockEntity::new, TRADING_BLOCK).build()
	    );
	
	@Override
	public void onInitialize() {
		Registry.register(Registry.BLOCK, new Identifier("trading_block_mod", "trading_block"), TRADING_BLOCK);
		Registry.register(Registry.ITEM, new Identifier("trading_block_mod", "trading_block"), new BlockItem(TRADING_BLOCK, new FabricItemSettings().group(ItemGroup.DECORATIONS)));
		
		LOGGER.info("Trading block mod initialized");
	}
}
