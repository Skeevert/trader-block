package net.fabricmc.tradingblockmod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;

public class TradingBlock extends Block implements BlockEntityProvider {
	public TradingBlock(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient()) {
			player.sendMessage(Text.of("Oh hi!"), false);
		}
		
		
		
		BlockEntity ent = world.getBlockEntity(pos);
		
		if (ent instanceof TradingBlockEntity) {
			((TradingBlockEntity)ent).onInteract(state, world, pos, player, hand, hit);
		}
		
		
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos arg0, BlockState arg1) {
		return new TradingBlockEntity(arg0, arg1);
	}
}
