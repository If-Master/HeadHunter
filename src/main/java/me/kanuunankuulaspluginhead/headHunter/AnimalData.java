package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

public class AnimalData {
    public static Sound getAnimalSound(EntityType entityType) {
        return switch (entityType) {
            case PIG -> Sound.ENTITY_PIG_AMBIENT;
            case COW -> Sound.ENTITY_COW_AMBIENT;
            case SHEEP -> Sound.ENTITY_SHEEP_AMBIENT;
            case CHICKEN -> Sound.ENTITY_CHICKEN_AMBIENT;
            case HORSE -> Sound.ENTITY_HORSE_AMBIENT;
            case DONKEY -> Sound.ENTITY_DONKEY_AMBIENT;
            case MULE -> Sound.ENTITY_MULE_AMBIENT;
            case LLAMA -> Sound.ENTITY_LLAMA_AMBIENT;
            case TRADER_LLAMA -> Sound.ENTITY_LLAMA_AMBIENT;
            case MOOSHROOM -> Sound.ENTITY_COW_AMBIENT;
            case GOAT -> Sound.ENTITY_GOAT_AMBIENT;
            case PLAYER -> Sound.ENTITY_PLAYER_BREATH;
            case CREAKING -> Sound.ENTITY_CREAKING_AMBIENT;

            case WOLF -> Sound.ENTITY_WOLF_AMBIENT;
            case CAT -> Sound.ENTITY_CAT_AMBIENT;
            case OCELOT -> Sound.ENTITY_OCELOT_AMBIENT;
            case FOX -> Sound.ENTITY_FOX_AMBIENT;
            case RABBIT -> Sound.ENTITY_RABBIT_HURT;
            case POLAR_BEAR -> Sound.ENTITY_POLAR_BEAR_AMBIENT;
            case PANDA -> Sound.ENTITY_PANDA_AMBIENT;
            case BEE -> Sound.ENTITY_BEE_LOOP;
            case BAT -> Sound.ENTITY_BAT_AMBIENT;
            case PARROT -> Sound.ENTITY_PARROT_AMBIENT;
            case TURTLE -> Sound.ENTITY_TURTLE_AMBIENT_LAND;
            case DOLPHIN -> Sound.ENTITY_DOLPHIN_AMBIENT;
            case FROG -> Sound.ENTITY_FROG_AMBIENT;
            case AXOLOTL -> Sound.ENTITY_AXOLOTL_IDLE_WATER;
            case ALLAY -> Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
            case ARMADILLO -> Sound.ENTITY_ARMADILLO_AMBIENT;
            case CAMEL -> Sound.ENTITY_CAMEL_AMBIENT;
            case SNIFFER -> Sound.ENTITY_SNIFFER_IDLE;
            case STRIDER -> Sound.ENTITY_STRIDER_AMBIENT;
            case DROWNED -> Sound.ENTITY_DROWNED_AMBIENT;
            case CAVE_SPIDER -> Sound.ENTITY_SPIDER_AMBIENT;
            case BOGGED -> Sound.ENTITY_BOGGED_AMBIENT;

            case ZOMBIE_HORSE -> Sound.ENTITY_ZOMBIE_HORSE_AMBIENT;
            case ZOMBIE_VILLAGER -> Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
            case SKELETON_HORSE -> Sound.ENTITY_SKELETON_HORSE_AMBIENT;
            case STRAY -> Sound.ENTITY_STRAY_AMBIENT;
            case HUSK -> Sound.ENTITY_HUSK_AMBIENT;

            case COD -> Sound.ENTITY_COD_HURT;
            case SALMON -> Sound.ENTITY_SALMON_HURT;
            case TROPICAL_FISH -> Sound.ENTITY_TROPICAL_FISH_HURT;
            case PUFFERFISH -> Sound.ENTITY_PUFFER_FISH_HURT;
            case SQUID -> Sound.ENTITY_SQUID_AMBIENT;
            case GLOW_SQUID -> Sound.ENTITY_GLOW_SQUID_AMBIENT;
            case TADPOLE -> Sound.ENTITY_TADPOLE_GROW_UP;
            case GIANT -> Sound.ENTITY_ZOMBIE_AMBIENT;

            case SPIDER -> Sound.ENTITY_SPIDER_AMBIENT;
            case CREEPER -> Sound.ENTITY_CREEPER_PRIMED;
            case ENDERMAN -> Sound.ENTITY_ENDERMAN_AMBIENT;
            case WITCH -> Sound.ENTITY_WITCH_AMBIENT;
            case VILLAGER -> Sound.ENTITY_VILLAGER_AMBIENT;
            case WANDERING_TRADER -> Sound.ENTITY_WANDERING_TRADER_AMBIENT;
            case IRON_GOLEM -> Sound.ENTITY_IRON_GOLEM_STEP;
            case SNOW_GOLEM -> Sound.BLOCK_SNOW_STEP;
            case WITHER -> Sound.ENTITY_WITHER_AMBIENT;
            case BLAZE -> Sound.ENTITY_BLAZE_AMBIENT;
            case GHAST -> Sound.ENTITY_GHAST_AMBIENT;
            case SLIME -> Sound.ENTITY_SLIME_SQUISH;
            case MAGMA_CUBE -> Sound.ENTITY_MAGMA_CUBE_SQUISH;
            case SILVERFISH -> Sound.ENTITY_SILVERFISH_AMBIENT;
            case ENDERMITE -> Sound.ENTITY_ENDERMITE_AMBIENT;
            case GUARDIAN -> Sound.ENTITY_GUARDIAN_AMBIENT;
            case ELDER_GUARDIAN -> Sound.ENTITY_ELDER_GUARDIAN_AMBIENT;
            case ILLUSIONER -> Sound.ENTITY_ILLUSIONER_AMBIENT;
            case SHULKER -> Sound.ENTITY_SHULKER_AMBIENT;
            case PHANTOM -> Sound.ENTITY_PHANTOM_AMBIENT;
            case VEX -> Sound.ENTITY_VEX_AMBIENT;
            case EVOKER -> Sound.ENTITY_EVOKER_AMBIENT;
            case VINDICATOR -> Sound.ENTITY_VINDICATOR_AMBIENT;
            case PILLAGER -> Sound.ENTITY_PILLAGER_AMBIENT;
            case RAVAGER -> Sound.ENTITY_RAVAGER_AMBIENT;
            case PIGLIN_BRUTE -> Sound.ENTITY_PIGLIN_BRUTE_AMBIENT;
            case ZOMBIFIED_PIGLIN -> Sound.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT;
            case HOGLIN -> Sound.ENTITY_HOGLIN_AMBIENT;
            case ZOGLIN -> Sound.ENTITY_ZOGLIN_AMBIENT;
            case WARDEN -> Sound.ENTITY_WARDEN_AMBIENT;
            case BREEZE -> Sound.ENTITY_BREEZE_IDLE_GROUND;

            default -> null;
        };
    }

