# HeadHunter

With this plugin you are able to config both player heads and mob heads following information will be both how to activate it and the permissions related to it

Current version: `1.21.5.015` 
API-version: `1.21.`

Activating is as simple as placing it in your plugins folder. Following is the config of this plugin it will be updated accordingly:

```
# Enable/disable player head drops on player death
player-head-drops-enabled: true

# Enable/disable mob head drops on mob death
mob-head-drops-enabled: false

# sound settings
head-sound-effects:
  # Enable/disable the noteblock sound replacement feature
  enabled: true

  # Play sound when head is placed on noteblock
  play-on-place: false

  # Show particle effects when sounds play
  particles: true

  # Send chat messages when animal sounds play ( Wouldn't recommend it ngl ) 
  messages: false

  # Volume for the animal sounds (0.1 to 2.0)
  volume: 1.0

  # Pitch for the animal sounds (0.5 to 2.0)
  pitch: 1.0

# Drop chances for mob heads (percent) Change these to your preferred choice
mob-head-drop-chances:
  spider: 10.0
  cave_spider: 10.0
  drowned: 15.0
  blaze: 5.0
  slime: 8.0
  magma_cube: 8.0
  enderman: 12.0
  pig: 5.0
  cow: 5.0
  sheep: 5.0
  chicken: 5.0
  villager: 10.0
  witch: 12.5
  iron_golem: 7.5
  snow_golem: 7.5
  hoglin: 6.0
  zoglin: 6.0
  piglin_brute: 12.0
  ravager: 20.0
  warden: 2.0
  vex: 10.0
  breeze: 5.0
  bogged: 10.0
  evoker: 12.5
  shulker: 8.0
  silverfish: 5.0
  stray: 15.0
  illusioner: 12.5
  creaking: 12.5
  giant: 20.0
  elder_guardian: 5.0
  wither: 1.0
  guardian: 8.0
  zombified_piglin: 10.0
  endermite: 5.0
  phantom: 10.0
  pillager: 10.0
  allay: 5.0
  armadillo: 5.0
  axolotl: 5.0
  wolf: 5.0
  cod: 5.0
  bee: 5.0
  fox: 5.0
  goat: 5.0
  cat: 5.0
  bat: 2.5
  camel: 5.0
  donkey: 5.0
  frog: 5.0
  glow_squid: 5.0
  horse: 5.0
  mooshroom: 5.0
  mule: 5.0
  ocelot: 5.0
  parrot: 5.0
  pufferfish: 5.0
  rabbit: 5.0
  salmon: 5.0
  skeleton_horse: 5.0
  sniffer: 5.0
  squid: 5.0
  strider: 5.0
  tadpole: 2.5
  tropical_fish: 5.0
  turtle: 5.0
  wandering_trader: 5.0
  llama: 5.0
  trader_llama: 5.0
  zombie_villager: 10.0
  zombie_horse: 5.0
  panda: 5.0
  dolphin: 5.0
  polar_bear: 5.0
  vindicator: 12.0
```

Permissions:
```
  headhunter.update:
    description: Allows updaing the plugin
    default: op
  headhunter.reload:
    description: Allows reloading the plugin configuration
    default: op
  headhunter.spawn:
    description: Allows spawning heads with commands
    default: op
  headhunter.purchased:
    description: Allows spawning heads with commands (Console)
    default: op

  headhunter.*:
    description: Gives access to all HeadHunter permissions
    default: op
    children:
      headhunter.reload: true
      headhunter.spawn: true
      headhunter.update: true
      headhunter.purchased: true
```
