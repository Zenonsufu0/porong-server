# Cobblemon-fabric-1.7.3+1.21.1.jar

## fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "cobblemon",
  "version": "1.7.3+1.21.1",
  "name": "Cobblemon",
  "description": "Adds Pokémon to the world, letting you find, battle, capture, and evolve them.",
  "authors": [
    "The Cobblemon Team"
  ],
  "contact": {
    "homepage": "https://cobblemon.com/",
    "sources": "https://gitlab.com/cable-mc/cobblemon",
    "issues": "https://gitlab.com/cable-mc/cobblemon/-/issues"
  },
  "license": "MPL-2.0",
  "icon": "assets/cobblemon/icon_cobblemon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.cobblemon.mod.fabric.FabricBootstrap"
    ],
    "client": [
      "com.cobblemon.mod.fabric.client.CobblemonFabricClient"
    ],
    "jei_mod_plugin": [
      "com.cobblemon.mod.common.integration.jei.CobblemonJeiPlugin"
    ],
    "dynamiclights": [
      "com.cobblemon.mod.common.compat.lambdynamiclights.LambDynamicLightsInitializer"
    ],
    "modmenu": [
      "com.cobblemon.mod.fabric.client.integration.modmenu.CobblemonModMenu"
    ]
  },
  "mixins": [
    "mixins.cobblemon-common.json",
    "mixins.cobblemon-fabric.json"
  ],
  "accessWidener": "cobblemon-common.accesswidener",
  "depends": {
    "fabricloader": ">=0.17.2",
    "fabric-api": ">=0.116.6+1.21.1",
    "minecraft": "1.21.1",
    "java": "21"
  },
  "custom": {
    "modmenu": {
      "links": {
        "modmenu.curseforge": "https://www.curseforge.com/minecraft/mc-mods/cobblemon",
        "modmenu.modrinth": "https://modrinth.com/mod/cobblemon",
        "modmenu.discord": "https://discord.gg/cobblemon"
      }
    },
    "loom:injected_interfaces": {
      "net/minecraft/class_630": [
        "com/cobblemon/mod/common/client/render/models/blockbench/pose/Bone"
      ]
    }
  },
  "jars": [
    {
      "file": "META-INF/jars/fabric-language-kotlin-1.13.6+kotlin.2.2.20.jar"
    }
  ]
}
```

## Namespaces

- assets: ['cobblemon', 'minecraft']

- data: ['adorn', 'botanypots', 'c', 'carryon', 'cobblemon', 'minecraft']

## Lang files

- assets/cobblemon/lang/cs_cz.json
- assets/cobblemon/lang/de_de.json
- assets/cobblemon/lang/el_cy.json
- assets/cobblemon/lang/en_gb.json
- assets/cobblemon/lang/en_pt.json
- assets/cobblemon/lang/en_us.json
- assets/cobblemon/lang/eo_eo.json
- assets/cobblemon/lang/es_es.json
- assets/cobblemon/lang/es_mx.json
- assets/cobblemon/lang/fr_ca.json
- assets/cobblemon/lang/fr_fr.json
- assets/cobblemon/lang/hu_hu.json
- assets/cobblemon/lang/it_it.json
- assets/cobblemon/lang/ja_jp.json
- assets/cobblemon/lang/ko_kr.json
- assets/cobblemon/lang/nl_nl.json
- assets/cobblemon/lang/pl_pl.json
- assets/cobblemon/lang/pt_br.json
- assets/cobblemon/lang/pt_pt.json
- assets/cobblemon/lang/ru_ru.json
- assets/cobblemon/lang/sv_se.json
- assets/cobblemon/lang/th_th.json
- assets/cobblemon/lang/tr_tr.json
- assets/cobblemon/lang/uk_ua.json
- assets/cobblemon/lang/vi_vn.json
- assets/cobblemon/lang/zh_cn.json
- assets/cobblemon/lang/zh_hk.json
- assets/cobblemon/lang/zh_tw.json
- resourcepacks/adorncompatibility/assets/adorn/lang/en_us.json

## Interesting JSON/resources

- assets/cobblemon/bedrock/berries/eggant_berry.geo.json
- assets/cobblemon/bedrock/berries/pamtre_berry.geo.json
- assets/cobblemon/bedrock/berries/sitrus_berry.geo.json
- assets/cobblemon/bedrock/berries/watmel_berry.geo.json
- assets/cobblemon/bedrock/generic/animations/moves/stringshot.animation.json
- assets/cobblemon/bedrock/npcs/animations/trainer_generic.animation.json
- assets/cobblemon/bedrock/npcs/models/trainer.geo.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/ballsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/battle/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/battle/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/battle/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/casual/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/casual/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientcitrineball/casual/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/ballsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/battle/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/battle/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/battle/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/casual/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/casual/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientroseateball/casual/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/ballsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/battle/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/battle/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/battle/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/casual/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/casual/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ancientultraball/casual/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/capture/hisui/hisuitrail.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/ballsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/battle/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/battle/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/battle/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/casual/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/casual/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/citrineball/casual/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/ballsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/battle/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/battle/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/battle/sendflash.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/casual/ballsendsparkle.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/casual/ballsparks.particle.json
- assets/cobblemon/bedrock/particles/balls/ultraball/casual/sendflash.particle.json
- assets/cobblemon/bedrock/particles/generic/impact_electric.particle.json
- assets/cobblemon/bedrock/particles/moves/absorb/absorb_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/acidspray/acidspray_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/amnesia/amnesia_actortraillarge.particle.json
- assets/cobblemon/bedrock/particles/moves/amnesia/amnesia_actortraillargekill.particle.json
- assets/cobblemon/bedrock/particles/moves/amnesia/amnesia_actortrailsmall.particle.json
- assets/cobblemon/bedrock/particles/moves/amnesia/amnesia_actortrailsmallkill.particle.json
- assets/cobblemon/bedrock/particles/moves/bulldoze/bulldoze_targetrumble.particle.json
- assets/cobblemon/bedrock/particles/moves/crunch/crunch_targetrocks.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_actoregg.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_actoreggmiss.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_targetboom.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_targetfire.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_targetpoof.particle.json
- assets/cobblemon/bedrock/particles/moves/eggbomb/eggbomb_targetshards.particle.json
- assets/cobblemon/bedrock/particles/moves/eruption/eruption_targetrocks.particle.json
- assets/cobblemon/bedrock/particles/moves/frustration/frustration_target_1.particle.json
- assets/cobblemon/bedrock/particles/moves/frustration/frustration_target_2.particle.json
- assets/cobblemon/bedrock/particles/moves/gigadrain/gigadrain_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/icepunch/icepunch_targetmist.particle.json
- assets/cobblemon/bedrock/particles/moves/iceshard/iceshard_targetmist.particle.json
- assets/cobblemon/bedrock/particles/moves/leechlife/leechlife_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actor.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actoraccent.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actordrainaccent.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actorheal.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actorhealbig.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actorhealtimer.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actorhit.particle.json
- assets/cobblemon/bedrock/particles/moves/megadrain/megadrain_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/mudbomb/mudbomb_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/nastyplot/nastyplot_actortraillarge.particle.json
- assets/cobblemon/bedrock/particles/moves/nastyplot/nastyplot_actortraillargekill.particle.json
- assets/cobblemon/bedrock/particles/moves/nastyplot/nastyplot_actortrailsmall.particle.json
- assets/cobblemon/bedrock/particles/moves/nastyplot/nastyplot_actortrailsmallkill.particle.json
- assets/cobblemon/bedrock/particles/moves/sandattack/sandattack_impactresidual.particle.json
- assets/cobblemon/bedrock/particles/moves/seismictoss/seismictoss_targetrocks.particle.json
- assets/cobblemon/bedrock/particles/moves/sludge/sludge_targettrail.particle.json
- assets/cobblemon/bedrock/particles/moves/sludgebomb/sludgebomb_actortrail.particle.json
- assets/cobblemon/bedrock/particles/moves/stringshot/stringshot_actor.particle.json
- assets/cobblemon/bedrock/particles/moves/stringshot/stringshot_actorspray.particle.json
- assets/cobblemon/bedrock/particles/moves/stringshot/stringshot_target1.particle.json
- assets/cobblemon/bedrock/particles/moves/stringshot/stringshot_target2.particle.json
- assets/cobblemon/bedrock/particles/moves/watersport/watersport_targetrain.particle.json
- assets/cobblemon/bedrock/particles/pokemon/annihilape/annihilapeglare.particle.json
- assets/cobblemon/bedrock/particles/pokemon/blastoise/blastoise6.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_black.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_blue.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_brown.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_cyan.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_gray.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_green.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_light_blue.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_light_gray.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_lime.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_magenta.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_orange.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_particles.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_pink.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_purple.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_red.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_white.particle.json
- assets/cobblemon/bedrock/particles/pokemon/furfrou/poodle_hair_yellow.particle.json
- assets/cobblemon/bedrock/particles/pokemon/gastly/ghostly_smoke.particle.json
- assets/cobblemon/bedrock/particles/pokemon/gastly/ghostly_smoke_center.particle.json
- assets/cobblemon/bedrock/particles/pokemon/gastly/shiny_ghostly_smoke.particle.json
- assets/cobblemon/bedrock/particles/pokemon/gastly/shiny_ghostly_smoke_center.particle.json
- assets/cobblemon/bedrock/particles/pokemon/hooh/hooh_tailtrail.particle.json
- assets/cobblemon/bedrock/particles/pokemon/krabby/krabby_big_bubbles.particle.json
- assets/cobblemon/bedrock/particles/pokemon/krabby/krabby_bubble_pop.particle.json
- assets/cobblemon/bedrock/particles/pokemon/krabby/krabby_bubbles.particle.json
- assets/cobblemon/bedrock/particles/pokemon/krabby/krabby_particles_handler.particle.json
- assets/cobblemon/bedrock/particles/pokemon/lucario/lucario_aura.particle.json
- assets/cobblemon/bedrock/particles/pokemon/lucario/lucario_aura_sparks.particle.json
- assets/cobblemon/bedrock/particles/pokemon/torkoal/torkoal_particle_handler.particle.json
- assets/cobblemon/bedrock/particles/pokemon/torkoal/torkoal_smoke_bottom.particle.json
- assets/cobblemon/bedrock/particles/pokemon/torkoal/torkoal_smoke_top.particle.json
- assets/cobblemon/bedrock/poke_balls/variations/0_ancient_citrine_ball_base.json
- assets/cobblemon/bedrock/poke_balls/variations/0_ancient_ultra_ball_base.json
- assets/cobblemon/bedrock/poke_balls/variations/0_citrine_ball_base.json
- assets/cobblemon/bedrock/poke_balls/variations/0_strange_ball_base.json
- assets/cobblemon/bedrock/poke_balls/variations/0_ultra_ball_base.json
- assets/cobblemon/bedrock/pokemon/animations/0001_bulbasaur/bulbasaur.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0002_ivysaur/ivysaur.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0003_venusaur/venusaur.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0004_charmander/charmander.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0005_charmeleon/charmeleon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0006_charizard/charizard.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0007_squirtle/squirtle.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0008_wartortle/wartortle.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0009_blastoise/blastoise.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0010_caterpie/caterpie.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0011_metapod/metapod.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0012_butterfree/butterfree.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0013_weedle/weedle.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0014_kakuna/kakuna.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0015_beedrill/beedrill.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0016_pidgey/pidgey.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0017_pidgeotto/pidgeotto.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0018_pidgeot/pidgeot.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0019_rattata/rattata.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0019_rattata/rattata_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0020_raticate/raticate.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0020_raticate/raticate_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0021_spearow/spearow.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0022_fearow/fearow.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0023_ekans/ekans.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0024_arbok/arbok.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0025_pikachu/pikachu.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0026_raichu/raichu.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0026_raichu/raichu_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0027_sandshrew/sandshrew.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0028_sandslash/sandslash.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0029_nidoranf/nidoranf.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0030_nidorina/nidorina.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0031_nidoqueen/nidoqueen.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0032_nidoranm/nidoranm.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0033_nidorino/nidorino.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0034_nidoking/nidoking.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0035_clefairy/clefairy.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0036_clefable/clefable.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0037_vulpix/vulpix.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0037_vulpix/vulpix_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0038_ninetales/ninetales.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0038_ninetales/ninetales_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0039_jigglypuff/jigglypuff.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0040_wigglytuff/wigglytuff.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0041_zubat/zubat.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0042_golbat/golbat.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0043_oddish/oddish.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0044_gloom/gloom.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0045_vileplume/vileplume.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0046_paras/paras.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0047_parasect/parasect.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0048_venonat/venonat.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0049_venomoth/venomoth.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0050_diglett/diglett.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0050_diglett/diglett_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0051_dugtrio/dugtrio.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0051_dugtrio/dugtrio_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0052_meowth/meowth.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0052_meowth/meowth_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0052_meowth/meowth_galarian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0053_persian/persian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0053_persian/persian_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0054_psyduck/psyduck.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0055_golduck/golduck.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0056_mankey/mankey.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0057_primeape/primeape.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0058_growlithe/growlithe.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0059_arcanine/arcanine.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0060_poliwag/poliwag.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0061_poliwhirl/poliwhirl.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0062_poliwrath/poliwrath.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0063_abra/abra.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0064_kadabra/kadabra_female.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0064_kadabra/kadabra_male.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0065_alakazam/alakazam_female.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0065_alakazam/alakazam_male.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0066_machop/machop.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0067_machoke/machoke.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0068_machamp/machamp.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0069_bellsprout/bellsprout.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0070_weepinbell/weepinbell.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0071_victreebel/victreebel.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0072_tentacool/tentacool.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0073_tentacruel/tentacruel.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0074_geodude/geodude.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0074_geodude/geodude_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0075_graveler/graveler.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0075_graveler/graveler_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0076_golem/golem.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0076_golem/golem_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0077_ponyta/ponyta.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0077_ponyta/ponyta_galar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0078_rapidash/rapidash.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0078_rapidash/rapidash_galar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0079_slowpoke/slowpoke.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0079_slowpoke/slowpoke_galarian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0080_slowbro/slowbro.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0080_slowbro/slowbro_galarian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0081_magnemite/magnemite.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0082_magneton/magneton.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0083_farfetchd/farfetchd.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0083_farfetchd/farfetchd_galar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0084_doduo/doduo.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0085_dodrio/dodrio.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0086_seel/seel.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0087_dewgong/dewgong.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0088_grimer/grimer.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0088_grimer/grimer_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0089_muk/muk.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0089_muk/muk_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0090_shellder/shellder.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0091_cloyster/cloyster.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0092_gastly/gastly.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0092_gastly/gastly_shiny.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0093_haunter/haunter.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0094_gengar/gengar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0095_onix/onix.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0096_drowzee/drowzee.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0097_hypno/hypno.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0098_krabby/krabby.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0099_kingler/kingler.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0100_voltorb/voltorb.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0100_voltorb/voltorb_hisuian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0101_electrode/electrode.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0101_electrode/electrode_hisuian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0102_exeggcute/exeggcute.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0103_exeggutor/exeggutor.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0103_exeggutor/exeggutor_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0104_cubone/cubone.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0105_marowak/marowak.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0105_marowak/marowak_alolan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0106_hitmonlee/hitmonlee.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0107_hitmonchan/hitmonchan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0108_lickitung/lickitung.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0109_koffing/koffing.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0110_weezing/weezing.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0110_weezing/weezing_galar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0111_rhyhorn/rhyhorn.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0112_rhydon/rhydon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0113_chansey/chansey.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0114_tangela/tangela.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0115_kangaskhan/kangaskhan.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0116_horsea/horsea.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0117_seadra/seadra.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0118_goldeen/goldeen.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0119_seaking/seaking.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0120_staryu/staryu.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0121_starmie/starmie.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0122_mrmime/mr_mime.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0122_mrmime/mr_mime_galarian.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0123_scyther/scyther.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0124_jynx/jynx.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0125_electabuzz/electabuzz.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0126_magmar/magmar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0127_pinsir/pinsir.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0128_tauros/tauros.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0128_tauros/tauros_paldean_aqua.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0128_tauros/tauros_paldean_blaze.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0128_tauros/tauros_paldean_combat.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0129_magikarp/magikarp.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0130_gyarados/gyarados.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0131_lapras/lapras.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0132_ditto/ditto.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0133_eevee/eevee.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0134_vaporeon/vaporeon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0135_jolteon/jolteon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0136_flareon/flareon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0137_porygon/porygon.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0138_omanyte/omanyte.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0139_omastar/omastar.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0140_kabuto/kabuto.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0141_kabutops/kabutops.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0142_aerodactyl/aerodactyl.animation.json
- assets/cobblemon/bedrock/pokemon/animations/0143_snorlax/snorlax.animation.json
- ... and 10246 more

## assets/cobblemon/lang/en_us.json

```json
{"item.cobblemon.poke_ball":"Poké Ball","item.cobblemon.slate_ball":"Slate Ball","item.cobblemon.azure_ball":"Azure Ball","item.cobblemon.verdant_ball":"Verdant Ball","item.cobblemon.roseate_ball":"Roseate Ball","item.cobblemon.citrine_ball":"Citrine Ball","item.cobblemon.great_ball":"Great Ball","item.cobblemon.ultra_ball":"Ultra Ball","item.cobblemon.master_ball":"Master Ball","item.cobblemon.safari_ball":"Safari Ball","item.cobblemon.fast_ball":"Fast Ball","item.cobblemon.level_ball":"Level Ball","item.cobblemon.lure_ball":"Lure Ball","item.cobblemon.heavy_ball":"Heavy Ball","item.cobblemon.love_ball":"Love Ball","item.cobblemon.friend_ball":"Friend Ball","item.cobblemon.moon_ball":"Moon Ball","item.cobblemon.sport_ball":"Sport Ball","item.cobblemon.net_ball":"Net Ball","item.cobblemon.dive_ball":"Dive Ball","item.cobblemon.nest_ball":"Nest Ball","item.cobblemon.repeat_ball":"Repeat Ball","item.cobblemon.timer_ball":"Timer Ball","item.cobblemon.luxury_ball":"Luxury Ball","item.cobblemon.premier_ball":"Premier Ball","item.cobblemon.dusk_ball":"Dusk Ball","item.cobblemon.heal_ball":"Heal Ball","item.cobblemon.quick_ball":"Quick Ball","item.cobblemon.cherish_ball":"Cherish Ball","item.cobblemon.park_ball":"Park Ball","item.cobblemon.dream_ball":"Dream Ball","item.cobblemon.beast_ball":"Beast Ball","item.cobblemon.ancient_poke_ball":"Ancient Poké Ball","item.cobblemon.ancient_citrine_ball":"Ancient Citrine Ball","item.cobblemon.ancient_verdant_ball":"Ancient Verdant Ball","item.cobblemon.ancient_azure_ball":"Ancient Azure Ball","item.cobblemon.ancient_roseate_ball":"Ancient Roseate Ball","item.cobblemon.ancient_slate_ball":"Ancient Slate Ball","item.cobblemon.ancient_ivory_ball":"Ancient Ivory Ball","item.cobblemon.ancient_great_ball":"Ancient Great Ball","item.cobblemon.ancient_ultra_ball":"Ancient Ultra Ball","item.cobblemon.ancient_heavy_ball":"Ancient Heavy Ball","item.cobblemon.ancient_leaden_ball":"Ancient Leaden Ball","item.cobblemon.ancient_gigaton_ball":"Ancient Gigaton Ball","item.cobblemon.ancient_feather_ball":"Ancient Feather Ball","item.cobblemon.ancient_wing_ball":"Ancient Wing Ball","item.cobblemon.ancient_jet_ball":"Ancient Jet Ball","item.cobblemon.ancient_origin_ball":"Ancient Origin Ball","item.cobblemon.pokerod_smithing_template":"Smithing Template","item.cobblemon.smithing_template.pokerod.ingredients":"Poké Balls","item.cobblemon.smithing_template.pokerod.base_slot_description":"Add Fishing Rod","item.cobblemon.smithing_template.pokerod.additions_slot_description":"Add Poké Ball","upgrade.cobblemon.pokerod":"Poké Rod Upgrade","item.cobblemon.poke_rod":"Poké Rod","cobblemon.pokerod.bobber":"Bobber: %1$s","cobblemon.pokerod.bait":"Bait: %1$s ×%2$s","cobblemon.pokerod.apply":"Right-click to apply bait","cobblemon.pokerod.remove":"Right click to remove bait","item.cobblemon.black_apricorn_seed":"Black Apricorn Sprout","item.cobblemon.blue_apricorn_seed":"Blue Apricorn Sprout","item.cobblemon.green_apricorn_seed":"Green Apricorn Sprout","item.cobblemon.pink_apricorn_seed":"Pink Apricorn Sprout","item.cobblemon.red_apricorn_seed":"Red Apricorn Sprout","item.cobblemon.white_apricorn_seed":"White Apricorn Sprout","item.cobblemon.yellow_apricorn_seed":"Yellow Apricorn Sprout","item.cobblemon.black_apricorn":"Black Apricorn","item.cobblemon.blue_apricorn":"Blue Apricorn","item.cobblemon.green_apricorn":"Green Apricorn","item.cobblemon.pink_apricorn":"Pink Apricorn","item.cobblemon.red_apricorn":"Red Apricorn","item.cobblemon.white_apricorn":"White Apricorn","item.cobblemon.yellow_apricorn":"Yellow Apricorn","item.cobblemon.saccharine_sapling":"Saccharine Sapling","block.cobblemon.potted_saccharine_sapling":"Potted Saccharine Sapling","item.cobblemon.vivichoke_seeds":"Vivichoke Seeds","item.cobblemon.vivichoke":"Vivichoke","item.cobblemon.galarica_nuts":"Galarica Nuts","item.cobblemon.hearty_grains":"Hearty Grains","item.cobblemon.bugwort":"Bugwort","item.cobblemon.apricorn_boat":"Apricorn Boat","item.cobblemon.apricorn_chest_boat":"Apricorn Boat with Chest","item.cobblemon.saccharine_boat":"Saccharine Boat","item.cobblemon.saccharine_chest_boat":"Saccharine Boat with Chest","item.cobblemon.pokedex_black":"Pokédex","item.cobblemon.pokedex_blue":"Pokédex","item.cobblemon.pokedex_green":"Pokédex","item.cobblemon.pokedex_pink":"Pokédex","item.cobblemon.pokedex_red":"Pokédex","item.cobblemon.pokedex_white":"Pokédex","item.cobblemon.pokedex_yellow":"Pokédex","item.cobblemon.npc_editor":"NPC's Trainer Card","item.cobblemon.link_cable":"Link Cable","item.cobblemon.link_cable.tooltip":"Evolves pokemon that require trading","item.cobblemon.dragon_scale":"Dragon Scale","item.cobblemon.dragon_scale.tooltip":"Evolves Seadra into Kingdra when held while trading","item.cobblemon.kings_rock":"Kings Rock","item.cobblemon.kings_rock.tooltip_1":"When the holder successfully inflicts damage, the target may also flinch","item.cobblemon.kings_rock.tooltip_2":"Evolves Poliwhirl into Politoed and Slowpoke into Slowking when held during trading","item.cobblemon.metal_coat":"Metal Coat","item.cobblemon.metal_coat.tooltip_1":"Boosts the power of the holder's Steel-type moves","item.cobblemon.metal_coat.tooltip_2":"Evolves Onix into Steelix and Scyther into Scizor when held during trading","item.cobblemon.upgrade":"Upgrade","item.cobblemon.upgrade.tooltip":"Evolves Porygon into Porygon2 when held during trading","item.cobblemon.dubious_disc":"Dubious Disc","item.cobblemon.dubious_disc.tooltip":"Evolves Porygon2 into Porygon-Z when held during trading","item.cobblemon.deep_sea_scale":"Deep Sea Scale","item.cobblemon.deep_sea_scale.tooltip_1":"Boosts Clamperl's Sp. Def","item.cobblemon.deep_sea_scale.tooltip_2":"Evolves Clamperl into Gorebyss when held during trading","item.cobblemon.deep_sea_tooth":"Deep Sea Tooth","item.cobblemon.deep_sea_tooth.tooltip_1":"Boosts Clamperl's Sp. Atk","item.cobblemon.deep_sea_tooth.tooltip_2":"Evolves Clamperl into Huntail when held during trading","item.cobblemon.electirizer":"Electirizer","item.cobblemon.electirizer.tooltip":"Evolves Electabuzz into Electivire when held while trading","item.cobblemon.magmarizer":"Magmarizer","item.cobblemon.magmarizer.tooltip":"Evolves Magmar into Magmortar when held during trading","item.cobblemon.oval_stone":"Oval Stone","item.cobblemon.oval_stone.tooltip":"Evolves Happiny into Chansey when held during the day","item.cobblemon.protector":"Protector","item.cobblemon.protector.tooltip":"Evolves Rhydon into Rhyperior when held while trading","item.cobblemon.reaper_cloth":"Reaper Cloth","item.cobblemon.reaper_cloth.tooltip":"Evolves Dusclops into Dusknoir when held while trading","item.cobblemon.prism_scale":"Prism Scale","item.cobblemon.prism_scale.tooltip":"Evolves Feebas into Milotic when held while trading","item.cobblemon.sachet":"Sachet","item.cobblemon.sachet.tooltip":"Evolves Spritzee into Aromatisse when held while trading","item.cobblemon.whipped_dream":"Whipped Dream","item.cobblemon.whipped_dream.tooltip":"Evolves Swirlix into Slurpuff when held while trading","item.cobblemon.strawberry_sweet":"Strawberry Sweet","item.cobblemon.strawberry_sweet.tooltip":"Evolves Milcery into Alcremie with a Strawberry Sweet","item.cobblemon.love_sweet":"Love Sweet","item.cobblemon.love_sweet.tooltip":"Evolves Milcery into Alcremie with a Love Sweet","item.cobblemon.berry_sweet":"Berry Sweet","item.cobblemon.berry_sweet.tooltip":"Evolves Milcery into Alcremie with a Berry Sweet","item.cobblemon.clover_sweet":"Clover Sweet","item.cobblemon.clover_sweet.tooltip":"Evolves Milcery into Alcremie with a Clover Sweet","item.cobblemon.flower_sweet":"Flower Sweet","item.cobblemon.flower_sweet.tooltip":"Evolves Milcery into Alcremie with a Flower Sweet","item.cobblemon.star_sweet":"Star Sweet","item.cobblemon.star_sweet.tooltip":"Evolves Milcery into Alcremie with a Star Sweet","item.cobblemon.ribbon_sweet":"Ribbon Sweet","item.cobblemon.ribbon_sweet.tooltip":"Evolves Milcery into Alcremie with a Ribbon Sweet","item.cobblemon.chipped_pot":"Chipped Pot","item.cobblemon.chipped_pot.tooltip":"Evolves Antique Sinistea into Polteageist","item.cobblemon.cracked_pot":"Cracked Pot","item.cobblemon.cracked_pot.tooltip":"Evolves Phony Sinistea into Polteageist","item.cobblemon.masterpiece_teacup":"Masterpiece Teacup","item.cobblemon.masterpiece_teacup.tooltip":"Evolves Artisan Poltchageist into Masterpiece Sinistcha","item.cobblemon.unremarkable_teacup":"Unremarkable Teacup","item.cobblemon.unremarkable_teacup.tooltip":"Evolves Counterfeit Poltchageist into Unremarkable Sinistcha","item.cobblemon.sweet_apple":"Sweet Apple","item.cobblemon.sweet_apple.tooltip":"Evolves Applin into Appletun","item.cobblemon.tart_apple":"Tart Apple","item.cobblemon.tart_apple.tooltip":"Evolves Applin into Flapple","item.cobblemon.syrupy_apple":"Syrupy Apple","item.cobblemon.syrupy_apple.tooltip":"Evolves Applin into Dipplin","item.cobblemon.galarica_cuff":"Galarica Cuff","item.cobblemon.galarica_cuff.tooltip":"Evolves Galarian Slowpoke into Galarian Slowbro","item.cobblemon.galarica_wreath":"Galarica Wreath","item.cobblemon.galarica_wreath.tooltip":"Evolves Galarian Slowpoke into Galarian Slowking","item.cobblemon.black_augurite":"Black Augurite","item.cobblemon.black_augurite.tooltip":"Evolves Scyther into Kleavor","item.cobblemon.peat_block":"Peat Block","item.cobblemon.peat_block.tooltip":"Evolves Ursaring into Ursaluna when used during a full moon at night","item.cobblemon.metal_alloy":"Metal Alloy","item.cobblemon.metal_alloy.tooltip":"Evolves Duraludon into Archaludon","item.cobblemon.scroll_of_darkness":"Scroll of Darkness","item.cobblemon.scroll_of_darkness.tooltip":"Evolves Kubfu into Single Strike Style Urshifu","item.cobblemon.scroll_of_waters":"Scroll of Waters","item.cobblemon.scroll_of_waters.tooltip":"Evolves Kubfu into Rapid Strike Style Urshifu","item.cobblemon.dawn_stone":"Dawn Stone","item.cobblemon.dawn_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.dusk_stone":"Dusk Stone","item.cobblemon.dusk_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.fire_stone":"Fire Stone","item.cobblemon.fire_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.ice_stone":"Ice Stone","item.cobblemon.ice_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.leaf_stone":"Leaf Stone","item.cobblemon.leaf_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.moon_stone":"Moon Stone","item.cobblemon.moon_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.shiny_stone":"Shiny Stone","item.cobblemon.shiny_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.sun_stone":"Sun Stone","item.cobblemon.sun_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.thunder_stone":"Thunder Stone","item.cobblemon.thunder_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.water_stone":"Water Stone","item.cobblemon.water_stone.tooltip":"Evolves certain Pokémon","item.cobblemon.rare_candy":"Rare Candy","item.cobblemon.rare_candy.tooltip":"Increases a Pokémon's level by one","item.cobblemon.exp_candy_xs":"Exp. Candy XS","item.cobblemon.exp_candy_xs.tooltip":"Increases a Pokémon's experience by 100 Exp. Points","item.cobblemon.exp_candy_s":"Exp. Candy S","item.cobblemon.exp_candy_s.tooltip":"Increases a Pokémon's experience by 800 Exp. Points","item.cobblemon.exp_candy_m":"Exp. Candy M","item.cobblemon.exp_candy_m.tooltip":"Increases a Pokémon's experience by 3000 Exp. Points","item.cobblemon.exp_candy_l":"Exp. Candy L","item.cobblemon.exp_candy_l.tooltip":"Increases a Pokémon's experience by 10000 Exp. Points","item.cobblemon.exp_candy_xl":"Exp. Candy XL","item.cobblemon.exp_candy_xl.tooltip":"Increases a Pokémon's experience by 30000 Exp. Points","item.cobblemon.calcium":"Calcium","item.cobblemon.calcium.tooltip":"Raises the Pokémon's Sp. Atk EVs by 10","item.cobblemon.carbos":"Carbos","item.cobblemon.carbos.tooltip":"Raises the Pokémon's Speed EVs by 10","item.cobblemon.hp_up":"HP Up","item.cobblemon.hp_up.tooltip":"Raises the Pokémon's HP EVs by 10","item.cobblemon.iron":"Iron","item.cobblemon.iron.tooltip":"Raises the Pokémon's Defense EVs by 10","item.cobblemon.protein":"Protein","item.cobblemon.protein.tooltip":"Raises the Pokémon's Attack EVs by 10","item.cobblemon.zinc":"Zinc","item.cobblemon.zinc.tooltip":"Raises the Pokémon's Sp. Defense EVs by 10","item.cobblemon.potion":"Potion","item.cobblemon.potion.tooltip":"Restores 20 HP of a Pokémon","item.cobblemon.super_potion":"Super Potion","item.cobblemon.super_potion.tooltip":"Restores 60 HP of a Pokémon","item.cobblemon.hyper_potion":"Hyper Potion","item.cobblemon.hyper_potion.tooltip":"Restores 120 HP of a Pokémon","item.cobblemon.max_potion":"Max Potion","item.cobblemon.max_potion.tooltip":"Fully restores the HP of a Pokémon","item.cobblemon.full_restore":"Full Restore","item.cobblemon.full_restore.tooltip":"Fully restores the HP and heals any status problems of a Pokémon","item.cobblemon.health_feather":"Health Feather","item.cobblemon.health_feather.tooltip":"Raises the Pokémon's HP EVs by 1","item.cobblemon.muscle_feather":"Muscle Feather","item.cobblemon.muscle_feather.tooltip":"Raises the Pokémon's Atk EVs by 1","item.cobblemon.resist_feather":"Resist Feather","item.cobblemon.resist_feather.tooltip":"Raises the Pokémon's Defense EVs by 1","item.cobblemon.genius_feather":"Genius Feather","item.cobblemon.genius_feather.tooltip":"Raises the Pokémon's Sp. Atk EVs by 1","item.cobblemon.clever_feather":"Clever Feather","item.cobblemon.clever_feather.tooltip":"Raises the Pokémon's Sp. Defense EVs by 1","item.cobblemon.swift_feather":"Swift Feather","item.cobblemon.swift_feather.tooltip":"Raises the Pokémon's Speed EVs by 1","item.cobblemon.ability_capsule":"Ability Capsule","item.cobblemon.ability_capsule.tooltip":"Changes the ability of the Pokémon it is used on to their alternative standard ability","item.cobblemon.ability_patch":"Ability Patch","item.cobblemon.ability_patch.tooltip":"Changes the ability of the Pokémon it is used on to their alternative hidden ability","item.cobblemon.red_mint_leaf":"Red Mint Leaf","item.cobblemon.red_mint_leaf.tooltip":"Capable of influencing a Pokémon's attack stat","item.cobblemon.blue_mint_leaf":"Blue Mint Leaf","item.cobblemon.blue_mint_leaf.tooltip":"Capable of influencing a Pokémon's defense stat","item.cobblemon.cyan_mint_leaf":"Cyan Mint Leaf","item.cobblemon.cyan_mint_leaf.tooltip":"Capable of influencing a Pokémon's special attack stat","item.cobblemon.pink_mint_leaf":"Pink Mint Leaf","item.cobblemon.pink_mint_leaf.tooltip":"Capable of influencing a Pokémon's special defense stat","item.cobblemon.green_mint_leaf":"Green Mint Leaf","item.cobblemon.green_mint_leaf.tooltip":"Capable of influencing a Pokémon's speed stat","item.cobblemon.white_mint_leaf":"White Mint Leaf","item.cobblemon.white_mint_leaf.tooltip":"Capable of neutralizing a Pokémon's nature","item.cobblemon.lonely_mint":"Lonely Mint","item.cobblemon.lonely_mint.tooltip":"Changes a Pokémon's stats to be that of a Lonely nature","item.cobblemon.adamant_mint":"Adamant Mint","item.cobblemon.adamant_mint.tooltip":"Changes a Pokémon's stats to be that of a Adamant nature","item.cobblemon.naughty_mint":"Naughty Mint","item.cobblemon.naughty_mint.tooltip":"Changes a Pokémon's stats to be that of a Naughty nature","item.cobblemon.brave_mint":"Brave Mint","item.cobblemon.brave_mint.tooltip":"Changes a Pokémon's stats to be that of a Brave nature","item.cobblemon.bold_mint":"Bold Mint","item.cobblemon.bold_mint.tooltip":"Changes a Pokémon's stats to be that of a Bold nature","item.cobblemon.impish_mint":"Impish Mint","item.cobblemon.impish_mint.tooltip":"Changes a Pokémon's stats to be that of a Impish nature","item.cobblemon.lax_mint":"Lax Mint","item.cobblemon.lax_mint.tooltip":"Changes a Pokémon's stats to be that of a Lax nature","item.cobblemon.relaxed_mint":"Relaxed Mint","item.cobblemon.relaxed_mint.tooltip":"Changes a Pokémon's stats to be that of a Relaxed nature","item.cobblemon.modest_mint":"Modest Mint","item.cobblemon.modest_mint.tooltip":"Changes a Pokémon's stats to be that of a Modest nature","item.cobblemon.mild_mint":"Mild Mint","item.cobblemon.mild_mint.tooltip":"Changes a Pokémon's stats to be that of a Mild nature","item.cobblemon.rash_mint":"Rash Mint","item.cobblemon.rash_mint.tooltip":"Changes a Pokémon's stats to be that of a Rash nature","item.cobblemon.quiet_mint":"Quiet Mint","item.cobblemon.quiet_mint.tooltip":"Changes a Pokémon's stats to be that of a Quiet nature","item.cobblemon.calm_mint":"Calm Mint","item.cobblemon.calm_mint.tooltip":"Changes a Pokémon's stats to be that of a Calm nature","item.cobblemon.gentle_mint":"Gentle Mint","item.cobblemon.gentle_mint.tooltip":"Changes a Pokémon's stats to be that of a Gentle nature","item.cobblemon.careful_mint":"Careful Mint","item.cobblemon.careful_mint.tooltip":"Changes a Pokémon's stats to be that of a Careful nature","item.cobblemon.sassy_mint":"Sassy Mint","item.cobblemon.sassy_mint.tooltip":"Changes a Pokémon's stats to be that of a Sassy nature","item.cobblemon.timid_mint":"Timid Mint","item.cobblemon.timid_mint.tooltip":"Changes a Pokémon's stats to be that of a Timid nature","item.cobblemon.hasty_mint":"Hasty Mint","item.cobblemon.hasty_mint.tooltip":"Changes a Pokémon's stats to be that of a Hasty nature","item.cobblemon.jolly_mint":"Jolly Mint","item.cobblemon.jolly_mint.tooltip":"Changes a Pokémon's stats to be that of a Jolly nature","item.cobblemon.naive_mint":"Naive Mint","item.cobblemon.naive_mint.tooltip":"Changes a Pokémon's stats to be that of a Naive nature","item.cobblemon.serious_mint":"Serious Mint","item.cobblemon.serious_mint.tooltip":"Changes a Pokémon's stats to be that of a Serious nature","item.cobblemon.ability_shield":"Ability Shield","item.cobblemon.ability_shield.tooltip":"Prevents the holder's ability from being changed or suppressed","item.cobblemon.absorb_bulb":"Absorb Bulb","item.cobblemon.absorb_bulb.tooltip_1":"Boosts the holder's Sp. Atk when hit by a Water-type move","item.cobblemon.absorb_bulb.tooltip_2":"Consumed after use","item.cobblemon.air_balloon":"Air Balloon","item.cobblemon.air_balloon.tooltip_1":"Makes the holder ungrounded","item.cobblemon.air_balloon.tooltip_2":"Destroyed on hit","item.cobblemon.assault_vest":"Assault Vest","item.cobblemon.assault_vest.tooltip":"Raises the holder's Sp. Def but prevents the use of status moves","item.cobblemon.auspicious_armor":"Auspicious Armor","item.cobblemon.auspicious_armor.tooltip":"Evolves Charcadet into Armarouge","item.cobblemon.malicious_armor":"Malicious Armor","item.cobblemon.malicious_armor.tooltip":"Evolves Charcadet into Ceruledge","item.cobblemon.binding_band":"Binding Band","item.cobblemon.binding_band.tooltip":"Increases damage dealt by the binding status","item.cobblemon.black_belt":"Black Belt","item.cobblemon.black_belt.tooltip":"Boosts the power of the holder's Fighting-type moves","item.cobblemon.black_glasses":"Black Glasses","item.cobblemon.black_glasses.tooltip":"Boosts the power of the holder's Dark-type moves","item.cobblemon.black_sludge":"Black Sludge","item.cobblemon.black_sludge.tooltip":"If the holder is a Poison-type, its HP is gradually restored throughout a battle. It damages any other type","item.cobblemon.blunder_policy":"Blunder Policy","item.cobblemon.blunder_policy.tooltip_1":"Boosts the holder's Speed when an attack is missed","item.cobblemon.blunder_policy.tooltip_2":"Consumed after use","item.cobblemon.cell_battery":"Cell Battery","item.cobblemon.cell_battery.tooltip_1":"Boosts the holder's Attack when hit by an Electric-type move","item.cobblemon.cell_battery.tooltip_2":"Consumed after use","item.cobblemon.charcoal_stick":"Charcoal Stick","item.cobblemon.charcoal_stick.tooltip":"Boosts the power of the holder's Fire-type moves","item.cobblemon.choice_band":"Choice Band","item.cobblemon.choice_band.tooltip":"Boosts the holder's Attack, but locks it into only using one move","item.cobblemon.choice_scarf":"Choice Scarf","item.cobblemon

... truncated ...
```

## assets/cobblemon/lang/ko_kr.json

```json
{"item.cobblemon.poke_ball":"몬스터볼","item.cobblemon.slate_ball":"블랙볼","item.cobblemon.azure_ball":"블루볼","item.cobblemon.verdant_ball":"그린볼","item.cobblemon.roseate_ball":"핑크볼","item.cobblemon.citrine_ball":"옐로볼","item.cobblemon.great_ball":"슈퍼볼","item.cobblemon.ultra_ball":"하이퍼볼","item.cobblemon.master_ball":"마스터볼","item.cobblemon.safari_ball":"사파리볼","item.cobblemon.fast_ball":"스피드볼","item.cobblemon.level_ball":"레벨볼","item.cobblemon.lure_ball":"루어볼","item.cobblemon.heavy_ball":"헤비볼","item.cobblemon.love_ball":"러브러브볼","item.cobblemon.friend_ball":"프렌드볼","item.cobblemon.moon_ball":"문볼","item.cobblemon.sport_ball":"컴퍼티션볼","item.cobblemon.net_ball":"네트볼","item.cobblemon.dive_ball":"다이브볼","item.cobblemon.nest_ball":"네스트볼","item.cobblemon.repeat_ball":"리피트볼","item.cobblemon.timer_ball":"타이머볼","item.cobblemon.luxury_ball":"럭셔리볼","item.cobblemon.premier_ball":"프리미어볼","item.cobblemon.dusk_ball":"다크볼","item.cobblemon.heal_ball":"힐볼","item.cobblemon.quick_ball":"퀵볼","item.cobblemon.cherish_ball":"프레셔스볼","item.cobblemon.park_ball":"파크볼","item.cobblemon.dream_ball":"드림볼","item.cobblemon.beast_ball":"울트라볼","item.cobblemon.ancient_poke_ball":"고대 몬스터볼","item.cobblemon.ancient_citrine_ball":"고대 옐로볼","item.cobblemon.ancient_verdant_ball":"고대 그린볼","item.cobblemon.ancient_azure_ball":"고대 블루볼","item.cobblemon.ancient_roseate_ball":"고대 핑크볼","item.cobblemon.ancient_slate_ball":"고대 블랙볼","item.cobblemon.ancient_ivory_ball":"고대 아이보리볼","item.cobblemon.ancient_great_ball":"고대 슈퍼볼","item.cobblemon.ancient_ultra_ball":"고대 하이퍼볼","item.cobblemon.ancient_heavy_ball":"고대 헤비볼","item.cobblemon.ancient_leaden_ball":"고대 메가톤볼","item.cobblemon.ancient_gigaton_ball":"고대 기가톤볼","item.cobblemon.ancient_feather_ball":"고대 페더볼","item.cobblemon.ancient_wing_ball":"고대 윙볼","item.cobblemon.ancient_jet_ball":"고대 제트볼","item.cobblemon.ancient_origin_ball":"고대 오리진볼","item.cobblemon.black_apricorn_seed":"검은규토리 싹","item.cobblemon.blue_apricorn_seed":"파란규토리 싹","item.cobblemon.green_apricorn_seed":"초록규토리 싹","item.cobblemon.pink_apricorn_seed":"담홍규토리 싹","item.cobblemon.red_apricorn_seed":"빨간규토리 싹","item.cobblemon.white_apricorn_seed":"하얀규토리 싹","item.cobblemon.yellow_apricorn_seed":"노랑규토리 싹","item.cobblemon.black_apricorn":"검은규토리","item.cobblemon.blue_apricorn":"파란규토리","item.cobblemon.green_apricorn":"초록규토리","item.cobblemon.pink_apricorn":"담홍규토리","item.cobblemon.red_apricorn":"빨간규토리","item.cobblemon.white_apricorn":"하얀규토리","item.cobblemon.yellow_apricorn":"노랑규토리","item.cobblemon.vivichoke_seeds":"기력의봉오리 씨앗","item.cobblemon.vivichoke":"기력의봉오리","item.cobblemon.apricorn_boat":"규토리나무 보트","item.cobblemon.apricorn_chest_boat":"상자가 실린 규토리나무 보트","item.cobblemon.link_cable":"연결의끈","item.cobblemon.dragon_scale":"용의비늘","item.cobblemon.kings_rock":"왕의징표석","item.cobblemon.metal_coat":"금속코트","item.cobblemon.upgrade":"업그레이드","item.cobblemon.dubious_disc":"괴상한패치","item.cobblemon.deep_sea_scale":"심해의비늘","item.cobblemon.deep_sea_tooth":"심해의이빨","item.cobblemon.electirizer":"에레키부스터","item.cobblemon.magmarizer":"마그마부스터","item.cobblemon.oval_stone":"동글동글돌","item.cobblemon.protector":"프로텍터","item.cobblemon.reaper_cloth":"영계의천","item.cobblemon.prism_scale":"고운비늘","item.cobblemon.sachet":"향기주머니","item.cobblemon.whipped_dream":"휘핑팝","item.cobblemon.strawberry_sweet":"딸기사탕공예","item.cobblemon.love_sweet":"하트사탕공예","item.cobblemon.berry_sweet":"베리사탕공예","item.cobblemon.clover_sweet":"네잎사탕공예","item.cobblemon.flower_sweet":"꽃사탕공예","item.cobblemon.star_sweet":"스타사탕공예","item.cobblemon.ribbon_sweet":"리본사탕공예","item.cobblemon.chipped_pot":"이빠진포트","item.cobblemon.cracked_pot":"깨진포트","item.cobblemon.masterpiece_teacup":"걸작찻잔","item.cobblemon.unremarkable_teacup":"범작찻잔","item.cobblemon.sweet_apple":"달콤한사과","item.cobblemon.tart_apple":"새콤한사과","item.cobblemon.galarica_cuff":"가라두구팔찌","item.cobblemon.galarica_wreath":"가라두구머리장식","item.cobblemon.black_augurite":"검은휘석","item.cobblemon.peat_block":"피트블록","item.cobblemon.dawn_stone":"각성의돌","item.cobblemon.dusk_stone":"어둠의돌","item.cobblemon.fire_stone":"불꽃의돌","item.cobblemon.ice_stone":"얼음의돌","item.cobblemon.leaf_stone":"리프의돌","item.cobblemon.moon_stone":"달의돌","item.cobblemon.shiny_stone":"빛의돌","item.cobblemon.sun_stone":"태양의돌","item.cobblemon.thunder_stone":"천둥의돌","item.cobblemon.water_stone":"물의돌","item.cobblemon.rare_candy":"이상한사탕","item.cobblemon.exp_candy_xs":"경험사탕XS","item.cobblemon.exp_candy_s":"경험사탕S","item.cobblemon.exp_candy_m":"경험사탕M","item.cobblemon.exp_candy_l":"경험사탕L","item.cobblemon.exp_candy_xl":"경험사탕XL","item.cobblemon.calcium":"리보플라빈","item.cobblemon.carbos":"알칼로이드","item.cobblemon.hp_up":"맥스업","item.cobblemon.iron":"사포닌","item.cobblemon.protein":"타우린","item.cobblemon.zinc":"키토산","item.cobblemon.potion":"상처약","item.cobblemon.super_potion":"좋은상처약","item.cobblemon.hyper_potion":"고급상처약","item.cobblemon.max_potion":"풀회복약","item.cobblemon.full_restore":"회복약","item.cobblemon.health_feather":"체력깃털","item.cobblemon.muscle_feather":"근력깃털","item.cobblemon.resist_feather":"저항력깃털","item.cobblemon.genius_feather":"지력깃털","item.cobblemon.clever_feather":"정신력깃털","item.cobblemon.swift_feather":"순발력깃털","item.cobblemon.ability_capsule":"특성캡슐","item.cobblemon.ability_patch":"특성패치","item.cobblemon.red_mint_leaf":"빨간색 민트","item.cobblemon.red_mint_leaf.tooltip":"공격 능력치가 증가하는 성격과 관련 있는 민트","item.cobblemon.blue_mint_leaf":"파란색 민트","item.cobblemon.blue_mint_leaf.tooltip":"방어 수치가 증가하는 성격과 관련이 있는 민트","item.cobblemon.cyan_mint_leaf":"하늘색 민트","item.cobblemon.cyan_mint_leaf.tooltip":"특수공격 수치가 증가하는 성격과 관련이 있는 민트","item.cobblemon.pink_mint_leaf":"분홍색 민트","item.cobblemon.pink_mint_leaf.tooltip":"특수방어 수치가 증가하는 성격과 관련이 있는 민트","item.cobblemon.green_mint_leaf":"초록색 민트","item.cobblemon.green_mint_leaf.tooltip":"스피드 수치가 증가하는 성격과 관련이 있는 민트","item.cobblemon.white_mint_leaf":"하얀색 민트","item.cobblemon.white_mint_leaf.tooltip":"능력 보정이 없는 성격과 관련이 있는 민트","item.cobblemon.lonely_mint":"외로움민트","item.cobblemon.adamant_mint":"고집민트","item.cobblemon.naughty_mint":"개구쟁이민트","item.cobblemon.brave_mint":"용감민트","item.cobblemon.bold_mint":"대담민트","item.cobblemon.impish_mint":"장난꾸러기민트","item.cobblemon.lax_mint":"촐랑민트","item.cobblemon.relaxed_mint":"무사태평민트","item.cobblemon.modest_mint":"조심민트","item.cobblemon.mild_mint":"의젓민트","item.cobblemon.rash_mint":"덜렁민트","item.cobblemon.quiet_mint":"냉정민트","item.cobblemon.calm_mint":"차분민트","item.cobblemon.gentle_mint":"얌전민트","item.cobblemon.careful_mint":"신중민트","item.cobblemon.sassy_mint":"건방민트","item.cobblemon.timid_mint":"겁쟁이민트","item.cobblemon.hasty_mint":"성급민트","item.cobblemon.jolly_mint":"명랑민트","item.cobblemon.naive_mint":"천진난만민트","item.cobblemon.serious_mint":"성실민트","item.cobblemon.ability_shield":"특성가드","item.cobblemon.absorb_bulb":"구근","item.cobblemon.air_balloon":"풍선","item.cobblemon.assault_vest":"돌격조끼","item.cobblemon.auspicious_armor":"축복받은갑옷","item.cobblemon.malicious_armor":"저주받은갑옷","item.cobblemon.binding_band":"조임밴드","item.cobblemon.black_belt":"검은띠","item.cobblemon.black_glasses":"검은안경","item.cobblemon.black_sludge":"검은진흙","item.cobblemon.blunder_policy":"허탕보험","item.cobblemon.cell_battery":"충전지","item.cobblemon.charcoal_stick":"목탄","item.cobblemon.choice_band":"구애머리띠","item.cobblemon.choice_scarf":"구애스카프","item.cobblemon.choice_specs":"구애안경","item.cobblemon.cleanse_tag":"순결의부적","item.cobblemon.clear_amulet":"클리어참","item.cobblemon.covert_cloak":"은밀망토","item.cobblemon.dragon_fang":"용의이빨","item.cobblemon.eject_button":"탈출버튼","item.cobblemon.eject_pack":"탈출팩","item.cobblemon.everstone":"변함없는돌","item.cobblemon.eviolite":"진화의휘석","item.cobblemon.exp_share":"학습장치","item.cobblemon.expert_belt":"달인의띠","item.cobblemon.fairy_feather":"요정의깃털","item.cobblemon.float_stone":"가벼운돌","item.cobblemon.focus_band":"기합의머리띠","item.cobblemon.focus_sash":"기합의띠","item.cobblemon.grip_claw":"끈기갈고리손톱","item.cobblemon.hard_stone":"딱딱한돌","item.cobblemon.heavy_duty_boots":"통굽부츠","item.cobblemon.iron_ball":"검은철구","item.cobblemon.lagging_tail":"느림보꼬리","item.cobblemon.leftovers":"먹다남은음식","item.cobblemon.light_ball":"전기구슬","item.cobblemon.light_clay":"빛의점토","item.cobblemon.loaded_dice":"속임수주사위","item.cobblemon.lucky_egg":"행복의알","item.cobblemon.luminous_moss":"빛이끼","item.cobblemon.magnet":"자석","item.cobblemon.metronome":"메트로놈","item.cobblemon.miracle_seed":"기적의씨","item.cobblemon.muscle_band":"힘의머리띠","item.cobblemon.mystic_water":"신비의물방울","item.cobblemon.never_melt_ice":"녹지않는얼음","item.cobblemon.poison_barb":"독바늘","item.cobblemon.protective_pads":"방호패드","item.cobblemon.punching_glove":"펀치글러브","item.cobblemon.quick_claw":"선제공격손톱","item.cobblemon.razor_claw":"예리한손톱","item.cobblemon.razor_fang":"예리한이빨","item.cobblemon.red_card":"레드카드","item.cobblemon.ring_target":"겨냥표적","item.cobblemon.rocky_helmet":"울퉁불퉁멧","item.cobblemon.room_service":"룸서비스","item.cobblemon.safety_goggles":"방진고글","item.cobblemon.scope_lens":"초점렌즈","item.cobblemon.sharp_beak":"예리한부리","item.cobblemon.shed_shell":"아름다운허물","item.cobblemon.shell_bell":"조개껍질방울","item.cobblemon.silk_scarf":"실크스카프","item.cobblemon.silver_powder":"은빛가루","item.cobblemon.soft_sand":"부드러운모래","item.cobblemon.soothe_bell":"평온의방울","item.cobblemon.spell_tag":"저주의부적","item.cobblemon.sticky_barb":"끈적끈적바늘","item.cobblemon.terrain_extender":"그라운드코트","item.cobblemon.throat_spray":"목스프레이","item.cobblemon.twisted_spoon":"휘어진스푼","item.cobblemon.utility_umbrella":"만능우산","item.cobblemon.weakness_policy":"약점보험","item.cobblemon.wide_lens":"광각렌즈","item.cobblemon.wise_glasses":"박식안경","item.cobblemon.zoom_lens":"포커스렌즈","item.cobblemon.mental_herb":"멘탈허브","item.cobblemon.mirror_herb":"흉내허브","item.cobblemon.power_herb":"파워풀허브","item.cobblemon.white_herb":"하양허브","item.cobblemon.bright_powder":"반짝가루","item.cobblemon.metal_powder":"금속파우더","item.cobblemon.quick_powder":"스피드파우더","item.cobblemon.destiny_knot":"빨간실","item.cobblemon.power_anklet":"파워앵클릿","item.cobblemon.power_band":"파워밴드","item.cobblemon.power_belt":"파워벨트","item.cobblemon.power_bracer":"파워리스트","item.cobblemon.power_lens":"파워렌즈","item.cobblemon.power_weight":"파워웨이트","item.cobblemon.flame_orb":"화염구슬","item.cobblemon.light_orb":"전기구슬","item.cobblemon.smoke_ball":"연막탄","item.cobblemon.toxic_orb":"맹독구슬","item.cobblemon.life_orb":"생명의구슬","item.cobblemon.damp_rock":"축축한바위","item.cobblemon.heat_rock":"뜨거운바위","item.cobblemon.smooth_rock":"보송보송바위","item.cobblemon.icy_rock":"차가운바위","item.cobblemon.poke_ball.tooltip":"야생 포켓몬에게 던져서 잡기 위한 볼. 캡슐식으로 되어 있다 (포획률: 1배)","item.cobblemon.slate_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.azure_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.verdant_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.roseate_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.citrine_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.great_ball.tooltip":"몬스터볼보다도 더욱 포켓몬을 잡기 쉬워진 약간 성능이 좋은 볼 (포획률: 1.5배)","item.cobblemon.ultra_ball.tooltip":"슈퍼볼보다도 더욱 포켓몬을 잡기 쉬워진 매우 성능이 좋은 볼 (포획률: 2배)","item.cobblemon.master_ball.tooltip":"야생 포켓몬을 반드시 잡을 수 있는 최고 성능의 볼","item.cobblemon.safari_ball.tooltip":"전투 중이 아닐 때 사용하면 일정 확률로 포획할 수 있다 (포획률: 1.5배)","item.cobblemon.fast_ball.tooltip":"스피드 종족값이 100 이상인 포켓몬에게 사용할 시 포획률이 상승한다 (포획률: 4배)","item.cobblemon.level_ball.tooltip":"자신의 포켓몬보다 레벨이 낮을수록 잡기 쉬워지는 조금 특이한 볼 (포획률: 1~4배)","item.cobblemon.lure_ball.tooltip":"낚싯대로 낚아올린 포켓몬이라면 잡기 쉬워지는 조금 특이한 볼 (포획률: 4배)","item.cobblemon.heavy_ball.tooltip":"체중이 무거운 포켓몬일수록 잡기 쉬워지는 조금 특이한 볼 (포획률: 1~4배)","item.cobblemon.love_ball.tooltip":"상대 포켓몬의 성별이 다를 경우 2.5배, 같은 종이면서 성별이 다를 경우 8배의 포획률을 가진다 (포획률: 2.5~8배)","item.cobblemon.friend_ball.tooltip":"잡은 야생 포켓몬이 바로 친밀해지는 조금 특이한 볼, 초기 친밀도 150 적용 (포획률: 1배)","item.cobblemon.moon_ball.tooltip":"밤에 사용할 때 달 모양이 보름달에 가까울수록 잡기 쉬워지는 조금 특이한 볼 (포획률: 1~4배)","item.cobblemon.sport_ball.tooltip":"성도지방의 곤충채집 대회에서 사용되었던 특별한 볼 (포획률: 1.5배)","item.cobblemon.net_ball.tooltip":"물 타입과 벌레 타입의 포켓몬을 잡기 쉬운 조금 특이한 볼 (포획률: 3배)","item.cobblemon.dive_ball.tooltip":"물의 세계에서 사는 포켓몬을 잡기 쉬운 조금 특이한 볼 (포획률: 3.5배)","item.cobblemon.nest_ball.tooltip":"잡으려는 포켓몬의 레벨 차이가 30에 가까워질수록 잡기 쉬워지는 조금 특이한 볼 (포획률: 1~4배)","item.cobblemon.repeat_ball.tooltip":"이미 포획한 적이 있는 포켓몬일 경우 잡기 쉬워지는 조금 특이한 볼 (포획률: 3.5배)","item.cobblemon.timer_ball.tooltip":"턴이 지날수록 더 잘 잡히는 볼, 10턴 이상에서 4배로 고정 (포획률: 1~4배)","item.cobblemon.luxury_ball.tooltip":"잡은 야생 포켓몬이 매우 친밀해지기 쉬운 편안한 볼, 친밀도 상승 2배 적용 (포획률: 1배)","item.cobblemon.premier_ball.tooltip":"무언가의 기념품으로 특별히 만들어진 조금 희귀한 볼 (포획률: 1배)","item.cobblemon.dusk_ball.tooltip":"주변 밝기가 낮을수록 잘 잡히는 볼로, 밝기 0일 때 3.5배, 1~7일 때 3배의 포획률을 가진다 (포획률: 3~3.5배)","item.cobblemon.heal_ball.tooltip":"포획 시 HP와 PP, 상태 이상이 모두 회복되는 볼 (포획률: 1배)","item.cobblemon.quick_ball.tooltip":"배틀 첫 턴에 더 잘 잡히는 볼 (포획률: 5배)","item.cobblemon.cherish_ball.tooltip":"무언가의 기념품으로 특별히 만들어진 상당히 진귀한 볼 (포획률: 1배)","item.cobblemon.park_ball.tooltip":"숲이나 평원 바이옴에서 잡기 쉬워지는 볼 (포획률: 2.5배)","item.cobblemon.dream_ball.tooltip":"잠든 포켓몬을 더 잘 잡을 수 있는 볼 (포획률: 4배)","item.cobblemon.beast_ball.tooltip":"울트라비스트일 경우 포획률 5배, 그 외 포켓몬은 0.1배 (포획률: 0.1~5배)","item.cobblemon.ancient_poke_ball.tooltip":"야생 포켓몬에게 던지면 잡을 수 있는 신비한 볼. 재료를 모아 공작으로 만든다 (포획률: 1배)","item.cobblemon.ancient_citrine_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_verdant_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_azure_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_roseate_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_slate_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_ivory_ball.tooltip":"야생 포켓몬에게 던지면 일정 확률로 포획할 수 있다 (포획률: 1배)","item.cobblemon.ancient_great_ball.tooltip":"몬스터볼보다도 더욱 포켓몬을 잡기 쉬워진 약간 성능이 좋고 신비한 볼 (포획률: 1.5배)","item.cobblemon.ancient_ultra_ball.tooltip":"슈퍼볼보다도 더욱 포켓몬을 잡기 쉬워진 매우 성능이 좋고 신비한 볼 (포획률: 2배)","item.cobblemon.ancient_feather_ball.tooltip":"더 멀리 던질 수 있는 볼 (포획률: 1배)","item.cobblemon.ancient_wing_ball.tooltip":"더 멀리 던질 수 있는 볼 (포획률: 1.5배)","item.cobblemon.ancient_jet_ball.tooltip":"더 멀리 던질 수 있는 볼 (포획률: 2배)","item.cobblemon.ancient_heavy_ball.tooltip":"멀리 날아가지 않는 볼 (포획률: 1배)","item.cobblemon.ancient_leaden_ball.tooltip":"멀리 날아가지 않는 볼 (포획률: 1.5배)","item.cobblemon.ancient_gigaton_ball.tooltip":"멀리 날아가지 않는 볼 (포획률: 2배)","item.cobblemon.ancient_origin_ball.tooltip":"던지면 무조건 잡힌다","item.cobblemon.mulch_base":"비료","item.cobblemon.coarse_mulch":"거친비료","item.cobblemon.growth_mulch":"무럭무럭비료","item.cobblemon.humid_mulch":"축축이비료","item.cobblemon.loamy_mulch":"푹신푹신한비료","item.cobblemon.peat_mulch":"피트비료","item.cobblemon.rich_mulch":"주렁주렁비료","item.cobblemon.sandy_mulch":"모래비료","item.cobblemon.surprise_mulch":"깜놀비료","item.cobblemon.medicinal_leek":"약초 대파","item.cobblemon.energy_root":"힘의뿌리","item.cobblemon.revival_herb":"부활초","item.cobblemon.roasted_leek":"구운 대파","item.cobblemon.leek_and_potato_stew":"감자 대파 스튜","item.cobblemon.braised_vivichoke":"구운 기억의봉오리","item.cobblemon.vivichoke_dip":"기력의봉오리 딥","item.cobblemon.berry_juice":"나무열매쥬스","item.cobblemon.remedy":"한방약","item.cobblemon.fine_remedy":"좋은한방약","item.cobblemon.superb_remedy":"고급한방약","item.cobblemon.heal_powder":"만능가루","item.cobblemon.medicinal_brew":"약용 물약","item.cobblemon.revive":"기력의조각","item.cobblemon.max_revive":"기력의덩어리","item.cobblemon.pp_up":"포인트업","item.cobblemon.pp_max":"포인트맥스","item.cobblemon.x_attack":"플러스파워","item.cobblemon.x_defence":"디펜드업","item.cobblemon.x_accuracy":"잘-맞히기","item.cobblemon.x_speed":"스피드업","item.cobblemon.x_special_defence":"스페셜가드","item.cobblemon.x_special_attack":"스페셜업","item.cobblemon.dire_hit":"크리티컬커터","item.cobblemon.guard_spec":"이펙트가드","item.cobblemon.full_heal":"만병통치제","item.cobblemon.antidote":"해독제","item.cobblemon.awakening":"잠깨는약","item.cobblemon.burn_heal":"화상치료제","item.cobblemon.ice_heal":"얼음상태치료제","item.cobblemon.paralyze_heal":"마비치료제","item.cobblemon.ether":"PP에이드","item.cobblemon.max_ether":"PP회복","item.cobblemon.elixir":"PP에이더","item.cobblemon.max_elixir":"PP맥스","item.cobblemon.normal_gem":"노말주얼","item.cobblemon.water_gem":"물주얼","item.cobblemon.fire_gem":"불꽃주얼","item.cobblemon.grass_gem":"풀주얼","item.cobblemon.dragon_gem":"드래곤주얼","item.cobblemon.ghost_gem":"고스트주얼","item.cobblemon.ground_gem":"땅주얼","item.cobblemon.steel_gem":"강철주얼","item.cobblemon.fairy_gem":"페어리주얼","item.cobblemon.rock_gem":"바위주얼","item.cobblemon.psychic_gem":"에스퍼주얼","item.cobblemon.electric_gem":"전기주얼","item.cobblemon.bug_gem":"벌레주얼","item.cobblemon.poison_gem":"독주얼","item.cobblemon.ice_gem":"얼음주얼","item.cobblemon.dark_gem":"악주얼","item.cobblemon.fighting_gem":"격투주얼","item.cobblemon.flying_gem":"비행주얼","item.cobblemon.armor_fossil":"방패의화석","item.cobblemon.fossilized_bird":"화석새","item.cobblemon.claw_fossil":"발톱화석","item.cobblemon.cover_fossil":"덮개화석","item.cobblemon.fossilized_dino":"화석긴목","item.cobblemon.dome_fossil":"껍질화석","item.cobblemon.fossilized_drake":"화석용","item.cobblemon.fossilized_fish":"화석물고기","item.cobblemon.helix_fossil":"조개화석","item.cobblemon.jaw_fossil":"턱화석","item.cobblemon.old_amber_fossil":"비밀의호박","item.cobblemon.plume_fossil":"깃털화석","item.cobblemon.root_fossil":"뿌리화석","item.cobblemon.sail_fossil":"지느러미화석","item.cobblemon.skull_fossil":"두개의화석","item.cobblemon.bygone_sherd":"과거 도자기 조각","item.cobblemon.capture_sherd":"포획 도자기 조각","item.cobblemon.dome_sherd":"껍질 도자기 조각","item.cobblemon.helix_sherd":"조개 도자기 조각","item.cobblemon.nostalgic_sherd":"추억 도자기 조각","item.cobblemon.suspicious_sherd":"수상한 도자기 조각","item.cobblemon.tumblestone":"옥돌","item.cobblemon.black_tumblestone":"검은옥돌","item.cobblemon.sky_tumblestone":"하늘색옥돌","item.cobblemon.relic_coin":"유물 금화","block.cobblemon.relic_coin_pouch":"유물 금화 주머니","block.cobblemon.relic_coin_sack":"유물 금화 자루","trim_pattern.cobblemon.automaton":"로봇 갑옷 장식","cobblemon.fossil.armor":"방패","cobblemon.fossil.bird":"새","cobblemon.fossil.claw":"발톱","cobblemon.fossil.cover":"덮개","cobblemon.fossil.dino":"긴목","cobblemon.fossil.dome":"껍질","cobblemon.fossil.drake":"용","cobblemon.fossil.fish":"물고기","cobblemon.fossil.helix":"조개","cobblemon.fossil.jaw":"턱","cobblemon.fossil.old_amber":"비밀의호박","cobblemon.fossil.plume":"깃털","cobblemon.fossil.root":"뿌리","cobblemon.fossil.sail":"지느러미","cobblemon.fossil.skull":"두개","cobblemon.fossilmachine.protected":"이 화석은 %s의 것입니다!","itemGroup.cobblemon.blocks":"코블몬: 블록","itemGroup.cobblemon.pokeball":"몬스터볼","itemGroup.cobblemon.agriculture":"코블몬: 농작물","itemGroup.cobblemon.consumables":"코블몬: 소모품","itemGroup.cobblemon.held_item":"코블몬: 소지품","itemGroup.cobblemon.evolution_item":"코블몬: 진화 아이템","itemGroup.cobblemon.archaeology":"코블몬: 고고학","block.cobblemon.apricorn_log":"규토리 원목","block.cobblemon.stripped_apricorn_log":"껍질 벗긴 규토리 원목","block.cobblemon.apricorn_wood":"규토리나무","block.cobblemon.stripped_apricorn_wood":"껍질 벗긴 규토리나무","block.cobblemon.apricorn_planks":"규토리나무 판자","block.cobblemon.apricorn_leaves":"규토리나무 잎","block.cobblemon.apricorn_fence":"규토리나무 울타리","block.cobblemon.apricorn_sign":"규토리나무 표지판","block.cobblemon.apricorn_hanging_sign":"규토리나무 매다는 표지판","block.cobblemon.apricorn_stairs":"규토리나무 계단","block.cobblemon.apricorn_pressure_plate":"규토리나무 압력판","block.cobblemon.apricorn_button":"규토리나무 버튼","block.cobblemon.apricorn_slab":"규토리나무 반 블록","block.cobblemon.apricorn_fence_gate":"규토리나무 울타리 문","block.cobblemon.big_root":"큰뿌리","block.cobblemon.black_apricorn_sapling":"검은규토리 묘목","block.cobblemon.blue_apricorn_sapling":"파란규토리 묘목","block.cobblemon.green_apricorn_sapling":"초록규토리 묘목","block.cobblemon.pink_apricorn_sapling":"담홍규토리 묘목","block.cobblemon.red_apricorn_sapling":"빨간규토리 묘목","block.cobblemon.white_apricorn_sapling":"하얀규토리 묘목","block.cobblemon.yellow_apricorn_sapling":"노랑규토리 묘목","block.cobblemon.apricorn_door":"규토리나무 문","block.cobblemon.apricorn_trapdoor":"규토리나무 다락문","block.cobblemon.healing_machine":"회복 장치","block.cobblemon.healing_machine.tooltip1":"설치 후 시간이 지나면 충전된다.","block.cobblemon.pc":"PC","block.cobblemon.restoration_tank":"복원 탱크","block.cobblemon.fossil_analyzer":"화석 분석기","block.cobblemon.monitor":"데이터 모니터","block.cobblemon.red_mint":"빨간색 민트 씨앗","block.cobblemon.blue_mint":"파란색 민트 씨앗","block.cobblemon.cyan_mint":"하늘색 민트 씨앗","block.cobblemon.pink_mint":"분홍색 민트 씨앗","block.cobblemon.green_mint":"초록색 민트 씨앗","block.cobblemon.white_mint":"하얀색 민트 씨앗","block.cobblemon.vivichoke_seeds":"기력의봉오리 씨앗","block.cobblemon.pep_up_flower":"PP풀","block.cobblemon.potted_pep_up_fl

... truncated ...
```

## resourcepacks/adorncompatibility/assets/adorn/lang/en_us.json

```json
{
  "block.adorn.cobblemon.apricorn_chair": "Apricorn Chair",
  "block.adorn.cobblemon.apricorn_table": "Apricorn Table",
  "block.adorn.cobblemon.apricorn_drawer": "Apricorn Drawer",
  "block.adorn.cobblemon.apricorn_kitchen_counter": "Apricorn Kitchen Counter",
  "block.adorn.cobblemon.apricorn_kitchen_cupboard": "Apricorn Kitchen Cupboard",
  "block.adorn.cobblemon.apricorn_kitchen_sink": "Apricorn Kitchen Sink",
  "block.adorn.cobblemon.apricorn_post": "Apricorn Post",
  "block.adorn.cobblemon.apricorn_platform": "Apricorn Platform",
  "block.adorn.cobblemon.apricorn_step": "Apricorn Step",
  "block.adorn.cobblemon.apricorn_shelf": "Apricorn Shelf",
  "block.adorn.cobblemon.apricorn_coffee_table": "Apricorn Coffee Table",
  "block.adorn.cobblemon.apricorn_bench": "Apricorn Bench",
  "block.adorn.cobblemon.saccharine_chair": "Saccharine Chair",
  "block.adorn.cobblemon.saccharine_table": "Saccharine Table",
  "block.adorn.cobblemon.saccharine_drawer": "Saccharine Drawer",
  "block.adorn.cobblemon.saccharine_kitchen_counter": "Saccharine Kitchen Counter",
  "block.adorn.cobblemon.saccharine_kitchen_cupboard": "Saccharine Kitchen Cupboard",
  "block.adorn.cobblemon.saccharine_kitchen_sink": "Saccharine Kitchen Sink",
  "block.adorn.cobblemon.saccharine_post": "Saccharine Post",
  "block.adorn.cobblemon.saccharine_platform": "Saccharine Platform",
  "block.adorn.cobblemon.saccharine_step": "Saccharine Step",
  "block.adorn.cobblemon.saccharine_shelf": "Saccharine Shelf",
  "block.adorn.cobblemon.saccharine_coffee_table": "Saccharine Coffee Table",
  "block.adorn.cobblemon.saccharine_bench": "Saccharine Bench"
}

```
