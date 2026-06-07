# LegendaryMonuments-7.8.jar

## fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "legendarymonuments",
  "version": "7.8",
  "name": "Legendary Monuments",
  "description": "Adds new mechanics ands structures to obtain Legendary and Mythical pokemon.",
  "authors": [
    "JorgaoMC"
  ],
  "contact": {
    "homepage": "https://fabricmc.net/",
    "sources": "https://github.com/FabricMC/fabric-example-mod"
  },
  "entrypoints": {
    "main": [
      "github.jorgaomc.LegendaryMonuments"
    ],
    "client": [
      "github.jorgaomc.LegendaryMonumentsClient"
    ],
    "fabric-datagen": [
      "github.jorgaomc.LegendaryMonumentsDataGenerator"
    ],
    "terrablender": [
      "github.jorgaomc.world.biome.LegendaryMonumentsTerraBlender"
    ]
  },
  "license": "MPL-2.0",
  "icon": "assets/legendarymonuments/icon.png",
  "environment": "*",
  "mixins": [
    "legendarymonuments.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.16.10",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "fabric-api": "*",
    "cobblemon": ">=1.7.0",
    "chipped": "*",
    "mega_showdown": "*",
    "cobblefurnies": "*",
    "terrablender": "*",
    "accessories": ">=1.1.0-beta.52+1.21.1"
  }
}
```

## Namespaces

- assets: ['legendarymonuments']

- data: ['accessories', 'cobblemon', 'cobblemon_drops', 'legendarymonuments', 'mega_showdown', 'minecraft']

## Lang files

- assets/legendarymonuments/lang/en_us.json
- assets/legendarymonuments/lang/es_es.json
- assets/legendarymonuments/lang/ja_jp.json
- assets/legendarymonuments/lang/ru_ru.json
- assets/legendarymonuments/lang/zh_cn.json
- assets/legendarymonuments/lang/zh_hk.json
- assets/legendarymonuments/lang/zh_tw.json

## Interesting JSON/resources

- assets/legendarymonuments/blockstates/ancient_rubble_ore.json
- assets/legendarymonuments/blockstates/cobalion_footprints.json
- assets/legendarymonuments/blockstates/correct_regi_light.json
- assets/legendarymonuments/blockstates/cosmic_dust_block.json
- assets/legendarymonuments/blockstates/deepslate_galar_particle_ore.json
- assets/legendarymonuments/blockstates/deepslate_regidrac_ore.json
- assets/legendarymonuments/blockstates/deepslate_regiglace_ore.json
- assets/legendarymonuments/blockstates/deepslate_regimetal_ore.json
- assets/legendarymonuments/blockstates/deepslate_registone_ore.json
- assets/legendarymonuments/blockstates/deepslate_regivolt_ore.json
- assets/legendarymonuments/blockstates/dialga_pedestal.json
- assets/legendarymonuments/blockstates/distortion_button.json
- assets/legendarymonuments/blockstates/distortion_cobblestone.json
- assets/legendarymonuments/blockstates/distortion_cobblestone_bricks.json
- assets/legendarymonuments/blockstates/distortion_cobblestone_bricks_slab.json
- assets/legendarymonuments/blockstates/distortion_cobblestone_bricks_stairs.json
- assets/legendarymonuments/blockstates/distortion_cobblestone_bricks_wall.json
- assets/legendarymonuments/blockstates/distortion_crystal.json
- assets/legendarymonuments/blockstates/distortion_crystal_block.json
- assets/legendarymonuments/blockstates/distortion_deepslate.json
- assets/legendarymonuments/blockstates/distortion_deepslate_bricks.json
- assets/legendarymonuments/blockstates/distortion_deepslate_bricks_slab.json
- assets/legendarymonuments/blockstates/distortion_deepslate_bricks_stairs.json
- assets/legendarymonuments/blockstates/distortion_deepslate_bricks_wall.json
- assets/legendarymonuments/blockstates/distortion_deepslate_iron_ore.json
- assets/legendarymonuments/blockstates/distortion_door.json
- assets/legendarymonuments/blockstates/distortion_fence.json
- assets/legendarymonuments/blockstates/distortion_fence_gate.json
- assets/legendarymonuments/blockstates/distortion_hanging_sign.json
- assets/legendarymonuments/blockstates/distortion_iron_ore.json
- assets/legendarymonuments/blockstates/distortion_leaves.json
- assets/legendarymonuments/blockstates/distortion_log.json
- assets/legendarymonuments/blockstates/distortion_origin_ore.json
- assets/legendarymonuments/blockstates/distortion_planks.json
- assets/legendarymonuments/blockstates/distortion_pressure_plate.json
- assets/legendarymonuments/blockstates/distortion_sapling.json
- assets/legendarymonuments/blockstates/distortion_sign.json
- assets/legendarymonuments/blockstates/distortion_slab.json
- assets/legendarymonuments/blockstates/distortion_stairs.json
- assets/legendarymonuments/blockstates/distortion_stone.json
- assets/legendarymonuments/blockstates/distortion_trapdoor.json
- assets/legendarymonuments/blockstates/distortion_wall_hanging_sign.json
- assets/legendarymonuments/blockstates/distortion_wall_sign.json
- assets/legendarymonuments/blockstates/dragon_golem_block.json
- assets/legendarymonuments/blockstates/dream_catcher.json
- assets/legendarymonuments/blockstates/electric_golem_block.json
- assets/legendarymonuments/blockstates/elekidrago_lock.json
- assets/legendarymonuments/blockstates/entei_pedestal.json
- assets/legendarymonuments/blockstates/eternatus_cocoon.json
- assets/legendarymonuments/blockstates/false_regi_light.json
- assets/legendarymonuments/blockstates/firescourge_shrine.json
- assets/legendarymonuments/blockstates/firescourge_stake.json
- assets/legendarymonuments/blockstates/galar_particle_block.json
- assets/legendarymonuments/blockstates/galar_particle_ore.json
- assets/legendarymonuments/blockstates/galarian_torch.json
- assets/legendarymonuments/blockstates/galarian_urn_of_embers_block.json
- assets/legendarymonuments/blockstates/galarian_urn_of_frost_block.json
- assets/legendarymonuments/blockstates/galarian_urn_of_storms_block.json
- assets/legendarymonuments/blockstates/galarian_wall_torch.json
- assets/legendarymonuments/blockstates/giratina_pedestal.json
- assets/legendarymonuments/blockstates/grasswither_shrine.json
- assets/legendarymonuments/blockstates/grasswither_stake.json
- assets/legendarymonuments/blockstates/groundblight_shrine.json
- assets/legendarymonuments/blockstates/groundblight_stake.json
- assets/legendarymonuments/blockstates/heatran_pedestal.json
- assets/legendarymonuments/blockstates/ho_oh_pedestal.json
- assets/legendarymonuments/blockstates/hoopa_boss_summon.json
- assets/legendarymonuments/blockstates/hoopa_pedestal.json
- assets/legendarymonuments/blockstates/ice_golem_block.json
- assets/legendarymonuments/blockstates/icerend_shrine.json
- assets/legendarymonuments/blockstates/icerend_stake.json
- assets/legendarymonuments/blockstates/ilex_shrine.json
- assets/legendarymonuments/blockstates/kyurem_pedestal.json
- assets/legendarymonuments/blockstates/latias_pedestal.json
- assets/legendarymonuments/blockstates/lugia_lock.json
- assets/legendarymonuments/blockstates/lugia_lock_activated.json
- assets/legendarymonuments/blockstates/lugia_pedestal.json
- assets/legendarymonuments/blockstates/meltan_box.json
- assets/legendarymonuments/blockstates/mew_pedestal.json
- assets/legendarymonuments/blockstates/origin_block.json
- assets/legendarymonuments/blockstates/origin_glass.json
- assets/legendarymonuments/blockstates/origin_glass_slab.json
- assets/legendarymonuments/blockstates/origin_glass_stairs.json
- assets/legendarymonuments/blockstates/palkia_pedestal.json
- assets/legendarymonuments/blockstates/pedestal.json
- assets/legendarymonuments/blockstates/pokemon_trial_spawner.json
- assets/legendarymonuments/blockstates/raikou_pedestal.json
- assets/legendarymonuments/blockstates/regi_statue.json
- assets/legendarymonuments/blockstates/regice_lock.json
- assets/legendarymonuments/blockstates/regigigas_lock.json
- assets/legendarymonuments/blockstates/regirock_lock.json
- assets/legendarymonuments/blockstates/registeel_lock.json
- assets/legendarymonuments/blockstates/reshiram_pedestal.json
- assets/legendarymonuments/blockstates/rock_golem_block.json
- assets/legendarymonuments/blockstates/sanctuary_block.json
- assets/legendarymonuments/blockstates/sandstone_pressure_plate.json
- assets/legendarymonuments/blockstates/steel_golem_block.json
- assets/legendarymonuments/blockstates/suicune_pedestal.json
- assets/legendarymonuments/blockstates/suitcase_block.json
- assets/legendarymonuments/blockstates/temple_lock.json
- assets/legendarymonuments/blockstates/terrakion_footprints.json
- assets/legendarymonuments/blockstates/urn_of_embers_block.json
- assets/legendarymonuments/blockstates/urn_of_frost_block.json
- assets/legendarymonuments/blockstates/urn_of_storms_block.json
- assets/legendarymonuments/blockstates/victini_lock.json
- assets/legendarymonuments/blockstates/virizion_footprints.json
- assets/legendarymonuments/blockstates/zacian_pedestal.json
- assets/legendarymonuments/blockstates/zamazenta_pedestal.json
- assets/legendarymonuments/blockstates/zekrom_pedestal.json
- assets/legendarymonuments/lang/en_us.json
- assets/legendarymonuments/lang/es_es.json
- assets/legendarymonuments/lang/ja_jp.json
- assets/legendarymonuments/lang/ru_ru.json
- assets/legendarymonuments/lang/zh_cn.json
- assets/legendarymonuments/lang/zh_hk.json
- assets/legendarymonuments/lang/zh_tw.json
- assets/legendarymonuments/models/block/ancient_rubble_ore.json
- assets/legendarymonuments/models/block/cobalion_footprints.json
- assets/legendarymonuments/models/block/correct_regi_light_off.json
- assets/legendarymonuments/models/block/correct_regi_light_on.json
- assets/legendarymonuments/models/block/cosmic_dust_block.json
- assets/legendarymonuments/models/block/deepslate_galar_particle_ore.json
- assets/legendarymonuments/models/block/dialga_pedestal.json
- assets/legendarymonuments/models/block/distortion_button.json
- assets/legendarymonuments/models/block/distortion_button_inventory.json
- assets/legendarymonuments/models/block/distortion_button_pressed.json
- assets/legendarymonuments/models/block/distortion_cobblestone.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_slab.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_slab_top.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_stairs.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_stairs_inner.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_stairs_outer.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_wall_inventory.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_wall_post.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_wall_side.json
- assets/legendarymonuments/models/block/distortion_cobblestone_bricks_wall_side_tall.json
- assets/legendarymonuments/models/block/distortion_crystal.json
- assets/legendarymonuments/models/block/distortion_crystal_block.json
- assets/legendarymonuments/models/block/distortion_deepslate.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_slab.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_slab_top.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_stairs.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_stairs_inner.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_stairs_outer.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_wall_inventory.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_wall_post.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_wall_side.json
- assets/legendarymonuments/models/block/distortion_deepslate_bricks_wall_side_tall.json
- assets/legendarymonuments/models/block/distortion_deepslate_iron_ore.json
- assets/legendarymonuments/models/block/distortion_door_bottom_left.json
- assets/legendarymonuments/models/block/distortion_door_bottom_left_open.json
- assets/legendarymonuments/models/block/distortion_door_bottom_right.json
- assets/legendarymonuments/models/block/distortion_door_bottom_right_open.json
- assets/legendarymonuments/models/block/distortion_door_top_left.json
- assets/legendarymonuments/models/block/distortion_door_top_left_open.json
- assets/legendarymonuments/models/block/distortion_door_top_right.json
- assets/legendarymonuments/models/block/distortion_door_top_right_open.json
- assets/legendarymonuments/models/block/distortion_fence_gate.json
- assets/legendarymonuments/models/block/distortion_fence_gate_open.json
- assets/legendarymonuments/models/block/distortion_fence_gate_wall.json
- assets/legendarymonuments/models/block/distortion_fence_gate_wall_open.json
- assets/legendarymonuments/models/block/distortion_fence_post.json
- assets/legendarymonuments/models/block/distortion_fence_side.json
- assets/legendarymonuments/models/block/distortion_hanging_sign.json
- assets/legendarymonuments/models/block/distortion_iron_ore.json
- assets/legendarymonuments/models/block/distortion_leaves.json
- assets/legendarymonuments/models/block/distortion_log.json
- assets/legendarymonuments/models/block/distortion_origin_ore.json
- assets/legendarymonuments/models/block/distortion_planks.json
- assets/legendarymonuments/models/block/distortion_pressure_plate.json
- assets/legendarymonuments/models/block/distortion_pressure_plate_down.json
- assets/legendarymonuments/models/block/distortion_sapling.json
- assets/legendarymonuments/models/block/distortion_sign.json
- assets/legendarymonuments/models/block/distortion_slab.json
- assets/legendarymonuments/models/block/distortion_slab_top.json
- assets/legendarymonuments/models/block/distortion_stairs.json
- assets/legendarymonuments/models/block/distortion_stairs_inner.json
- assets/legendarymonuments/models/block/distortion_stairs_outer.json
- assets/legendarymonuments/models/block/distortion_stone.json
- assets/legendarymonuments/models/block/distortion_trapdoor_bottom.json
- assets/legendarymonuments/models/block/distortion_trapdoor_open.json
- assets/legendarymonuments/models/block/distortion_trapdoor_top.json
- assets/legendarymonuments/models/block/distortion_wall_hanging_sign.json
- assets/legendarymonuments/models/block/distortion_wall_sign.json
- assets/legendarymonuments/models/block/dragon_golem_block.json
- assets/legendarymonuments/models/block/dream_catcher.json
- assets/legendarymonuments/models/block/electric_golem_block.json
- assets/legendarymonuments/models/block/elekidrago_lock.json
- assets/legendarymonuments/models/block/elekidrago_lock_activated.json
- assets/legendarymonuments/models/block/entei_pedestal.json
- assets/legendarymonuments/models/block/eternatus_cocoon.json
- assets/legendarymonuments/models/block/false_regi_light_off.json
- assets/legendarymonuments/models/block/false_regi_light_on.json
- assets/legendarymonuments/models/block/firescourge_shrine.json
- assets/legendarymonuments/models/block/firescourge_stake.json
- assets/legendarymonuments/models/block/galar_particle_block.json
- assets/legendarymonuments/models/block/galar_particle_ore.json
- assets/legendarymonuments/models/block/galarian_torch.json
- assets/legendarymonuments/models/block/galarian_urn_of_embers_block.json
- assets/legendarymonuments/models/block/galarian_urn_of_frost_block.json
- assets/legendarymonuments/models/block/galarian_urn_of_storms_block.json
- assets/legendarymonuments/models/block/galarian_wall_torch.json
- assets/legendarymonuments/models/block/giratina_pedestal.json
- assets/legendarymonuments/models/block/grasswither_shrine.json
- assets/legendarymonuments/models/block/grasswither_stake.json
- assets/legendarymonuments/models/block/groundblight_shrine.json
- assets/legendarymonuments/models/block/groundblight_stake.json
- assets/legendarymonuments/models/block/heatran_pedestal.json
- assets/legendarymonuments/models/block/ho_oh_pedestal.json
- assets/legendarymonuments/models/block/hoopa_boss_summon.json
- assets/legendarymonuments/models/block/hoopa_pedestal.json
- assets/legendarymonuments/models/block/ice_golem_block.json
- assets/legendarymonuments/models/block/icerend_shrine.json
- assets/legendarymonuments/models/block/icerend_stake.json
- assets/legendarymonuments/models/block/ilex_shrine_lower.json
- assets/legendarymonuments/models/block/ilex_shrine_upper.json
- assets/legendarymonuments/models/block/invisible_block.json
- assets/legendarymonuments/models/block/kyurem_pedestal.json
- assets/legendarymonuments/models/block/latias_pedestal.json
- assets/legendarymonuments/models/block/lugia_lock.json
- assets/legendarymonuments/models/block/lugia_lock_activated.json
- assets/legendarymonuments/models/block/lugia_pedestal.json
- assets/legendarymonuments/models/block/meltan_box.json
- assets/legendarymonuments/models/block/mew_pedestal.json
- assets/legendarymonuments/models/block/origin_block.json
- assets/legendarymonuments/models/block/origin_glass.json
- assets/legendarymonuments/models/block/origin_glass_slab.json
- assets/legendarymonuments/models/block/origin_glass_slab_top.json
- assets/legendarymonuments/models/block/origin_glass_stairs.json
- assets/legendarymonuments/models/block/origin_glass_stairs_inner.json
- assets/legendarymonuments/models/block/origin_glass_stairs_outer.json
- assets/legendarymonuments/models/block/palkia_pedestal.json
- assets/legendarymonuments/models/block/pedestal.json
- assets/legendarymonuments/models/block/pokemon_trial_spawner.json
- assets/legendarymonuments/models/block/pokemon_trial_spawner_ominous.json
- assets/legendarymonuments/models/block/raikou_pedestal.json
- assets/legendarymonuments/models/block/regi_statue_regice.json
- assets/legendarymonuments/models/block/regi_statue_regice_activated.json
- assets/legendarymonuments/models/block/regi_statue_regidrago.json
- assets/legendarymonuments/models/block/regi_statue_regidrago_activated.json
- assets/legendarymonuments/models/block/regi_statue_regieleki.json
- assets/legendarymonuments/models/block/regi_statue_regieleki_activated.json
- assets/legendarymonuments/models/block/regi_statue_regigigas.json
- assets/legendarymonuments/models/block/regi_statue_regigigas_activated.json
- assets/legendarymonuments/models/block/regi_statue_regirock.json
- assets/legendarymonuments/models/block/regi_statue_regirock_activated.json
- assets/legendarymonuments/models/block/regi_statue_registeel.json
- assets/legendarymonuments/models/block/regi_statue_registeel_activated.json
- assets/legendarymonuments/models/block/regice_lock.json
- assets/legendarymonuments/models/block/regice_lock_activated.json
- assets/legendarymonuments/models/block/regigigas_lock.json
- assets/legendarymonuments/models/block/regigigas_lock_activated.json
- assets/legendarymonuments/models/block/regirock_lock.json
- assets/legendarymonuments/models/block/regirock_lock_activated.json
- assets/legendarymonuments/models/block/registeel_lock.json
- assets/legendarymonuments/models/block/registeel_lock_activated.json
- assets/legendarymonuments/models/block/reshiram_pedestal.json
- assets/legendarymonuments/models/block/rock_golem_block.json
- assets/legendarymonuments/models/block/sanctuary_block_lower_active.json
- assets/legendarymonuments/models/block/sanctuary_block_lower_inactive.json
- assets/legendarymonuments/models/block/sanctuary_block_upper_active.json
- assets/legendarymonuments/models/block/sanctuary_block_upper_inactive.json
- assets/legendarymonuments/models/block/sandstone_pressure_plate.json
- assets/legendarymonuments/models/block/sandstone_pressure_plate_down.json
- assets/legendarymonuments/models/block/steel_golem_block.json
- assets/legendarymonuments/models/block/suicune_pedestal.json
- assets/legendarymonuments/models/block/suitcase_block.json
- assets/legendarymonuments/models/block/temple_lock.json
- assets/legendarymonuments/models/block/temple_lock_activated.json
- assets/legendarymonuments/models/block/terrakion_footprints.json
- assets/legendarymonuments/models/block/urn_of_embers_block.json
- assets/legendarymonuments/models/block/urn_of_frost_block.json
- assets/legendarymonuments/models/block/urn_of_storms_block.json
- assets/legendarymonuments/models/block/victini_lock.json
- assets/legendarymonuments/models/block/virizion_footprints.json
- assets/legendarymonuments/models/block/zacian_pedestal.json
- assets/legendarymonuments/models/block/zamazenta_pedestal.json
- assets/legendarymonuments/models/block/zekrom_pedestal.json
- assets/legendarymonuments/models/item/ancient_rubble_ore.json
- assets/legendarymonuments/models/item/antimatter_globe.json
- assets/legendarymonuments/models/item/arc_phone.json
- assets/legendarymonuments/models/item/arctic_stone.json
- assets/legendarymonuments/models/item/azelf_fang.json
- assets/legendarymonuments/models/item/azure_flute.json
- assets/legendarymonuments/models/item/blue_feather.json
- assets/legendarymonuments/models/item/celestica_flute.json
- assets/legendarymonuments/models/item/clear_bell.json
- assets/legendarymonuments/models/item/cobalion_footprints.json
- assets/legendarymonuments/models/item/correct_regi_light.json
- assets/legendarymonuments/models/item/cosmic_bag.json
- assets/legendarymonuments/models/item/cosmic_dust_block.json
- assets/legendarymonuments/models/item/curry_of_justice.json
- assets/legendarymonuments/models/item/darkstone.json
- assets/legendarymonuments/models/item/darkstone_shard.json
- assets/legendarymonuments/models/item/deepslate_galar_particle_ore.json
- assets/legendarymonuments/models/item/dialga_pedestal.json
- assets/legendarymonuments/models/item/distortion_button.json
- assets/legendarymonuments/models/item/distortion_cobblestone.json
- ... and 757 more

## assets/legendarymonuments/lang/en_us.json

```json
{
  "item.legendarymonuments.arc_phone": "Arc Phone",
  "screen.legendarymonuments.arc_phone": "Arc Phone",
  "item.legendarymonuments.temple_key": "Temple Key",
  "block.legendarymonuments.temple_lock": "Temple Lock",
  "block.legendarymonuments.temple_lock_activated": "Temple Lock Activated",
  "item.legendarymonuments.lugia_key": "Lugia Key",
  "block.legendarymonuments.lugia_lock": "Lugia Lock",
  "block.legendarymonuments.lugia_lock_activated": "Lugia Lock Activated",
  "block.legendarymonuments.victini_lock": "Victini Lock",
  "item.legendarymonuments.liberty_pass": "Liberty Pass",
  "block.legendarymonuments.regigigas_lock": "Regigigas Lock",
  "block.legendarymonuments.registeel_lock": "Registeel Lock",
  "block.legendarymonuments.elekidrago_lock": "Elekidrago Lock",
  "block.legendarymonuments.regice_lock": "Regice Lock",
  "block.legendarymonuments.regirock_lock": "Regirock Lock",
  "item.legendarymonuments.truthbottle": "Truth Bottle",
  "item.legendarymonuments.idealsbottle": "Ideals Bottle",
  "item.legendarymonuments.lightstone_shard": "Light Stone Shard",
  "item.legendarymonuments.darkstone_shard": "Dark Stone Shard",
  "item.legendarymonuments.lightstone": "Light Stone",
  "item.legendarymonuments.darkstone": "Dark Stone",
  "item.legendarymonuments.azure_flute": "Azure Flute",
  "item.legendarymonuments.space_globe": "Space Globe",
  "item.legendarymonuments.time_globe": "Time Globe",
  "item.legendarymonuments.antimatter_globe": "Antimatter Globe",
  "item.legendarymonuments.cosmic_bag": "Cosmic Catalyst",
  "item.legendarymonuments.proof_of_conquest_m": "Proof of Conquest (M)",
  "item.legendarymonuments.proof_of_conquest_a": "Proof of Conquest (A)",
  "item.legendarymonuments.proof_of_conquest_u": "Proof of Conquest (U)",
  "item.legendarymonuments.curry_of_justice": "Curry of Justice",
  "item.legendarymonuments.special_spices": "Special Spices",
  "item.legendarymonuments.special_leafy_greens": "Special Leafy Greens",
  "item.legendarymonuments.special_meat_chunks": "Special Meat Chunks",
  "item.legendarymonuments.dyna_apple": "Dyna Apple",
  "item.legendarymonuments.urn_of_embers": "Urn of Embers",
  "item.legendarymonuments.urn_of_storms": "Urn of Storms",
  "item.legendarymonuments.urn_of_frost": "Urn of Frost",
  "item.legendarymonuments.galarian_urn_of_embers": "Galarian Urn of Embers",
  "item.legendarymonuments.galarian_urn_of_storms": "Galarian Urn of Storms",
  "item.legendarymonuments.galarian_urn_of_frost": "Galarian Urn of Frost",
  "item.legendarymonuments.galar_particle": "Galar Particle",
  "item.legendarymonuments.herosword": "Hero Sword",
  "item.legendarymonuments.heroshield": "Hero Shield",
  "item.legendarymonuments.regice_tablet": "Regice Tablet",
  "item.legendarymonuments.regirock_tablet": "Regirock Tablet",
  "item.legendarymonuments.registeel_tablet": "Registeel Tablet",
  "item.legendarymonuments.regieleki_tablet": "Regieleki Tablet",
  "item.legendarymonuments.regidrago_tablet": "Regidrago Tablet",
  "item.legendarymonuments.steel_pauldron": "Steel Pauldron",
  "item.legendarymonuments.electric_pauldron": "Electric Pauldron",
  "item.legendarymonuments.ice_pauldron": "Ice Pauldron",
  "item.legendarymonuments.rock_pauldron": "Rock Pauldron",
  "item.legendarymonuments.dragon_pauldron": "Dragon Pauldron",
  "item.legendarymonuments.titan_pauldron": "Titan Pauldron",
  "item.legendarymonuments.titan_hammer": "Titan Hammer",
  "item.legendarymonuments.golem_scrap": "Golem Scrap",
  "item.legendarymonuments.electric_golem_ingot": "Electric Golem Ingot",
  "item.legendarymonuments.dragon_golem_ingot": "Dragon Golem Ingot",
  "item.legendarymonuments.rock_golem_ingot": "Rock Golem Ingot",
  "item.legendarymonuments.ice_golem_ingot": "Ice Golem Ingot",
  "item.legendarymonuments.steel_golem_ingot": "Steel Golem Ingot",
  "item.legendarymonuments.electric_golem_key": "Electric Golem Key",
  "item.legendarymonuments.dragon_golem_key": "Dragon Golem Key",
  "item.legendarymonuments.rock_golem_key": "Rock Golem Key",
  "item.legendarymonuments.ice_golem_key": "Ice Golem Key",
  "item.legendarymonuments.steel_golem_key": "Steel Golem Key",
  "item.legendarymonuments.titan_key": "Titan Key", 
  "item.legendarymonuments.magma_stone": "Magma Stone",
  "item.legendarymonuments.molten_stone": "Molten Stone",
  "item.legendarymonuments.arctic_stone": "Arctic Stone",
  "item.legendarymonuments.zap_stone": "Zap Stone",
  "item.legendarymonuments.vortex_stone": "Vortex Stone",
  "item.legendarymonuments.mesprit_plume": "Mesprit's Plume",
  "item.legendarymonuments.azelf_fang": "Azelf's Fang",
  "item.legendarymonuments.uxie_claw": "Uxie's Claw",
  "item.legendarymonuments.old_sea_map": "Old Sea Map",
  "item.legendarymonuments.tuft_of_mew_hair": "Tuft of Mew Hair",
  "item.legendarymonuments.griseous_key": "Griseous Key",
  "item.legendarymonuments.ominous_griseous_key": "Ominous Griseous Key",
  "item.legendarymonuments.icerend_seal": "Icerend Seal",
  "item.legendarymonuments.groundblight_seal": "Groundblight Seal",
  "item.legendarymonuments.firescourge_seal": "Firescourge Seal",
  "item.legendarymonuments.grasswither_seal": "Grasswither Seal",
  "item.legendarymonuments.entei_treat": "Entei Treat",
  "item.legendarymonuments.raikou_treat": "Raikou Treat",
  "item.legendarymonuments.suicune_treat": "Suicune Treat",
  "item.legendarymonuments.latios_treat": "Latios Treat",
  "item.legendarymonuments.latias_treat": "Latias Treat",
  "item.legendarymonuments.poketreat_box": "Poketreat Box",
  "item.legendarymonuments.dream_string": "Dream String",
  "item.legendarymonuments.gs_ball": "GS Ball",
  "item.legendarymonuments.dream_string.drop": "You found a Dream String in the darkness!",
  "item.legendarymonuments.red_feather": "Red Feather",
  "item.legendarymonuments.blue_feather": "Blue Feather",
  "item.legendarymonuments.yellow_feather": "Yellow Feather",
  "item.legendarymonuments.celestica_flute": "Celestica Flute",
  "item.legendarymonuments.titan_core": "Titan Core",
  "item.legendarymonuments.rainbow_feather": "Rainbow Feather",
  "item.legendarymonuments.lunar_feather": "Lunar Feather",
  "item.legendarymonuments.nightmare_essence": "Nightmare Essence",
  "item.legendarymonuments.fullmoon_whistle": "Fulmoon Whistle",
  "item.legendarymonuments.newmoon_whistle": "Newmoon Whistle",
  "item.legendarymonuments.distortion_portal": "Distortion Portal",
  "item.legendarymonuments.silver_wing": "Silver Wing",
  "item.legendarymonuments.raw_origin": "Raw Origin",
  "item.legendarymonuments.origin_ingot": "Origin Ingot",
  "item.legendarymonuments.red_chain": "Red Chain",
  "item.legendarymonuments.fragmented_red_chain": "Fragmented Red Chain",
  "block.legendarymonuments.origin_glass": "Origin Glass",
  "block.legendarymonuments.origin_glass_stairs": "Origin Glass Stairs",
  "block.legendarymonuments.origin_glass_slab": "Origin Glass Slab",
  "block.legendarymonuments.sandstone_pressure_plate": "Sandstone Pressure Plate",
  "block.legendarymonuments.distortion_deepslate": "Distortion Deepslate",
  "block.legendarymonuments.distortion_iron_ore": "Distortion Iron Ore",
  "block.legendarymonuments.distortion_deepslate_iron_ore": "Distortion Deepslate Iron Ore",
  "block.legendarymonuments.distortion_origin_ore": "Distortion Origin Ore",
  "block.legendarymonuments.distortion_crystal": "Distortion Crystal",
  "block.legendarymonuments.distortion_crystal_block": "Distortion Crystal Block",
  "block.legendarymonuments.origin_block": "Origin Block",
  "block.legendarymonuments.electric_golem_block": "Electric Golem Block",
  "block.legendarymonuments.dragon_golem_block": "Dragon Golem Block",
  "block.legendarymonuments.rock_golem_block": "Rock Golem Block",
  "block.legendarymonuments.ice_golem_block": "Ice Golem Block",
  "block.legendarymonuments.steel_golem_block": "Steel Golem Block",
  "block.legendarymonuments.suitcase_block": "Suitcase Workstation",
  "block.legendarymonuments.sanctuary_block": "Sanctuary Block",
  "block.legendarymonuments.meltan_box": "Meltan Box",
  "block.legendarymonuments.ancient_rubble_ore": "Ancient Rubble",
  "block.legendarymonuments.galarian_torch": "Galarian Torch",
  "block.legendarymonuments.galar_particle_ore": "Galar Particle Ore",
  "block.legendarymonuments.deepslate_galar_particle_ore": "Deepslate Galar Particle Ore",
  "block.legendarymonuments.galar_particle_block": "Galar Particle Block",
  "block.legendarymonuments.galarian_urn_of_frost_block": "Galarian Urn of Frost",
  "block.legendarymonuments.galarian_urn_of_embers_block": "Galarian Urn of Embers",
  "block.legendarymonuments.galarian_urn_of_storms_block": "Galarian Urn of Storms",
  "block.legendarymonuments.urn_of_frost_block": "Urn of Frost",
  "block.legendarymonuments.urn_of_storms_block": "Urn of Storms",
  "block.legendarymonuments.urn_of_embers_block": "Urn of Embers",
  "block.legendarymonuments.pokemon_trial_spawner": "Pokémon Trial Spawner",
  "block.legendarymonuments.regi_statue": "Regi Statue",
  "block.legendarymonuments.correct_regi_light": "Correct Regi Light",
  "block.legendarymonuments.false_regi_light": "False Regi Light",
  "block.legendarymonuments.dream_catcher": "Dream Catcher",
  "block.legendarymonuments.distortion_cobblestone": "Distortion Cobblestone",
  "block.legendarymonuments.distortion_stone": "Distortion Stone",
  "block.legendarymonuments.distortion_log": "Distortion Log",
  "block.legendarymonuments.distortion_leaves": "Distortion Leaves",
  "block.legendarymonuments.distortion_planks": "Distortion Planks",
  "block.legendarymonuments.distortion_stairs": "Distortion Stairs",
  "block.legendarymonuments.distortion_slab": "Distortion Slab",
  "block.legendarymonuments.distortion_fence": "Distortion Fence",
  "block.legendarymonuments.distortion_fence_gate": "Distortion Fence Gate",
  "block.legendarymonuments.distortion_button": "Distortion Button",
  "block.legendarymonuments.distortion_pressure_plate": "Distortion Pressure Plate",
  "block.legendarymonuments.distortion_sign": "Distortion Sign",
  "block.legendarymonuments.distortion_hanging_sign": "Distortion Hanging Sign",
  "block.legendarymonuments.distortion_door": "Distortion Door",
  "block.legendarymonuments.distortion_trapdoor": "Distortion Trapdoor",
  "block.legendarymonuments.distortion_sapling": "Distortion Tree Sapling",
  "block.legendarymonuments.distortion_cobblestone_bricks": "Distortion Cobblestone Bricks",
  "block.legendarymonuments.distortion_deepslate_bricks": "Distortion Deepslate Bricks",
  "block.legendarymonuments.distortion_cobblestone_bricks_stairs": "Distortion Cobblestone Bricks Stairs",
  "block.legendarymonuments.distortion_cobblestone_bricks_slab": "Distortion Cobblestone Bricks Slab",
  "block.legendarymonuments.distortion_cobblestone_bricks_wall": "Distortion Cobblestone Bricks Wall",
  "block.legendarymonuments.distortion_deepslate_bricks_stairs": "Distortion Deepslate Bricks Stairs",
  "block.legendarymonuments.distortion_deepslate_bricks_slab": "Distortion Deepslate Bricks Slab",
  "block.legendarymonuments.distortion_deepslate_bricks_wall": "Distortion Deepslate Bricks Wall",
  "block.legendarymonuments.cosmic_dust_block": "Cosmic Dust",
  "block.legendarymonuments.terrakion_footprints": "Terrakion Footprints",
  "block.legendarymonuments.cobalion_footprints": "Cobalion Footprints",
  "block.legendarymonuments.virizion_footprints": "Virizion Footprints",
  "block.legendarymonuments.pedestal": "Pedestal Block",
  "block.legendarymonuments.giratina_pedestal": "Giratina Pedestal",
  "block.legendarymonuments.palkia_pedestal": "Palkia Pedestal",
  "block.legendarymonuments.latias_pedestal": "Lati Pedestal",
  "block.legendarymonuments.dialga_pedestal": "Dialga Pedestal",
  "block.legendarymonuments.suicune_pedestal": "Suicune Pedestal",
  "block.legendarymonuments.entei_pedestal": "Entei Pedestal",
  "block.legendarymonuments.raikou_pedestal": "Raikou Pedestal",
  "block.legendarymonuments.lugia_pedestal": "Lugia Pedestal",
  "block.legendarymonuments.ho_oh_pedestal": "Ho-oh Pedestal",
  "block.legendarymonuments.hoopa_pedestal": "Hoopa Pedestal",
  "block.legendarymonuments.zacian_pedestal": "Zacian Pedestal",
  "block.legendarymonuments.zamazenta_pedestal": "Zamazenta Pedestal",
  "block.legendarymonuments.kyurem_pedestal": "Kyurem Pedestal",
  "block.legendarymonuments.reshiram_pedestal": "Reshiram Pedestal",
  "block.legendarymonuments.zekrom_pedestal": "Zekrom Pedestal",
  "block.legendarymonuments.mew_pedestal": "Mew Pedestal",
  "block.legendarymonuments.heatran_pedestal": "Heatran Pedestal",
  "block.legendarymonuments.ilex_shrine": "Ilex Shrine",
  "block.legendarymonuments.eternatus_cocoon": "Eternatus Cocoon",
  "block.legendarymonuments.firescourge_shrine": "Firescourge Shrine",
  "block.legendarymonuments.icerend_shrine": "Icerend Shrine",
  "block.legendarymonuments.groundblight_shrine": "Groundblight Shrine",
  "block.legendarymonuments.grasswither_shrine": "Grasswither Shrine",
  "block.legendarymonuments.firescourge_stake": "Firescourge Stake",
  "block.legendarymonuments.icerend_stake": "Icerend Stake",
  "block.legendarymonuments.groundblight_stake": "Groundblight Stake",
  "block.legendarymonuments.grasswither_stake": "Grasswither Stake",
  "message.legendarymonuments.footprint_found": "%d/%d %s footprints found",
  "message.legendarymonuments.collection_complete": "You have collected all %s footprints!",
  "itemGroup.legendarymonuments.legendary_monuments_group": "Legendary Monuments",
  "item.legendarymonuments.urn.progress": "Progress: %d/%d",
  "item.legendarymonuments.urn.update": "Your urn absorbs energy from %s! Progress: %d/%d",
  "item.legendarymonuments.urn.complete": "Your urn is now fully charged! Right-click again to summon a legendary Pokémon!",
  "item.legendarymonuments.urn.spawned": "The power of the urn summons %s!",
  "item.legendarymonuments.urn.spawned.shiny": "The power of the urn summons a SHINY %s!",
  "item.legendarymonuments.urn.tooltip.type": "Type: %s",
  "item.legendarymonuments.urn.tooltip.progress": "Progress: %d/%d",
  "item.legendarymonuments.urn.tooltip.ready": "Ready to summon! Right-click to release the power",
  "item.legendarymonuments.urn.tooltip.instruction": "Defeat Pokémon of the matching type to charge",
  "item.legendarymonuments.urn.stone.received": "You received a %s",
  "item.legendarymonuments.curry_of_justice.tooltip.1": "A delicious curry that lures Keldeo.",
  "item.legendarymonuments.curry_of_justice.tooltip.2": "Right-click to lure Keldeo",
  "item.legendarymonuments.proof_of_conquest_m.tooltip.1": "Right-click to summon Mesprit",
  "item.legendarymonuments.proof_of_conquest_a.tooltip.1": "Right-click to summon Azelf",
  "item.legendarymonuments.proof_of_conquest_u.tooltip.1": "Right-click to summon Uxie",
  "item.legendarymonuments.fullmoon_whistle.tooltip.1": "A whistle that produces a sound you remember from your dreams",
  "item.legendarymonuments.fullmoon_whistle.tooltip.2": "Right-click to summon Cresselia",
  "item.legendarymonuments.newmoon_whistle.tooltip.1": "A whistle that produces a sound you remember from your nightmares",
  "item.legendarymonuments.newmoon_whistle.tooltip.2": "Right-click to summon Darkrai",
  "item.legendarymonuments.titan_hammer.tooltip": "A mighty hammer that shakes the earth",
  "trinkets.slot.chest.pauldron": "Pauldron",
  "item.legendarymonuments.silver_wing.tooltip.1": "Increases Lugia's Special Attack and Special Defense by 50% when held.",
  "item.legendarymonuments.vortex_stone.tooltip.1": "A stone infused with the torrential power of Articuno, Zapdos, and Moltres. Whoever possesses this stone has earned the right to challenge Lugia.",
  "item.legendarymonuments.cosmic_bag.tooltip.1": "A catalyst that has the power of the cosmos. It can collect and store cosmic dust",
  "tooltip.legendarymonuments.lightstone": "A strange stone that radiates a blazing aura. Used alongside a fire gem to resurrect Reshiram",
  "tooltip.legendarymonuments.darkstone": "A strange stone that radiates a bursting aura. Used alongside an electric gem to resurrect Zekrom",
  "tooltip.legendarymonuments.lightstone_shard": "Fragments of the Light stone, it seems to hold immense power",
  "tooltip.legendarymonuments.darkstone_shard": "Fragments of the Dark stone, it seems to hold immense power",
  "tooltip.legendarymonuments.truth_bottle": "Truth given physical form, used alongside the ideals bottle to summon Kyurem",
  "tooltip.legendarymonuments.ideals_bottle": "ideals given physical form, used alongside the truth bottle to summon Kyurem",
  "tooltip.legendarymonuments.regi_tablet": "Can be used to craft the Titan Key",
  "tooltip.legendarymonuments.terraberry": "A berry found in Terrakion's presence, can be used to create the keldo dango",
  "tooltip.legendarymonuments.viriberry": "A berry found in Virizion's presence, can be used to create the keldeo dango",
  "tooltip.legendarymonuments.cobalberry": "A berry found in Cobalion's presence, can be used to create the keldeo dango",
  "tooltip.legendarymonuments.dyna_apple": "An apple native to the Galar Region, that can be used to craft galarian urns",
  "tooltip.legendarymonuments.magma_stone": "A stone used to summon Heatran at his cave in the Nether",
  "tooltip.legendarymonuments.molten_stone": "A stone infused with the power of Moltres",
  "tooltip.legendarymonuments.arctic_stone": "A stone infused with the power of Articuno",
  "tooltip.legendarymonuments.zap_stone": "A stone infused with the power of Zapdos",
  "tooltip.legendarymonuments.old_sea_map": "Used to summon Mew in the Final Island",
  "tooltip.legendarymonuments.tuft_of_mew_hair": "Extremely small hair follicles from Mew. Contains the DNA of every single pokemon.",
  "tooltip.legendarymonuments.special_meat_chunks": "Cobalion's Favorite Snack. A great curry ingredient.",
  "tooltip.legendarymonuments.special_spices": "Terrakion's Favorite Snack. A great curry ingredient.",
  "tooltip.legendarymonuments.special_leafy_greens": "Virizion's Favorite Snack. A great curry ingredient.",
  "tooltip.legendarymonuments.firescourge_seal": "Used to locate the Firescourge Shrine",
  "tooltip.legendarymonuments.icerend_seal": "Used to find the Icerend Shrine",
  "tooltip.legendarymonuments.groundblight_seal": "Used to find the Groundblight Shrine",
  "tooltip.legendarymonuments.grasswither_seal": "Used to find the Grasswither Shrine",
  "tooltip.legendarymonuments.celestica_flute": "A magical flute used to craft the Azure Flute",
  "tooltip.legendarymonuments.azure_flute": "A magical flute used to enter the Hall of Origin",
  "tooltip.legendarymonuments.space_globe": "A globe that distorts the fabric of space. It hums with the vast energy of Palkia’s domain.",
  "tooltip.legendarymonuments.time_globe": "A sphere pulsating with the rhythm of time itself. Fragments of Dialga’s eternal power swirl within.",
  "tooltip.legendarymonuments.antimatter_globe": "A dark orb radiating void energy. Giratina’s essence twists within, defying reality.",
  "tooltip.legendarymonuments.steel_golem_key": "Unlocks the Registeel Room at Snowpoint Temple",
  "tooltip.legendarymonuments.rock_golem_key": "Unlocks the Regirock Room at Snowpoint Temple",
  "tooltip.legendarymonuments.ice_golem_key": "Unlocks the Regice Room at Snowpoint Temple",
  "tooltip.legendarymonuments.electric_golem_key": "Unlocks the Regieleki/Regidrago Room at Snowpoint Temple",
  "tooltip.legendarymonuments.dragon_golem_key": "Unlocks the Regieleki/Regidrago Room at Snowpoint Temple",
  "tooltip.legendarymonuments.titan_key": "Unlocks the Regigigas Room at Snowpoint Temple",
  "tooltip.legendarymonuments.mesprit_plume": "A delicate feather radiating warmth and sorrow. It carries Mesprit’s power over emotion.",
  "tooltip.legendarymonuments.uxie_claw": "A claw from the Being of Knowledge. Touching it floods the mind with ancient wisdom.",
  "tooltip.legendarymonuments.azelf_fang": "A sharp relic imbued with unwavering resolve. Azelf’

... truncated ...
```
