package net.fabricmc.traderblockmod;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Random;

import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;

public class TraderBlockEntity extends BlockEntity implements Merchant {
	private TradeOfferList offers = new TradeOfferList();
	private VillagerProfession profession = VillagerProfession.NONE;
	
	private int experience = 0;
	private final int[] LEVEL_BASE_EXPERIENCE = new int[] {0, 10, 70, 150, 250};
	private int level = 1;
	
	private final Random rand = new Random();
	
	private World world;
	private PlayerEntity customer;
	
	private long lastRestockTime = 0;
	private long restockTime = 24000L;
	
	private BlockPos adjacentPositions[] = {null, null, null, null};
	
	public TraderBlockEntity(BlockPos pos, BlockState state) {
		super(TraderBlockMod.TRADER_BLOCK_ENTITY, pos, state);
		fillOfferList();
	}
	
	public ActionResult onInteract(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		this.world = world;
				
		checkProfession(pos);
		
		// We don't have the AI, so that's the solution of the restock problem
		// TODO: Maybe increase restock time to help villagers stay somewhat viable
		long currentTime = this.world.getTimeOfDay();
		if (this.lastRestockTime + restockTime < currentTime) {
			fullRestock();
			this.lastRestockTime = currentTime;
		}
		
		this.setCustomer(player);
		this.sendOffers(customer, Text.of("Trader block"), this.level);
		
		return ActionResult.SUCCESS;
	}

	@Override
	public PlayerEntity getCustomer() {
		return customer;
	}

	@Override
	public int getExperience() {
		return experience;
	}

	@Override
	public TradeOfferList getOffers() {
		return offers;
	}

	@Override
	public SoundEvent getYesSound() {
		return null;
	}

	@Override
	public boolean isClient() {
		// TODO Auto-generated method stub
		return this.world.isClient;
	}

	@Override
	public boolean isLeveledMerchant() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onSellingItem(ItemStack stack) {		
	}

	@Override
	public void setCustomer(PlayerEntity arg0) {
		this.customer = arg0;
	}

	@Override
	public void setExperienceFromServer(int experience) {
		this.experience = experience;
	}

	@Override
	public void setOffersFromServer(TradeOfferList offers) {
		this.offers = offers;
	}

	@Override
	public void trade(TradeOffer offer) {
		offer.use();
		this.experience += offer.getMerchantExperience();
		while (checkLevelRequirements()) {
			level++;
			fillOfferList();
		}
		this.markDirty();
	}
	
	@Override
	public boolean canRefreshTrades() {
		return true;
	}
	
    @Override           
    public void sendOffers(PlayerEntity player2, Text test, int levelProgress) {    
        TradeOfferList tradeOfferList;    
        OptionalInt optionalInt = player2.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, player) -> new TraderBlockScreenHandler(syncId, playerInventory, this), test));    
                        
        if (optionalInt.isPresent() && !(tradeOfferList = this.getOffers()).isEmpty()) {    
            player2.sendTradeOffers(optionalInt.getAsInt(), tradeOfferList, levelProgress, this.getExperience(), this.isLeveledMerchant(), this.canRefreshTrades());    
        }               
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) { 	
    	nbt.put("offers", this.offers.toNbt());
    	
    	// Dirty hack to simplify profession serialization
    	VillagerData ser = new VillagerData(VillagerType.DESERT, this.profession, this.level);
    	VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, ser).result().ifPresent(nbtElement -> nbt.put("merchData", (NbtElement)nbtElement));
    	
    	nbt.putInt("experience", experience);
    	nbt.putLong("lastRestockTime", lastRestockTime);
    	super.writeNbt(nbt);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
    	super.readNbt(nbt);
    	
    	if (nbt.contains("merchData")) {
    		DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic<NbtElement>(NbtOps.INSTANCE, nbt.get("merchData")));
    		dataResult.result().ifPresent((merchData -> {
    			this.profession = ((VillagerData) merchData).getProfession();
    			this.level = ((VillagerData) merchData).getLevel();
    		}));
    	}
    	
    	if (nbt.contains("offers")) {
    		this.offers = new TradeOfferList(nbt.getCompound("offers"));
    		
    	}
    	
    	if (nbt.contains("experience")) {
    		this.experience = nbt.getInt("experience");
    	}
    	
    	if (nbt.contains("lastRestockTime")) {
    		this.lastRestockTime = nbt.getLong("lastRestockTime");
    	}
    }
	
	private boolean checkLevelRequirements() {
		if (level < 1 || level >= 5) {
			return false;
		}
		
		return this.experience >= LEVEL_BASE_EXPERIENCE[level];
	}
	
	private void fillOfferList() {
		int newOffersAmount = 2 * level - offers.size();
		
		// Hopefully I'm getting this right
		if (newOffersAmount <= 0) {
			return;
		}
		
		Int2ObjectMap<TradeOffers.Factory[]> offerMap = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession);
		if (offerMap == null || offerMap.isEmpty()) {
			return;
		}
		
		TradeOffers.Factory[] pool = (TradeOffers.Factory[])offerMap.get(level);
		HashSet<Integer> offersToAdd = Sets.newHashSet();
		
		for (int i = 0; i < newOffersAmount; ++i) {
			offersToAdd.add(rand.nextInt(pool.length));
		}
		
		for (Integer index : offersToAdd) {
			TradeOffers.Factory factory = pool[index];
			TradeOffer offer = factory.create(null, rand);
			if (offer != null) {
				offers.add(offer);
			}
		}
	}
	
	private void fullRestock() {
		for (TradeOffer offer : this.offers) {
			offer.updateDemandBonus();
			offer.resetUses();
		}
		this.markDirty();
	}
	
	private void checkProfession(BlockPos pos) {
		if (this.world instanceof ServerWorld) {
			ServerWorld servWorld = (ServerWorld) this.world;
			
			adjacentPositions[0] = pos.north();
			adjacentPositions[1] = pos.east();
			adjacentPositions[2] = pos.south();
			adjacentPositions[3] = pos.west();
			

			
			for (BlockPos adjPos : adjacentPositions) {
				PointOfInterestType poiType;
				poiType = servWorld.getPointOfInterestStorage().getType(adjPos).orElse(null);
				if (poiType != null &&  poiType != this.profession.getWorkStation()) {
					VillagerProfession newProf = Registry.VILLAGER_PROFESSION.stream().filter((prof) -> prof.getWorkStation() == poiType).findAny().orElse(VillagerProfession.NONE);
					if (newProf != VillagerProfession.NONE) {
						if (newProf != this.profession) {
							this.offers.clear();
							this.level = 1;
							this.experience = 0;
							this.profession = newProf;
							fillOfferList();
						}
						return;
					}
				}
			}
			this.offers.clear();
			this.level = 1;
			this.experience = 0;
			this.profession = VillagerProfession.NONE;
		}
	}
}