    public static void initializeMobTextures() {
        // minecraft-heads.com

        /* =====================
               Mobs only through commands & Unkillable mobs
        ===================== */

        MainScript.mobTextures.put(EntityType.GIANT, "http://textures.minecraft.net/texture/56fc854bb84cf4b7697297973e02b79bc10698460b51a639c60e5e417734e11");
        MainScript.mobTextures.put(EntityType.ILLUSIONER, "http://textures.minecraft.net/texture/4639d325f4494258a473a93a3b47f34a0c51b3fceaf59fee87205a5e7ff31f68");
        MainScript.mobTextures.put(EntityType.CREAKING, "http://textures.minecraft.net/texture/de373fa534c7c3f9340d33c32c213402a2cf8917e750eb8bc8710fcd3ee9a15d");



        /* =====================
               Passive mobs
        ===================== */

        MainScript.mobTextures.put(EntityType.PIG, "http://textures.minecraft.net/texture/621668ef7cb79dd9c22ce3d1f3f4cb6e2559893b6df4a469514e667c16aa4");
        MainScript.mobTextures.put(EntityType.COW, "http://textures.minecraft.net/texture/5d6c6eda942f7f5f71c3161c7306f4aed307d82895f9d2b07ab4525718edc5");
        MainScript.mobTextures.put(EntityType.SHEEP, "http://textures.minecraft.net/texture/f31f9ccc6b3e32ecf13b8a11ac29cd33d18c95fc73db8a66c5d657ccb8be70");
        MainScript.mobTextures.put(EntityType.CHICKEN, "http://textures.minecraft.net/texture/1638469a599ceef7207537603248a9ab11ff591fd378bea4735b346a7fae893");
        MainScript.mobTextures.put(EntityType.RABBIT, "http://textures.minecraft.net/texture/7d1169b2694a6aba826360992365bcda5a10c89a3aa2b48c438531dd8685c3a7");
        MainScript.mobTextures.put(EntityType.HORSE, "http://textures.minecraft.net/texture/be78c4762674dde8b1a5a1e873b33f28e13e7c102b193f683549b38dc70e0");
        MainScript.mobTextures.put(EntityType.DONKEY, "http://textures.minecraft.net/texture/63a976c047f412ebc5cb197131ebef30c004c0faf49d8dd4105fca1207edaff3");
        MainScript.mobTextures.put(EntityType.MULE, "http://textures.minecraft.net/texture/a0486a742e7dda0bae61ce2f55fa13527f1c3b334c57c034bb4cf132fb5f5f");
        MainScript.mobTextures.put(EntityType.LLAMA, "http://textures.minecraft.net/texture/3776a78f96244e3da732faffd93a339834db27b6955bf7a9b24ae98125b7ed");
        MainScript.mobTextures.put(EntityType.TRADER_LLAMA, "http://textures.minecraft.net/texture/8424780b3c5c5351cf49fb5bf41fcb289491df6c430683c84d7846188db4f84d");
        MainScript.mobTextures.put(EntityType.CAMEL, "http://textures.minecraft.net/texture/7eb6ad908b8d5155bc4d249271ef6084d455dd0e70a4002eb148f9e20b9deb2c");
        MainScript.mobTextures.put(EntityType.MOOSHROOM, "http://textures.minecraft.net/texture/da82eb643d056eb26dae5e2ef77db0126f71d620e62dfd648ed4e6cecdf0cbb8");
        MainScript.mobTextures.put(EntityType.PARROT, "http://textures.minecraft.net/texture/bb108a3f397b57b5957f6340d7ea93a948424e529bb3b65672f2bdf6f61e6be6");
        MainScript.mobTextures.put(EntityType.TURTLE, "http://textures.minecraft.net/texture/761cd3e5f7a9bb58a0ed24df94e27513ea61c7a41f33e0180ad9c85f5327f7c5");
        MainScript.mobTextures.put(EntityType.STRIDER, "http://textures.minecraft.net/texture/e245e4760abf10f2900626914cf42f80440cd53099ae5529534f59824067dad6");
        MainScript.mobTextures.put(EntityType.SNIFFER, "http://textures.minecraft.net/texture/43f3be09a7353eeae94d88320cb5b242de2f719c0e5c16a486327c605db1d463");
        MainScript.mobTextures.put(EntityType.TROPICAL_FISH, "http://textures.minecraft.net/texture/36389acd7c8280d2c8085e6a6a91e182465347cc898db8c2d9bb148e0271c3e5");
        MainScript.mobTextures.put(EntityType.SQUID, "http://textures.minecraft.net/texture/464bdc6f600656511bef596c1a16aab1d3f5dbaae8bee19d5c04de0db21ce92c");
        MainScript.mobTextures.put(EntityType.SALMON, "http://textures.minecraft.net/texture/8aeb21a25e46806ce8537fbd6668281cf176ceafe95af90e94a5fd84924878");
        MainScript.mobTextures.put(EntityType.FOX, "http://textures.minecraft.net/texture/d631aac2c7d795d9816ca838b005927bb352b525d61de8ebb1461d304d1d2c00");
        MainScript.mobTextures.put(EntityType.BAT, "http://textures.minecraft.net/texture/9e99deef919db66ac2bd28d6302756ccd57c7f8b12b9dca8f41c3e0a04ac1cc");
        MainScript.mobTextures.put(EntityType.CAT, "http://textures.minecraft.net/texture/27f596afb869f806f61085a499b7e75148913d3e60601f166258dfbcb82a3bbf");
        MainScript.mobTextures.put(EntityType.GLOW_SQUID, "http://textures.minecraft.net/texture/4cb07d905888f8472252f9cfa39aa317babcad30af08cfe751adefa716b02036");
        MainScript.mobTextures.put(EntityType.OCELOT, "http://textures.minecraft.net/texture/cbb214a348529a0979574b87b06a480cc6177810f79491ce983f16dc3d844662");
        MainScript.mobTextures.put(EntityType.PUFFERFISH, "http://textures.minecraft.net/texture/2dd4b96726cfd36015af3778336c5226ae12fe80ca8afeee763f4542a883c282");
        MainScript.mobTextures.put(EntityType.SKELETON_HORSE, "http://textures.minecraft.net/texture/ac7d8a16d3f0f98b598df93f2c2d34e75171cd52dbf4a1211d7b84c019416a40");
        MainScript.mobTextures.put(EntityType.TADPOLE, "http://textures.minecraft.net/texture/b23ebf26b7a441e10a86fb5c2a5f3b519258a5c5dddd6a1a75549f517332815b");
        MainScript.mobTextures.put(EntityType.ZOMBIE_HORSE, "http://textures.minecraft.net/texture/d22950f2d3efddb18de86f8f55ac518dce73f12a6e0f8636d551d8eb480ceec");
        MainScript.mobTextures.put(EntityType.DOLPHIN, "http://textures.minecraft.net/texture/8e9688b950d880b55b7aa2cfcd76e5a0fa94aac6d16f78e833f7443ea29fed3");
        MainScript.mobTextures.put(EntityType.FROG, "http://textures.minecraft.net/texture/6157f19da077a3df49b2925fb6e8b400222ba6e559e86815f9b296d9e9667dd7");
        MainScript.mobTextures.put(EntityType.COD, "http://textures.minecraft.net/texture/7892d7dd6aadf35f86da27fb63da4edda211df96d2829f691462a4fb1cab0");


        /* =====================
*            Neutral mobs
        ===================== */


        MainScript.mobTextures.put(EntityType.WOLF, "http://textures.minecraft.net/texture/8f0b221786f193c06dd19a7875a903635113f84523927bb69764237fe20703de");
        MainScript.mobTextures.put(EntityType.ENDERMAN, "http://textures.minecraft.net/texture/59d22acc5aa80cd96b97ba68b1d6764030d87036e605d0b21538ee6239f1f414");
        MainScript.mobTextures.put(EntityType.PIGLIN_BRUTE, "http://textures.minecraft.net/texture/3e300e9027349c4907497438bac29e3a4c87a848c50b34c21242727b57f4e1cf");
        MainScript.mobTextures.put(EntityType.POLAR_BEAR, "http://textures.minecraft.net/texture/cd5d60a4d70ec136a658507ce82e3443cdaa3958d7fca3d9376517c7db4e695d");
        MainScript.mobTextures.put(EntityType.PANDA, "http://textures.minecraft.net/texture/d5c3d618a70cc062e2edfaed15173e2a32ab6d773cf6050452e1b97fc66fb388");
        MainScript.mobTextures.put(EntityType.GOAT, "http://textures.minecraft.net/texture/457a0d538fa08a7affe312903468861720f9fa34e86d44b89dcec5639265f03");
        MainScript.mobTextures.put(EntityType.BEE, "http://textures.minecraft.net/texture/722cc5ed03e6bdce81675fa44f740bb9aff0c1a662f185c31b05371c94fb7029");
        MainScript.mobTextures.put(EntityType.HOGLIN, "http://textures.minecraft.net/texture/7ad7b5aeb220c079e319cd70ac8800e80774a9313c22f38e77afb89999e6ec87");
        MainScript.mobTextures.put(EntityType.ZOGLIN, "http://textures.minecraft.net/texture/e67e18602e03035ad68967ce090235d8996663fb9ea47578d3a7ebbc42a5ccf9");

        /* =====================
                Hostile mobs
        ===================== */
        MainScript.mobTextures.put(EntityType.ZOMBIE_VILLAGER, "http://textures.minecraft.net/texture/8c7505f224d5164a117d8c69f015f99eff434471c8a2df907096c4242c3524e8");
        MainScript.mobTextures.put(EntityType.HUSK, "http://textures.minecraft.net/texture/9b9da6b8d06cd28d441398b96766c3b4f370de85c7898205e5c429f178a24597");
        MainScript.mobTextures.put(EntityType.DROWNED, "http://textures.minecraft.net/texture/c84df79c49104b198cdad6d99fd0d0bcf1531c92d4ab6269e40b7d3cbbb8e98c");
        MainScript.mobTextures.put(EntityType.STRAY, "http://textures.minecraft.net/texture/6572747a639d2240feeae5c81c6874e6ee7547b599e74546490dc75fa2089186");
        MainScript.mobTextures.put(EntityType.SPIDER, "http://textures.minecraft.net/texture/cd541541daaff50896cd258bdbdd4cf80c3ba816735726078bfe393927e57f1");
        MainScript.mobTextures.put(EntityType.CAVE_SPIDER, "http://textures.minecraft.net/texture/eec5574603f3048f21ad5a3c94d97115706011fe6ba67781091b8a9ac10af54f");
        MainScript.mobTextures.put(EntityType.SLIME, "http://textures.minecraft.net/texture/7f58355989d1eaad98f839fdf791be8fa8d892bdef9d308555fb7aff0dc9efb7");
        MainScript.mobTextures.put(EntityType.MAGMA_CUBE, "http://textures.minecraft.net/texture/38957d5023c937c4c41aa2412d43410bda23cf79a9f6ab36b76fef2d7c429");
        MainScript.mobTextures.put(EntityType.WITCH, "http://textures.minecraft.net/texture/fce6604157fc4ab5591e4bcf507a749918ee9c41e357d47376e0ee7342074c90");
        MainScript.mobTextures.put(EntityType.BLAZE, "http://textures.minecraft.net/texture/b78ef2e4cf2c41a2d14bfde9caff10219f5b1bf5b35a49eb51c6467882cb5f0");
        MainScript.mobTextures.put(EntityType.GHAST, "http://textures.minecraft.net/texture/504843421c218d0634455fdb1a6c5f7ae5b85098a50b12b9ed9d9310c84dc61b");
        MainScript.mobTextures.put(EntityType.RAVAGER, "http://textures.minecraft.net/texture/cd20bf52ec390a0799299184fc678bf84cf732bb1bd78fd1c4b441858f0235a8");
        MainScript.mobTextures.put(EntityType.VINDICATOR, "http://textures.minecraft.net/texture/9e1cab382458e843ac4356e3e00e1d35c36f449fa1a84488ab2c6557b392d");
        MainScript.mobTextures.put(EntityType.EVOKER, "http://textures.minecraft.net/texture/3433322e2ccbd9c55ef41d96f38dbc666c803045b24391ac9391dccad7cd");
        MainScript.mobTextures.put(EntityType.PILLAGER, "http://textures.minecraft.net/texture/32fb80a6b6833e31d9ce8313a54777645f9c1e55b810918a706e7bcc8d35a5a2");
        MainScript.mobTextures.put(EntityType.VEX, "http://textures.minecraft.net/texture/b663134d7306bb604175d2575d686714b04412fe501143611fcf3cc19bd70abe");
        MainScript.mobTextures.put(EntityType.SHULKER, "http://textures.minecraft.net/texture/76640530d98db934fc5b955ea23c11c80c4fdad061001e8a2913e38390df69a6");
        MainScript.mobTextures.put(EntityType.SILVERFISH, "http://textures.minecraft.net/texture/33397111bf3f2a59374c186fa8b7556c6e50423b116e252a89f6b4225b3980e4");
        MainScript.mobTextures.put(EntityType.PHANTOM, "http://textures.minecraft.net/texture/b1109d9c6786c747c87fc36c9a271d51ff201603c38d2615ac4803908680e988");
        MainScript.mobTextures.put(EntityType.GUARDIAN, "http://textures.minecraft.net/texture/afb3e8a7caddef673959bcf2fb8d8db5086086a42cbeb6e3b66f80016db64770");
        MainScript.mobTextures.put(EntityType.ELDER_GUARDIAN, "http://textures.minecraft.net/texture/30f868caf19cf2124f0fef98e6b8773d27fbf42d93aab06b22ee033b2aee6447");
        MainScript.mobTextures.put(EntityType.WARDEN, "http://textures.minecraft.net/texture/cf6481c7c435c34f21dff1043a4c7034c445a383a5435fa1f2a503a348afd62f");
        MainScript.mobTextures.put(EntityType.BOGGED, "http://textures.minecraft.net/texture/a3b9003ba2d05562c75119b8a62185c67130e9282f7acbac4bc2824c21eb95d9");
        MainScript.mobTextures.put(EntityType.BREEZE, "http://textures.minecraft.net/texture/cd6e602f76f80c0657b5aed64e267eeea702b31e6dae86346c8506f2535ced02");
        MainScript.mobTextures.put(EntityType.ZOMBIFIED_PIGLIN, "http://textures.minecraft.net/texture/7eabaecc5fae5a8a49c8863ff4831aaa284198f1a2398890c765e0a8de18da8c");
        MainScript.mobTextures.put(EntityType.ENDERMITE, "http://textures.minecraft.net/texture/5a1a0831aa03afb4212adcbb24e5dfaa7f476a1173fce259ef75a85855");

// =====================
// Utility / Friendly Constructs
// =====================
        MainScript.mobTextures.put(EntityType.IRON_GOLEM, "http://textures.minecraft.net/texture/da6e0429ccaabb6f5f0c5d513c795bed6d80fce72f57f4bc3a616aee23e12572");
        MainScript.mobTextures.put(EntityType.SNOW_GOLEM, "http://textures.minecraft.net/texture/e6f20aec528c3968dd8164f9d9336b081b3a2c7ecf189cf73df6f925e5a4ed14");
        MainScript.mobTextures.put(EntityType.ALLAY, "http://textures.minecraft.net/texture/df5de940bfe499c59ee8dac9f9c3919e7535eff3a9acb16f4842bf290f4c679f");
        MainScript.mobTextures.put(EntityType.ARMADILLO, "http://textures.minecraft.net/texture/9164ed0e0ef69b0ce7815e4300b4413a4828fcb0092918543545a418a48e0c3c");
        MainScript.mobTextures.put(EntityType.AXOLOTL, "http://textures.minecraft.net/texture/21c3aa0d539208b47972bf8e72f0505cdcfb8d7796b2fcf85911ce94fd0193d0");
        MainScript.mobTextures.put(EntityType.VILLAGER, "http://textures.minecraft.net/texture/350ba6b7690363737a46c85a2795058ff166eb20a7b5c9fa3fb2b391199822d2");
        MainScript.mobTextures.put(EntityType.WANDERING_TRADER, "http://textures.minecraft.net/texture/ee011aac817259f2b48da3e5ef266094703866608b3d7d1754432bf249cd2234");

// =====================
// Bosses
// =====================
        MainScript.mobTextures.put(EntityType.WITHER, "http://textures.minecraft.net/texture/63b6bb53e33db2c19ae88b5ce7e24e8b5f3137c411b4f704f0aebd5deee15694");

    }


}
